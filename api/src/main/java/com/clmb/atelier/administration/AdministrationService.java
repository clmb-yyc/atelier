package com.clmb.atelier.administration;

import com.clmb.atelier.gyms.Gym;
import com.clmb.atelier.gyms.GymAdmin;
import com.clmb.atelier.gyms.GymAdminRepository;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.organizations.Organization;
import com.clmb.atelier.organizations.OrganizationAdmin;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.routes.Route;
import com.clmb.atelier.routes.RouteRepository;
import com.clmb.atelier.walls.Wall;
import com.clmb.atelier.walls.WallRepository;
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
    private final GymAdminRepository gymAdmins;
    private final WallRepository walls;
    private final RouteRepository routes;

    public AdministrationService(
            OrganizationRepository organizations, OrganizationAdminRepository organizationAdmins,
            GymRepository gyms, GymAdminRepository gymAdmins, WallRepository walls, RouteRepository routes) {
        this.organizations = organizations;
        this.organizationAdmins = organizationAdmins;
        this.gyms = gyms;
        this.gymAdmins = gymAdmins;
        this.walls = walls;
        this.routes = routes;
    }

    public Organization createOrganization(UUID actorId, String name) {
        Organization organization = organizations.saveAndFlush(new Organization(requiredText(name, "name")));
        organizationAdmins.grant(organization.getId(), actorId);
        return organization;
    }

    public Gym createGym(UUID actorId, UUID organizationId, String name) {
        requireActiveOrganization(organizationId);
        requireOrganizationAdmin(actorId, organizationId);
        return gyms.save(new Gym(organizationId, requiredText(name, "name")));
    }

    public boolean grantOrganizationAdmin(UUID actorId, UUID organizationId, UUID accountId) {
        requireActiveOrganization(organizationId);
        requireOrganizationAdmin(actorId, organizationId);
        return organizationAdmins.grant(organizationId, accountId) == 1;
    }

    public boolean grantGymAdmin(UUID actorId, UUID gymId, UUID accountId) {
        Gym gym = requireActiveGym(gymId);
        requireOrganizationAdmin(actorId, gym.getOrganizationId());
        return gymAdmins.grant(gymId, accountId) == 1;
    }

    public Wall createWall(UUID actorId, UUID gymId, String name, Wall.Discipline discipline) {
        requireGymAdmin(actorId, gymId);
        if (discipline == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discipline is required");
        }
        return walls.save(new Wall(gymId, requiredText(name, "name"), discipline));
    }

    public Route createRoute(UUID actorId, UUID gymId, UUID wallId, String name, String grade) {
        requireGymAdmin(actorId, gymId);
        walls.findByIdAndGymIdAndArchivedAtIsNull(wallId, gymId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wall not found in this gym"));
        String routeName = name == null || name.isBlank() ? null : name.strip();
        return routes.save(new Route(wallId, routeName, requiredText(grade, "grade"), Instant.now()));
    }

    private void requireActiveOrganization(UUID organizationId) {
        organizations.findById(organizationId)
                .filter(organization -> organization.getArchivedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private Gym requireActiveGym(UUID gymId) {
        Gym gym = gyms.findById(gymId).filter(candidate -> candidate.getArchivedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));
        requireActiveOrganization(gym.getOrganizationId());
        return gym;
    }

    private void requireGymAdmin(UUID actorId, UUID gymId) {
        Gym gym = requireActiveGym(gymId);
        if (!isOrganizationAdmin(actorId, gym.getOrganizationId())
                && !gymAdmins.existsById(new GymAdmin.Key(gymId, actorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gym admin access required");
        }
    }

    private void requireOrganizationAdmin(UUID actorId, UUID organizationId) {
        if (!isOrganizationAdmin(actorId, organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization admin access required");
        }
    }

    private boolean isOrganizationAdmin(UUID actorId, UUID organizationId) {
        return organizationAdmins.existsById(new OrganizationAdmin.Key(organizationId, actorId));
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.strip();
    }
}
