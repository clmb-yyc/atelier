package com.clmb.atelier.administration;

import com.clmb.atelier.grading.GradeRepository;
import com.clmb.atelier.grading.entities.GradingSystem;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.gyms.entities.Gym;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.organizations.dtos.GymRequest;
import com.clmb.atelier.organizations.entities.Organization;
import com.clmb.atelier.routes.RouteRepository;
import com.clmb.atelier.routes.dtos.RouteRequest;
import com.clmb.atelier.routes.dtos.RouteResponse;
import com.clmb.atelier.routes.entities.Route;
import com.clmb.atelier.walls.WallRepository;
import com.clmb.atelier.walls.entities.Wall;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AdministrationService {
    private final OrganizationRepository organizations;
    private final OrganizationAdminRepository organizationAdmins;
    private final GymRepository gyms;
    private final GymStaffRepository staff;
    private final WallRepository walls;
    private final RouteRepository routes;
    private final GradeRepository grades;
    private final AdministrationAccess access;

    public AdministrationService(
            OrganizationRepository organizations, OrganizationAdminRepository organizationAdmins,
            GymRepository gyms, GymStaffRepository staff,
            WallRepository walls, RouteRepository routes, GradeRepository grades,
            AdministrationAccess access) {
        this.organizations = organizations;
        this.organizationAdmins = organizationAdmins;
        this.gyms = gyms;
        this.staff = staff;
        this.walls = walls;
        this.routes = routes;
        this.grades = grades;
        this.access = access;
    }

    public Organization createOrganization(UUID actorId, String name) {
        Organization organization = organizations.saveAndFlush(new Organization(requiredText(name, "name")));
        organizationAdmins.grant(organization.getId(), actorId);
        return organization;
    }

    public Gym createGym(UUID actorId, UUID organizationId, GymRequest request) {
        access.requireOrganizationAdmin(actorId, organizationId);
        if (request.latitude() == null || request.longitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude and longitude are required");
        }
        return gyms.save(new Gym(
                organizationId,
                requiredText(request.name(), "name"),
                requiredText(request.address(), "address"),
                requiredText(request.city(), "city"),
                requiredText(request.postalCode(), "postalCode"),
                requiredText(request.country(), "country"),
                request.latitude(),
                request.longitude(),
                requiredText(request.timezone(), "timezone")));
    }

    public boolean grantOrganizationAdmin(UUID actorId, UUID organizationId, UUID userId) {
        access.requireOrganizationAdmin(actorId, organizationId);
        return organizationAdmins.grant(organizationId, userId) == 1;
    }

    public boolean grantGymStaff(UUID actorId, UUID gymId, UUID userId) {
        access.requireOrganizationAdminForGym(actorId, gymId);
        return staff.grant(gymId, userId) == 1;
    }

    public Wall createWall(UUID actorId, UUID gymId, String name, Wall.Discipline discipline) {
        access.requireOrganizationAdminForGym(actorId, gymId);
        if (discipline == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discipline is required");
        }
        return walls.save(new Wall(gymId, requiredText(name, "name"), discipline));
    }

    public RouteResponse createRoute(UUID actorId, UUID gymId, UUID wallId, RouteRequest request) {
        access.requireOrganizationAdminForGym(actorId, gymId);
        walls.findByIdAndGymIdAndArchivedAtIsNull(wallId, gymId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wall not found in this gym"));
        if (request.gradeId() == null || request.setterId() == null || request.difficultyRating() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gradeId, setterId and difficultyRating are required");
        }
        var grade = grades.findAvailableAtGym(request.gradeId(), gymId, GradingSystem.Visibility.PUBLIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade not found at this gym"));
        String name = request.name();
        String routeName = name == null || name.isBlank() ? null : name.strip();
        Route route = routes.save(new Route(wallId, routeName, grade.getId(), Instant.now(),
                requiredText(request.colour(), "colour"), request.setterId(),
                requiredText(request.holdColour(), "holdColour"), request.difficultyRating()));
        return new RouteResponse(route.getId(), route.getWallId(), route.getName(), route.getGradeId(),
                grade.getLabel(), route.getSetAt());
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.strip();
    }
}
