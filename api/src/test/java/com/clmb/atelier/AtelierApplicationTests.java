package com.clmb.atelier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtelierApplicationTests {
    private static final String ROUTES = "/api/locations/calgary/routes";
    private static final String AUTH_ISSUER = "clmb-auth";

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
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

    @Test
    void organizationAdminsCanOnboardGymsAndGrantScopedAccess() throws Exception {
        UUID organizationAdminId = UUID.randomUUID();
        UUID gymAdminId = UUID.randomUUID();
        String creator = token("ROLE_STAFF");
        String organizationAdmin = token("ROLE_VIEWER", organizationAdminId);
        String gymAdmin = token("ROLE_VIEWER", gymAdminId);
        String otherStaff = token("ROLE_STAFF");

        postJson("/api/organizations", gymAdmin, "{\"name\":\"Outside Co\"}")
                .andExpect(status().isForbidden());
        String organizationId = createdId(postJson("/api/organizations", creator, "{\"name\":\"Climbing Co\"}"));
        String otherOrganizationId = createdId(postJson("/api/organizations", otherStaff, "{\"name\":\"Other Co\"}"));
        String gymsPath = "/api/organizations/" + organizationId + "/gyms";
        postJson(gymsPath, otherStaff, "{\"name\":\"Unauthorized\"}").andExpect(status().isForbidden());

        String orgAdminPath = "/api/organizations/" + organizationId + "/admins/" + organizationAdminId;
        grant(orgAdminPath, creator).andExpect(status().isCreated());
        grant(orgAdminPath, creator).andExpect(status().isNoContent());
        String gymId = createdId(postJson(gymsPath, organizationAdmin, "{\"name\":\"Downtown\"}"));
        String siblingGymId = createdId(postJson(gymsPath, creator, "{\"name\":\"North\"}"));
        String otherGymId = createdId(postJson("/api/organizations/" + otherOrganizationId + "/gyms",
                otherStaff, "{\"name\":\"West\"}"));

        String gymAdminPath = "/api/gyms/" + gymId + "/admins/" + gymAdminId;
        grant(gymAdminPath, otherStaff).andExpect(status().isForbidden());
        grant(gymAdminPath, organizationAdmin).andExpect(status().isCreated());
        grant(gymAdminPath, organizationAdmin).andExpect(status().isNoContent());
        grant("/api/gyms/" + gymId + "/admins/" + UUID.randomUUID(), gymAdmin)
                .andExpect(status().isForbidden());
        grant("/api/organizations/" + organizationId + "/admins/" + gymAdminId, gymAdmin)
                .andExpect(status().isForbidden());

        String wall = "{\"name\":\"Slab\",\"discipline\":\"BOULDER\"}";
        String gymWallsPath = "/api/gyms/" + gymId + "/walls";
        String wallId = createdId(postJson(gymWallsPath, gymAdmin, wall));
        String siblingWallId = createdId(postJson("/api/gyms/" + siblingGymId + "/walls", organizationAdmin, wall));
        postJson("/api/gyms/" + siblingGymId + "/walls", gymAdmin, wall).andExpect(status().isForbidden());
        postJson("/api/gyms/" + otherGymId + "/walls", organizationAdmin, wall).andExpect(status().isForbidden());

        postJson(gymWallsPath + "/" + wallId + "/routes", gymAdmin, "{\"grade\":\"V4\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wallId").value(wallId))
                .andExpect(jsonPath("$.grade").value("V4"));
        postJson(gymWallsPath + "/" + siblingWallId + "/routes", gymAdmin, "{\"grade\":\"V4\"}")
                .andExpect(status().isNotFound());
        postJson(gymWallsPath + "/" + wallId + "/routes", gymAdmin, "{\"grade\":\" \"}")
                .andExpect(status().isBadRequest());
    }

    private ResultActions postJson(String path, String token, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions grant(String path, String token) throws Exception {
        return mvc.perform(put(path).header("Authorization", "Bearer " + token));
    }

    private String createdId(ResultActions result) throws Exception {
        String response = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asString();
    }

    private String token(String role) {
        return token(role, UUID.randomUUID());
    }

    private String token(String role, UUID accountId) {
        return token(role, AUTH_ISSUER, Instant.now(), Base64.getDecoder().decode(signingSecret), accountId);
    }

    private String token(String role, String issuer, Instant issuedAt, byte[] key) {
        return token(role, issuer, issuedAt, key, UUID.randomUUID());
    }

    private String token(String role, String issuer, Instant issuedAt, byte[] key, UUID accountId) {
        // Match the auth service's access-token claims and signing algorithm.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(accountId.toString())
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
