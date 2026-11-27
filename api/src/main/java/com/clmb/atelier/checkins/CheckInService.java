package com.clmb.atelier.checkins;

import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.checkins.entities.CheckIn;
import com.clmb.atelier.gyms.entities.Gym;
import com.clmb.atelier.passes.PassRepository;
import com.clmb.atelier.passes.UserPassRepository;
import com.clmb.atelier.passes.entities.Pass;
import com.clmb.atelier.passes.entities.UserPass;
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
    private final UserPassRepository userPasses;
    private final PassRepository passes;
    private final AdministrationAccess access;

    public CheckInService(CheckInRepository checkIns, UserPassRepository userPasses, PassRepository passes,
                          AdministrationAccess access) {
        this.checkIns = checkIns;
        this.userPasses = userPasses;
        this.passes = passes;
        this.access = access;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckIn checkIn(UUID actorId, UUID gymId, UUID userPassId) {
        if (userPassId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userPassId is required");
        }
        Gym gym = access.requireGymStaffOrAdmin(actorId, gymId);
        // Hold the user pass lock across the visit count and insert so two requests cannot use the final visit.
        UserPass userPass = userPasses.findInOrganizationForUpdate(userPassId, gym.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User pass not found"));
        // Archival and issuance deadlines do not affect user passes already issued.
        Pass pass = passes.findById(userPass.getPassId()).orElseThrow();
        if (pass.getGymId() != null && !pass.getGymId().equals(gymId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User pass does not cover this gym");
        }

        Instant now = Instant.now();
        if (userPass.getRevokedAt() != null || userPass.getStartsAt().isAfter(now) || !userPass.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User pass is not active");
        }

        if (pass.getVisitLimit() != null && checkIns.countByUserPassId(userPassId) >= pass.getVisitLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User pass has no visits remaining");
        }
        return checkIns.save(new CheckIn(gymId, userPassId, now));
    }
}
