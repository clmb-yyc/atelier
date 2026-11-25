package com.clmb.atelier.administration;

import com.clmb.atelier.gyms.Gym;
import com.clmb.atelier.organizations.Organization;
import com.clmb.atelier.routes.Route;
import com.clmb.atelier.walls.Wall;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static com.clmb.atelier.config.JwtAccount.accountId;

@RestController
@RequestMapping("/api")
public class AdministrationController {
    private final AdministrationService administration;

    public AdministrationController(AdministrationService administration) {
        this.administration = administration;
    }

    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrganization(@AuthenticationPrincipal Jwt jwt, @RequestBody NameRequest request) {
        Organization organization = administration.createOrganization(accountId(jwt), request.name());
        return new OrganizationResponse(organization.getId(), organization.getName());
    }

    @PostMapping("/organizations/{organizationId}/gyms")
    @ResponseStatus(HttpStatus.CREATED)
    public GymResponse createGym(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                                 @RequestBody NameRequest request) {
        Gym gym = administration.createGym(accountId(jwt), organizationId, request.name());
        return new GymResponse(gym.getId(), gym.getOrganizationId(), gym.getName());
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

    @PostMapping("/gyms/{gymId}/walls")
    @ResponseStatus(HttpStatus.CREATED)
    public WallResponse createWall(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                   @RequestBody WallRequest request) {
        Wall wall = administration.createWall(accountId(jwt), gymId, request.name(), request.discipline());
        return new WallResponse(wall.getId(), wall.getGymId(), wall.getName(), wall.getDiscipline());
    }

    @PostMapping("/gyms/{gymId}/walls/{wallId}/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse createRoute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                     @PathVariable UUID wallId, @RequestBody RouteRequest request) {
        Route route = administration.createRoute(accountId(jwt), gymId, wallId, request.name(), request.grade());
        return new RouteResponse(route.getId(), route.getWallId(), route.getName(), route.getGrade(), route.getSetAt());
    }

    private static ResponseEntity<Void> grantResponse(boolean created) {
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }

    public record NameRequest(String name) {}
    public record WallRequest(String name, Wall.Discipline discipline) {}
    public record RouteRequest(String name, String grade) {}
    public record OrganizationResponse(UUID id, String name) {}
    public record GymResponse(UUID id, UUID organizationId, String name) {}
    public record WallResponse(UUID id, UUID gymId, String name, Wall.Discipline discipline) {}
    public record RouteResponse(UUID id, UUID wallId, String name, String grade, Instant setAt) {}
}
