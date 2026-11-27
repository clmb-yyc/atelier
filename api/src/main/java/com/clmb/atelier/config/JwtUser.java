package com.clmb.atelier.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class JwtUser {
    private JwtUser() {}

    public static UUID userId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject != null) {
            try {
                return UUID.fromString(subject);
            } catch (IllegalArgumentException ignored) {
                // Auth uses UUID user IDs as token subjects.
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject must be a user UUID");
    }
}
