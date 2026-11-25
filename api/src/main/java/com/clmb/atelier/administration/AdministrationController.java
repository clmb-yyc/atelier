package com.clmb.atelier.administration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.clmb.atelier.config.JwtAccount.accountId;

@RestController
@RequestMapping("/api")
public class AdministrationController {
    private final AdministrationService administration;

    public AdministrationController(AdministrationService administration) {
        this.administration = administration;
    }

    @PutMapping("/organizations/{organizationId}/admins/{accountId}")
    public ResponseEntity<Void> grantOrganizationAdmin(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID organizationId, @PathVariable UUID accountId) {
        return grantResponse(administration.grantOrganizationAdmin(accountId(jwt), organizationId, accountId));
    }

    @PutMapping("/gyms/{gymId}/admins/{accountId}")
    public ResponseEntity<Void> grantGymAdmin(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID gymId, @PathVariable UUID accountId) {
        return grantResponse(administration.grantGymAdmin(accountId(jwt), gymId, accountId));
    }

    @PutMapping("/gyms/{gymId}/staff/{accountId}")
    public ResponseEntity<Void> grantGymStaff(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID gymId, @PathVariable UUID accountId) {
        return grantResponse(administration.grantGymStaff(accountId(jwt), gymId, accountId));
    }

    private static ResponseEntity<Void> grantResponse(boolean created) {
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }
}
