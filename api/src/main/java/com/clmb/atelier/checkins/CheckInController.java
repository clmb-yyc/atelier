package com.clmb.atelier.checkins;

import com.clmb.atelier.checkins.entities.CheckIn;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static com.clmb.atelier.config.JwtAccount.accountId;

@RestController
public class CheckInController {
    private final CheckInService checkIns;

    public CheckInController(CheckInService checkIns) {
        this.checkIns = checkIns;
    }

    @PostMapping("/api/gyms/{gymId}/check-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInResponse checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                   @RequestBody CheckInRequest request) {
        CheckIn checkIn = checkIns.checkIn(accountId(jwt), gymId, request.userPassId());
        return new CheckInResponse(checkIn.getId(), checkIn.getGymId(), checkIn.getUserPassId(),
                checkIn.getCheckedInAt());
    }

    public record CheckInRequest(UUID userPassId) {}
    public record CheckInResponse(UUID id, UUID gymId, UUID userPassId, Instant checkedInAt) {}
}
