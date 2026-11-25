package com.clmb.atelier.checkins;

import com.clmb.atelier.access.AccessPlan;
import com.clmb.atelier.access.AccessPlanRepository;
import com.clmb.atelier.access.MemberPass;
import com.clmb.atelier.access.MemberPassRepository;
import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.gyms.Gym;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class CheckInService {
    private final CheckInRepository checkIns;
    private final MemberPassRepository passes;
    private final AccessPlanRepository plans;
    private final AdministrationAccess access;

    public CheckInService(CheckInRepository checkIns, MemberPassRepository passes, AccessPlanRepository plans,
                          AdministrationAccess access) {
        this.checkIns = checkIns;
        this.passes = passes;
        this.plans = plans;
        this.access = access;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckIn checkIn(UUID accountId, UUID gymId, UUID passId) {
        if (passId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberPassId is required");
        }
        Gym gym = access.requireActiveGym(gymId);
        // Hold the pass lock across the visit count and insert so two requests cannot use the final visit.
        MemberPass pass = passes.findOwnedForUpdate(passId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member pass not found"));
        Instant now = Instant.now();
        if (pass.getRevokedAt() != null || pass.getStartsAt().isAfter(now) || !pass.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member pass is not active");
        }

        AccessPlan plan = plans.findById(pass.getAccessPlanId()).orElseThrow();
        // Plan archival and issuance deadlines do not affect passes that have already been issued.
        if (!plan.getOrganizationId().equals(gym.getOrganizationId())
                || (plan.getGymId() != null && !plan.getGymId().equals(gymId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member pass does not cover this gym");
        }
        if (plan.getVisitLimit() != null && checkIns.countByMemberPassId(passId) >= plan.getVisitLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member pass has no visits remaining");
        }
        return checkIns.save(new CheckIn(gymId, passId, now));
    }
}
