package com.clmb.atelier.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class JwtAccount {
    private JwtAccount() {}

    public static UUID accountId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject != null) {
            try {
                return UUID.fromString(subject);
            } catch (IllegalArgumentException ignored) {
                // Auth uses UUID account IDs as token subjects.
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject must be an account UUID");
    }
}
