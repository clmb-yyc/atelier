package com.clmb.atelier.access;

import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.gyms.Gym;
import com.clmb.atelier.organizations.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipService {
    private final AccessPlanRepository plans;
    private final MemberPassRepository passes;
    private final AdministrationAccess access;
    private final OrganizationRepository organizations;

    public MembershipService(AccessPlanRepository plans, MemberPassRepository passes, AdministrationAccess access,
                             OrganizationRepository organizations) {
        this.plans = plans;
        this.passes = passes;
        this.access = access;
        this.organizations = organizations;
    }

    @Transactional
    public AccessPlan createPlan(UUID actorId, UUID organizationId, String name, UUID gymId,
                                 AccessPlan.PlanType planType, Instant validUntil, Integer visitLimit) {
        access.requireOrganizationAdmin(actorId, organizationId);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (planType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planType is required");
        }
        if (visitLimit != null && visitLimit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visitLimit must be positive");
        }
        if (validUntil != null && !validUntil.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validUntil must be in the future");
        }
        if (gymId != null && !access.requireActiveGym(gymId).getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found in this organization");
        }
        return plans.save(new AccessPlan(organizationId, gymId, name.strip(), planType, validUntil, visitLimit));
    }

    @Transactional(readOnly = true)
    public List<AccessPlan> listPlans(UUID actorId, UUID organizationId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        return plans.findAllByOrganizationIdOrderByName(organizationId);
    }

    @Transactional
    public void archivePlan(UUID actorId, UUID organizationId, UUID planId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        AccessPlan plan = lockPlan(organizationId, planId);
        if (plan.getArchivedAt() == null) {
            plan.archive(Instant.now());
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberPass issuePass(UUID actorId, UUID organizationId, UUID planId, UUID memberId,
                                Instant startsAt, Instant expiresAt) {
        access.requireOrganizationAdmin(actorId, organizationId);
        return issuePassInScope(organizationId, null, planId, memberId, startsAt, expiresAt);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberPass issuePassAtGym(UUID actorId, UUID gymId, UUID planId, UUID memberId,
                                     Instant startsAt, Instant expiresAt) {
        Gym gym = access.requireFrontDeskAccess(actorId, gymId);
        return issuePassInScope(gym.getOrganizationId(), gymId, planId, memberId, startsAt, expiresAt);
    }

    private MemberPass issuePassInScope(UUID organizationId, UUID gymId, UUID planId, UUID memberId,
                                        Instant startsAt, Instant expiresAt) {
        // Issuance can conflict across different plans in the same organization.
        organizations.findForUpdate(organizationId).orElseThrow();
        AccessPlan plan = lockPlan(organizationId, planId);
        if (gymId != null && plan.getGymId() != null && !plan.getGymId().equals(gymId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Access plan not found at this gym");
        }
        Instant now = Instant.now();
        if (plan.getArchivedAt() != null || (plan.getValidUntil() != null && !plan.getValidUntil().isAfter(now))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Access plan is closed to new passes");
        }
        if (plan.getGymId() != null) {
            access.requireActiveGym(plan.getGymId());
        }
        Instant start = startsAt == null ? now : startsAt;
        if (memberId == null || expiresAt == null || !expiresAt.isAfter(start) || !expiresAt.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "accountId is required and expiresAt must be after startsAt and in the future");
        }
        requireCompatiblePasses(plan, memberId, start.isBefore(now) ? now : start, expiresAt);
        return passes.save(new MemberPass(plan.getId(), memberId, start, expiresAt));
    }

    @Transactional
    public void revokePass(UUID actorId, UUID organizationId, UUID passId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        MemberPass pass = passes.findInOrganizationForUpdate(passId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member pass not found"));
        if (pass.getRevokedAt() == null) {
            pass.revoke(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public List<MemberPass> listMemberPasses(UUID accountId) {
        return passes.findAllByAccountIdOrderByExpiresAtDesc(accountId);
    }

    private AccessPlan lockPlan(UUID organizationId, UUID planId) {
        return plans.findForUpdate(planId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access plan not found"));
    }

    private void requireCompatiblePasses(AccessPlan plan, UUID memberId, Instant after, Instant before) {
        if (plan.getPlanType() == null) {
            throw new PassIssuanceConflict("UNCLASSIFIED_PLAN",
                    "Choose a plan with a membership or pass type.", List.of());
        }
        List<MemberPassRepository.OverlappingPass> overlaps =
                passes.findUsableOverlaps(plan.getOrganizationId(), memberId, after, before,
                        AccessPlan.PlanType.MEMBERSHIP);
        List<UUID> memberships = overlaps.stream()
                .filter(pass -> pass.getPlanType() == AccessPlan.PlanType.MEMBERSHIP)
                .map(MemberPassRepository.OverlappingPass::getId).toList();
        if (!memberships.isEmpty()) {
            throw new PassIssuanceConflict("EXISTING_MEMBERSHIP",
                    "The member already has a membership covering this period.", memberships);
        }
        List<UUID> unclassified = overlaps.stream().filter(pass -> pass.getPlanType() == null)
                .map(MemberPassRepository.OverlappingPass::getId).toList();
        if (!unclassified.isEmpty()) {
            throw new PassIssuanceConflict("UNCLASSIFIED_PASS",
                    "Review older passes before issuing another pass.", unclassified);
        }
        if (plan.getPlanType() == AccessPlan.PlanType.MEMBERSHIP && !overlaps.isEmpty()) {
            throw new PassIssuanceConflict("EXISTING_PASSES_REVIEW",
                    "The member has unused passes covering this period.",
                    overlaps.stream().map(MemberPassRepository.OverlappingPass::getId).toList());
        }
    }
}
