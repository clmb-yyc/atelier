package com.clmb.atelier.organizations;

import com.clmb.atelier.administration.AdministrationService;
import com.clmb.atelier.gyms.Gym;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.clmb.atelier.config.JwtAccount.accountId;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationsController {
    private final AdministrationService administration;

    public OrganizationsController(AdministrationService administration) {
        this.administration = administration;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrganization(@AuthenticationPrincipal Jwt jwt, @RequestBody NameRequest request) {
        Organization organization = administration.createOrganization(accountId(jwt), request.name());
        return new OrganizationResponse(organization.getId(), organization.getName());
    }

    @PostMapping("/{organizationId}/gyms")
    @ResponseStatus(HttpStatus.CREATED)
    public GymResponse createGym(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                                 @RequestBody NameRequest request) {
        Gym gym = administration.createGym(accountId(jwt), organizationId, request.name());
        return new GymResponse(gym.getId(), gym.getOrganizationId(), gym.getName());
    }

    public record NameRequest(String name) {}
    public record OrganizationResponse(UUID id, String name) {}
    public record GymResponse(UUID id, UUID organizationId, String name) {}
}
