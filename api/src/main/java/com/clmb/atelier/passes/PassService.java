package com.clmb.atelier.passes;

import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.gyms.entities.Gym;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.passes.dtos.PassRequest;
import com.clmb.atelier.passes.entities.Pass;
import com.clmb.atelier.passes.entities.UserPass;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PassService {
    private final PassRepository passes;
    private final UserPassRepository userPasses;
    private final AdministrationAccess access;
    private final OrganizationRepository organizations;

    public PassService(PassRepository passes, UserPassRepository userPasses, AdministrationAccess access,
                             OrganizationRepository organizations) {
        this.passes = passes;
        this.userPasses = userPasses;
        this.access = access;
        this.organizations = organizations;
    }

    @Transactional
    public Pass createPass(UUID actorId, UUID organizationId, PassRequest request) {
        access.requireOrganizationAdmin(actorId, organizationId);
        String name = request.name();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (request.passType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passType is required");
        }
        Integer visitLimit = request.visitLimit();
        if (visitLimit != null && visitLimit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visitLimit must be positive");
        }
        Instant validUntil = request.validUntil();
        if (validUntil != null && !validUntil.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validUntil must be in the future");
        }
        UUID gymId = request.gymId();
        if (gymId != null && !access.requireActiveGym(gymId).getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found in this organization");
        }
        if (request.price() == null || request.price().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be zero or greater");
        }
        String currency = request.currency();
        if (currency == null || !currency.strip().matches("[A-Za-z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency must be a three-letter code");
        }
        if (request.isRenewable() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isRenewable is required");
        }
        return passes.save(new Pass(organizationId, gymId, name.strip(), request.passType(), validUntil, visitLimit,
                request.price(), currency.strip().toUpperCase(Locale.ROOT), request.isRenewable()));
    }

    @Transactional(readOnly = true)
    public List<Pass> listPasses(UUID actorId, UUID organizationId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        return passes.findAllByOrganizationIdOrderByName(organizationId);
    }

    @Transactional
    public void archivePass(UUID actorId, UUID organizationId, UUID passId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        Pass pass = lockPass(organizationId, passId);
        if (pass.getArchivedAt() == null) {
            pass.archive(Instant.now());
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserPass issueUserPass(UUID actorId, UUID organizationId, UUID passId, UUID memberId,
                                Instant startsAt, Instant expiresAt) {
        access.requireOrganizationAdmin(actorId, organizationId);
        return issueUserPassInScope(actorId, organizationId, null, passId, memberId, startsAt, expiresAt);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserPass issueUserPassAtGym(UUID actorId, UUID gymId, UUID passId, UUID memberId,
                                     Instant startsAt, Instant expiresAt) {
        Gym gym = access.requireGymStaffOrAdmin(actorId, gymId);
        return issueUserPassInScope(actorId, gym.getOrganizationId(), gymId, passId, memberId, startsAt, expiresAt);
    }

    private UserPass issueUserPassInScope(UUID actorId, UUID organizationId, UUID gymId, UUID passId, UUID memberId,
                                         Instant startsAt, Instant expiresAt) {
        // Issuance can conflict across different passes in the same organization.
        organizations.findForUpdate(organizationId).orElseThrow();
        Pass pass = lockPass(organizationId, passId);
        if (gymId != null && pass.getGymId() != null && !pass.getGymId().equals(gymId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pass not found at this gym");
        }
        Instant now = Instant.now();
        if (pass.getArchivedAt() != null || (pass.getValidUntil() != null && !pass.getValidUntil().isAfter(now))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pass is closed to new user passes");
        }
        if (pass.getGymId() != null) {
            access.requireActiveGym(pass.getGymId());
        }
        Instant start = startsAt == null ? now : startsAt;
        if (memberId == null || expiresAt == null || !expiresAt.isAfter(start) || !expiresAt.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userId is required and expiresAt must be after startsAt and in the future");
        }
        requireCompatiblePasses(pass, memberId, start.isBefore(now) ? now : start, expiresAt);
        return userPasses.save(new UserPass(pass.getId(), memberId, start, expiresAt, now, actorId));
    }

    @Transactional
    public void revokeUserPass(UUID actorId, UUID organizationId, UUID userPassId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        UserPass userPass = userPasses.findInOrganizationForUpdate(userPassId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User pass not found"));
        if (userPass.getRevokedAt() == null) {
            userPass.revoke(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public List<UserPass> listUserPasses(UUID userId) {
        return userPasses.findAllByUserIdOrderByExpiresAtDesc(userId);
    }

    private Pass lockPass(UUID organizationId, UUID passId) {
        return passes.findForUpdate(passId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pass not found"));
    }

    private void requireCompatiblePasses(Pass pass, UUID memberId, Instant after, Instant before) {
        if (pass.getPassType() == null) {
            throw new PassIssuanceConflict("UNCLASSIFIED_PASS_TYPE",
                    "Choose a pass with a membership, day pass, or visit pass type.", List.of());
        }
        List<UserPassRepository.OverlappingUserPass> overlaps =
                userPasses.findUsableOverlaps(pass.getOrganizationId(), memberId, after, before,
                        Pass.PassType.MEMBERSHIP);
        List<UUID> memberships = overlaps.stream()
                .filter(userPass -> userPass.getPassType() == Pass.PassType.MEMBERSHIP)
                .map(UserPassRepository.OverlappingUserPass::getId).toList();
        if (!memberships.isEmpty()) {
            throw new PassIssuanceConflict("EXISTING_MEMBERSHIP",
                    "The member already has a membership covering this period.", memberships);
        }
        List<UUID> unclassified = overlaps.stream().filter(userPass -> userPass.getPassType() == null)
                .map(UserPassRepository.OverlappingUserPass::getId).toList();
        if (!unclassified.isEmpty()) {
            throw new PassIssuanceConflict("UNCLASSIFIED_PASS",
                    "Review older user passes before issuing another.", unclassified);
        }
        if (pass.getPassType() == Pass.PassType.MEMBERSHIP && !overlaps.isEmpty()) {
            throw new PassIssuanceConflict("EXISTING_PASSES_REVIEW",
                    "The member has unused user passes covering this period.",
                    overlaps.stream().map(UserPassRepository.OverlappingUserPass::getId).toList());
        }
    }
}
