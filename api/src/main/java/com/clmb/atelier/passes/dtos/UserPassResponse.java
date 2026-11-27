package com.clmb.atelier.passes.dtos;

import com.clmb.atelier.passes.entities.UserPass;

import java.time.Instant;
import java.util.UUID;

public record UserPassResponse(UUID id, UUID passId, UUID userId, Instant startsAt, Instant expiresAt,
                           Instant revokedAt) {
    public static UserPassResponse from(UserPass userPass) {
        return new UserPassResponse(userPass.getId(), userPass.getPassId(), userPass.getUserId(), userPass.getStartsAt(),
                userPass.getExpiresAt(), userPass.getRevokedAt());
    }
}
