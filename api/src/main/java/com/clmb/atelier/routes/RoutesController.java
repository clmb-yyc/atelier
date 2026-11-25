package com.clmb.atelier.routes;

import com.clmb.atelier.administration.AdministrationService;
import com.clmb.atelier.walls.Wall;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.clmb.atelier.config.JwtAccount.accountId;

@RestController
@RequestMapping("/api/gyms/{gymId}")
public class RoutesController {
    private final AdministrationService administration;
    private final RoutesService routes;

    public RoutesController(AdministrationService administration, RoutesService routes) {
        this.administration = administration;
        this.routes = routes;
    }

    @GetMapping("/routes")
    public List<RouteListItem> listRoutes(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId) {
        return routes.listActiveRoutes(accountId(jwt), gymId);
    }

    @PostMapping("/walls")
    @ResponseStatus(HttpStatus.CREATED)
    public WallResponse createWall(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                   @RequestBody WallRequest request) {
        Wall wall = administration.createWall(accountId(jwt), gymId, request.name(), request.discipline());
        return new WallResponse(wall.getId(), wall.getGymId(), wall.getName(), wall.getDiscipline());
    }

    @PostMapping("/walls/{wallId}/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse createRoute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                     @PathVariable UUID wallId, @RequestBody RouteRequest request) {
        Route route = administration.createRoute(accountId(jwt), gymId, wallId, request.name(), request.grade());
        return new RouteResponse(route.getId(), route.getWallId(), route.getName(), route.getGrade(), route.getSetAt());
    }

    public record WallRequest(String name, Wall.Discipline discipline) {}
    public record RouteRequest(String name, String grade) {}
    public record WallResponse(UUID id, UUID gymId, String name, Wall.Discipline discipline) {}
    public record RouteResponse(UUID id, UUID wallId, String name, String grade, Instant setAt) {}
}
