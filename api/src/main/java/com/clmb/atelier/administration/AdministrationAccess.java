package com.clmb.atelier.administration;

import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.gyms.entities.Gym;
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
    private final GymStaffRepository staff;

    public AdministrationAccess(OrganizationRepository organizations, OrganizationAdminRepository organizationAdmins,
                                GymRepository gyms, GymStaffRepository staff) {
        this.organizations = organizations;
        this.organizationAdmins = organizationAdmins;
        this.gyms = gyms;
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

    public Gym requireOrganizationAdminForGym(UUID actorId, UUID gymId) {
        Gym gym = requireActiveGym(gymId);
        checkOrganizationAdmin(actorId, gym.getOrganizationId());
        return gym;
    }

    public Gym requireGymStaffOrAdmin(UUID actorId, UUID gymId) {
        Gym gym = requireActiveGym(gymId);
        if (!isOrganizationAdmin(actorId, gym.getOrganizationId()) && !isGymStaff(actorId, gymId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gym staff or organization admin access required");
        }
        return gym;
    }

    public Gym requireGymStaff(UUID actorId, UUID gymId) {
        Gym gym = requireActiveGym(gymId);
        if (!isGymStaff(actorId, gymId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gym staff access required");
        }
        return gym;
    }

    public void requireOrganizationAdmin(UUID actorId, UUID organizationId) {
        requireActiveOrganization(organizationId);
        checkOrganizationAdmin(actorId, organizationId);
    }

    private void checkOrganizationAdmin(UUID actorId, UUID organizationId) {
        if (!isOrganizationAdmin(actorId, organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization admin access required");
        }
    }

    private boolean isOrganizationAdmin(UUID actorId, UUID organizationId) {
        return organizationAdmins.existsById(new OrganizationAdmin.Key(organizationId, actorId));
    }

    private boolean isGymStaff(UUID actorId, UUID gymId) {
        return staff.existsById(new GymStaff.Key(gymId, actorId));
    }
}
