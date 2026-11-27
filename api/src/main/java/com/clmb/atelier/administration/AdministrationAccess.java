package com.clmb.atelier.administration;

import com.clmb.atelier.gyms.GymAdminRepository;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.gyms.entities.Gym;
import com.clmb.atelier.gyms.entities.GymAdmin;
import com.clmb.atelier.gyms.entities.GymStaff;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.organizations.entities.OrganizationAdmin;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class AdministrationAccess {
    private final OrganizationRepository organizations;
    private final OrganizationAdminRepository organizationAdmins;
    private final GymRepository gyms;
    private final GymAdminRepository gymAdmins;
    private final GymStaffRepository staff;

    public AdministrationAccess(OrganizationRepository organizations, OrganizationAdminRepository organizationAdmins,
                                GymRepository gyms, GymAdminRepository gymAdmins, GymStaffRepository staff) {
        this.organizations = organizations;
        this.organizationAdmins = organizationAdmins;
        this.gyms = gyms;
        this.gymAdmins = gymAdmins;
        this.staff = staff;
    }

    public void requireActiveOrganization(UUID organizationId) {
        organizations.findById(organizationId)
                .filter(organization -> organization.getArchivedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    public Gym requireActiveGym(UUID gymId) {
        Gym gym = gyms.findById(gymId).filter(candidate -> candidate.getArchivedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));
        requireActiveOrganization(gym.getOrganizationId());
        return gym;
    }

    public void requireGymAdmin(UUID actorId, UUID gymId) {
        Gym gym = requireActiveGym(gymId);
        if (!isGymAdmin(actorId, gym)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gym admin access required");
        }
    }

    public Gym requireGymStaffOrAdmin(UUID actorId, UUID gymId) {
        Gym gym = requireActiveGym(gymId);
        if (!isGymAdmin(actorId, gym) && !staff.existsById(new GymStaff.Key(gymId, actorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gym staff or admin access required");
        }
        return gym;
    }

    public void requireOrganizationAdmin(UUID actorId, UUID organizationId) {
        requireActiveOrganization(organizationId);
        if (!isOrganizationAdmin(actorId, organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization admin access required");
        }
    }

    private boolean isOrganizationAdmin(UUID actorId, UUID organizationId) {
        return organizationAdmins.existsById(new OrganizationAdmin.Key(organizationId, actorId));
    }

    private boolean isGymAdmin(UUID actorId, Gym gym) {
        return isOrganizationAdmin(actorId, gym.getOrganizationId())
                || gymAdmins.existsById(new GymAdmin.Key(gym.getId(), actorId));
    }
}
