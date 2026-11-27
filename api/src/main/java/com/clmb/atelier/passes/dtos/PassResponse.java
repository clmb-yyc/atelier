package com.clmb.atelier.passes.dtos;

import com.clmb.atelier.passes.entities.Pass;

import java.time.Instant;
import java.util.UUID;

public record PassResponse(UUID id, UUID organizationId, UUID gymId, String name, Pass.PassType passType,
                           Instant validUntil, Integer visitLimit, Instant archivedAt) {
    public static PassResponse from(Pass pass) {
        return new PassResponse(pass.getId(), pass.getOrganizationId(), pass.getGymId(), pass.getName(),
                pass.getPassType(), pass.getValidUntil(), pass.getVisitLimit(), pass.getArchivedAt());
    }
}
