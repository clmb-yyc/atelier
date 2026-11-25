package com.clmb.atelier;

import com.clmb.atelier.gyms.Gym;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaff;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.organizations.Organization;
import com.clmb.atelier.organizations.OrganizationAdmin;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.routes.Route;
import com.clmb.atelier.routes.RouteRepository;
import com.clmb.atelier.walls.Wall;
import com.clmb.atelier.walls.WallRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtelierApplicationTests {
    private static final String AUTH_ISSUER = "clmb-auth";

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired OrganizationRepository organizations;
    @Autowired OrganizationAdminRepository organizationAdmins;
    @Autowired GymRepository gyms;
    @Autowired GymStaffRepository gymStaff;
    @Autowired WallRepository walls;
    @Autowired RouteRepository routes;
    @Value("${atelier.jwt.secret}") String signingSecret;

    @Test
    void listsActiveRoutesAtTheRequestedGymWithScopedAccess() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID organization = organizations.save(new Organization("Climbing Co")).getId();
        organizationAdmins.save(new OrganizationAdmin(organization, owner));
        UUID gym = gyms.save(new Gym(organization, "Downtown")).getId();
        UUID sibling = gyms.save(new Gym(organization, "North")).getId();
        UUID empty = gyms.save(new Gym(organization, "South")).getId();
        gymStaff.save(new GymStaff(gym, employee));
        Wall active = walls.save(new Wall(gym, "Slab", Wall.Discipline.BOULDER));
        Wall archived = new Wall(gym, "Old slab", Wall.Discipline.BOULDER);
        Instant now = Instant.now();
        archived.archive(now);
        archived = walls.save(archived);
        Wall siblingWall = walls.save(new Wall(sibling, "Rope wall", Wall.Discipline.ROPE));
        Route older = routes.save(new Route(active.getId(), null, "V2", now.minusSeconds(360)));
        Route newer = routes.save(new Route(active.getId(), "Slab start", "V4", now.minusSeconds(60)));
        Route removed = new Route(active.getId(), "Removed", "V1", now.minusSeconds(300));
        removed.remove(now);
        routes.save(removed);
        routes.save(new Route(archived.getId(), "Archived wall", "V3", now.minusSeconds(600)));
        routes.save(new Route(siblingWall.getId(), "North only", "5.10a", now.minusSeconds(60)));

        String staffToken = token("ROLE_VIEWER", employee);
        String ownerToken = token("ROLE_STAFF", owner);
        mvc.perform(get("/api/gyms/{gymId}/routes", gym).header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer.getId().toString()))
                .andExpect(jsonPath("$[0].wallId").value(active.getId().toString()))
                .andExpect(jsonPath("$[0].wallName").value("Slab"))
                .andExpect(jsonPath("$[0].discipline").value("BOULDER"))
                .andExpect(jsonPath("$[0].grade").value("V4"))
                .andExpect(jsonPath("$[1].id").value(older.getId().toString()));
        mvc.perform(get("/api/gyms/{gymId}/routes", gym).header("Authorization", "Bearer " + token("ROLE_STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/gyms/{gymId}/routes", sibling).header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/gyms/{gymId}/routes", empty).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/gyms/{gymId}/routes", UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissingExpiredWrongIssuerAndWrongSignatureTokens() throws Exception {
        byte[] key = Base64.getDecoder().decode(signingSecret);
        Instant now = Instant.now();
        String wrongIssuer = token("ROLE_STAFF", "clmb-atelier", now, key);
        String expired = token("ROLE_STAFF", AUTH_ISSUER, now.minusSeconds(1200), key);
        key[0] ^= 1;
        String wrongSignature = token("ROLE_STAFF", AUTH_ISSUER, now, key);

        String path = "/api/gyms/" + UUID.randomUUID() + "/routes";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (String invalid : List.of(wrongIssuer, expired, wrongSignature, "opaque-refresh-token")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + invalid))
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
