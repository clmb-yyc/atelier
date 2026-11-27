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

import static com.clmb.atelier.config.JwtUser.userId;

@RestController
@RequestMapping("/api")
public class AdministrationController {
    private final AdministrationService administration;

    public AdministrationController(AdministrationService administration) {
        this.administration = administration;
    }

    @PutMapping("/organizations/{organizationId}/admins/{userId}")
    public ResponseEntity<Void> grantOrganizationAdmin(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID organizationId, @PathVariable UUID userId) {
        return grantResponse(administration.grantOrganizationAdmin(userId(jwt), organizationId, userId));
    }

    @PutMapping("/gyms/{gymId}/staff/{userId}")
    public ResponseEntity<Void> grantGymStaff(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID gymId, @PathVariable UUID userId) {
        return grantResponse(administration.grantGymStaff(userId(jwt), gymId, userId));
    }

    private static ResponseEntity<Void> grantResponse(boolean created) {
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }
}
