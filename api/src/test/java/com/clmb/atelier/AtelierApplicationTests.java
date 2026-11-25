package com.clmb.atelier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtelierApplicationTests {
    private static final String ROUTES = "/api/locations/calgary/routes";
    private static final String AUTH_ISSUER = "clmb-auth";

    @Autowired MockMvc mvc;
    @Value("${atelier.jwt.secret}") String signingSecret;

    @Test
    void acceptsViewerAndStaffTokensOnLocationReads() throws Exception {
        for (String role : List.of("ROLE_VIEWER", "ROLE_STAFF")) {
            mvc.perform(get(ROUTES).header("Authorization", "Bearer " + token(role)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("hello world"));
        }
    }

    @Test
    void requiresStaffForLocationWrites() throws Exception {
        mvc.perform(post(ROUTES).header("Authorization", "Bearer " + token("ROLE_VIEWER")))
                .andExpect(status().isForbidden());
        // STAFF passes security and reaches MVC, where this endpoint only supports GET.
        mvc.perform(post(ROUTES).header("Authorization", "Bearer " + token("ROLE_STAFF")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsMissingExpiredWrongIssuerAndWrongSignatureTokens() throws Exception {
        byte[] key = Base64.getDecoder().decode(signingSecret);
        Instant now = Instant.now();
        String wrongIssuer = token("ROLE_STAFF", "clmb-atelier", now, key);
        String expired = token("ROLE_STAFF", AUTH_ISSUER, now.minusSeconds(1200), key);
        key[0] ^= 1;
        String wrongSignature = token("ROLE_STAFF", AUTH_ISSUER, now, key);

        mvc.perform(get(ROUTES)).andExpect(status().isUnauthorized());
        for (String invalid : List.of(wrongIssuer, expired, wrongSignature, "opaque-refresh-token")) {
            mvc.perform(get(ROUTES).header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void accountEndpointsAreAbsentEvenWithAValidToken() throws Exception {
        String bearer = "Bearer " + token("ROLE_STAFF");
        for (String endpoint : List.of("/api/auth/signup", "/api/auth/login", "/api/auth/refresh")) {
            mvc.perform(post(endpoint).header("Authorization", bearer)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/users").header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    private String token(String role) {
        return token(role, AUTH_ISSUER, Instant.now(), Base64.getDecoder().decode(signingSecret));
    }

    private String token(String role, String issuer, Instant issuedAt, byte[] key) {
        // Match the auth service's access-token claims and signing algorithm.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("cb6f4a96-d493-4dd7-85bf-9d5dcc522036")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900))
                .claim("preferred_username", "climber")
                .claim("authorities", List.of(role))
                .build();
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
