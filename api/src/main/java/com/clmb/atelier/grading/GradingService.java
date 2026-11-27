package com.clmb.atelier.grading;

import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.grading.dtos.GradeRequest;
import com.clmb.atelier.grading.dtos.GradeResponse;
import com.clmb.atelier.grading.dtos.GradingSystemRequest;
import com.clmb.atelier.grading.dtos.GradingSystemResponse;
import com.clmb.atelier.grading.dtos.GymGradingSystem;
import com.clmb.atelier.grading.entities.Grade;
import com.clmb.atelier.grading.entities.GradingSystem;
import com.clmb.atelier.gyms.entities.Gym;
import com.clmb.atelier.organizations.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
public class GradingService {
    private final GradingSystemRepository systems;
    private final GradeRepository grades;
    private final OrganizationRepository organizations;
    private final AdministrationAccess access;

    public GradingService(GradingSystemRepository systems, GradeRepository grades,
                          OrganizationRepository organizations, AdministrationAccess access) {
        this.systems = systems;
        this.grades = grades;
        this.organizations = organizations;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<GradingSystemResponse> listSystems(UUID actorId, UUID gymId) {
        var gym = access.requireGymStaff(actorId, gymId);
        var available = systems.findAvailableAtGym(gymId, gym.getOrganizationId());
        if (available.isEmpty()) {
            return List.of();
        }

        var ids = available.stream().map(GradingSystem::getId).toList();
        var bySystem = grades.findByGradingSystemIdInOrderBySortOrderAscLabelAsc(ids).stream()
                .collect(groupingBy(Grade::getGradingSystemId, mapping(GradeResponse::from, toList())));
        return available.stream()
                .map(system -> GradingSystemResponse.from(system, bySystem.getOrDefault(system.getId(), List.of())))
                .toList();
    }

    @Transactional
    public GradingSystemResponse createGymSystem(UUID actorId, UUID gymId, GradingSystemRequest request) {
        return createSystem(access.requireGymStaff(actorId, gymId), gymId, request);
    }

    @Transactional
    public GradingSystemResponse createSharedSystem(UUID actorId, UUID gymId, GradingSystemRequest request) {
        return createSystem(access.requireGymStaff(actorId, gymId), null, request);
    }

    private GradingSystemResponse createSystem(Gym gym, UUID ownerGymId, GradingSystemRequest request) {
        var organization = organizations.getReferenceById(gym.getOrganizationId());
        var system = systems.save(new GradingSystem(ownerGymId, organization,
                requiredText(request.name(), "name"), request.description()));
        return GradingSystemResponse.from(system, List.of());
    }

    @Transactional(readOnly = true)
    public GymGradingSystem getSelectedSystem(UUID actorId, UUID gymId) {
        return new GymGradingSystem(access.requireGymStaff(actorId, gymId).getGradingSystemId());
    }

    @Transactional
    public GymGradingSystem selectSystem(UUID actorId, UUID gymId, UUID systemId) {
        var gym = access.requireGymStaff(actorId, gymId);
        if (systemId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gradingSystemId is required");
        }
        var system = systems.findAvailableAtGym(systemId, gymId, gym.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grading system not found at this gym"));
        gym.selectGradingSystem(system);
        return new GymGradingSystem(gym.getGradingSystemId());
    }

    @Transactional
    public GradeResponse addGrade(UUID actorId, UUID gymId, UUID systemId, GradeRequest request) {
        var gym = access.requireGymStaff(actorId, gymId);
        // Serialize additions to the same system so duplicate labels return a conflict on concurrent requests.
        systems.findEditableAtGym(systemId, gymId, gym.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Private grading system not found"));
        String label = requiredText(request.label(), "label");
        if (request.sortOrder() == null || request.sortOrder() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortOrder must be zero or greater");
        }
        if (grades.existsByGradingSystemIdAndLabel(systemId, label)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Grade label already exists in this system");
        }
        return GradeResponse.from(grades.save(new Grade(systemId, label, request.description(), request.sortOrder())));
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank() || value.strip().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must contain 1 to 255 characters");
        }
        return value.strip();
    }
}
