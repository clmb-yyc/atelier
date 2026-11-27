package com.clmb.atelier.grading.dtos;

import com.clmb.atelier.grading.entities.GradingSystem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GradingSystemResponse(UUID id, UUID gymId, UUID organizationId, String name, String description,
                                    GradingSystem.Visibility visibility, Instant createdAt,
                                    List<GradeResponse> grades) {
    public static GradingSystemResponse from(GradingSystem system, List<GradeResponse> grades) {
        return new GradingSystemResponse(system.getId(), system.getGymId(), system.getOrganizationId(), system.getName(),
                system.getDescription(), system.getVisibility(), system.getCreatedAt(), grades);
    }
}
