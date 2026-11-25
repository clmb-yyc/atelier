package com.clmb.atelier.access;

import com.clmb.atelier.administration.AdministrationAccess;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public MembershipService(AccessPlanRepository plans, MemberPassRepository passes, AdministrationAccess access) {
        this.plans = plans;
        this.passes = passes;
        this.access = access;
    }

    @Transactional
    public AccessPlan createPlan(UUID actorId, UUID organizationId, String name, UUID gymId,
                                 Instant validUntil, Integer visitLimit) {
        access.requireOrganizationAdmin(actorId, organizationId);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
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
        return plans.save(new AccessPlan(organizationId, gymId, name.strip(), validUntil, visitLimit));
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

    @Transactional
    public MemberPass issuePass(UUID actorId, UUID organizationId, UUID planId, UUID memberId,
                                Instant startsAt, Instant expiresAt) {
        access.requireOrganizationAdmin(actorId, organizationId);
        AccessPlan plan = lockPlan(organizationId, planId);
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
}
