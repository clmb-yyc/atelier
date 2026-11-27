package com.clmb.atelier.grading.dtos;

import com.clmb.atelier.grading.entities.Grade;

import java.time.Instant;
import java.util.UUID;

public record GradeResponse(UUID id, UUID gradingSystemId, String label, String description, int sortOrder,
                            Instant createdAt) {
    public static GradeResponse from(Grade grade) {
        return new GradeResponse(grade.getId(), grade.getGradingSystemId(), grade.getLabel(), grade.getDescription(),
                grade.getSortOrder(), grade.getCreatedAt());
    }
}
