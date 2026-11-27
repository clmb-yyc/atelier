package com.clmb.atelier.grading;

import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.grading.dtos.GradeRequest;
import com.clmb.atelier.grading.dtos.GradeResponse;
import com.clmb.atelier.grading.dtos.GradingSystemRequest;
import com.clmb.atelier.grading.dtos.GradingSystemResponse;
import com.clmb.atelier.grading.entities.Grade;
import com.clmb.atelier.grading.entities.GradingSystem;
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
    private final AdministrationAccess access;

    public GradingService(GradingSystemRepository systems, GradeRepository grades, AdministrationAccess access) {
        this.systems = systems;
        this.grades = grades;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<GradingSystemResponse> listSystems(UUID actorId, UUID gymId) {
        access.requireGymStaff(actorId, gymId);
        var available = systems.findByVisibilityOrGymIdOrderByNameAscIdAsc(GradingSystem.Visibility.PUBLIC, gymId);
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
    public GradingSystemResponse createSystem(UUID actorId, UUID gymId, GradingSystemRequest request) {
        access.requireGymStaff(actorId, gymId);
        var system = systems.save(new GradingSystem(gymId, requiredText(request.name(), "name"), request.description()));
        return GradingSystemResponse.from(system, List.of());
    }

    @Transactional
    public GradeResponse addGrade(UUID actorId, UUID gymId, UUID systemId, GradeRequest request) {
        access.requireGymStaff(actorId, gymId);
        // Serialize additions to the same system so duplicate labels return a conflict on concurrent requests.
        systems.findByIdAndGymId(systemId, gymId)
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
