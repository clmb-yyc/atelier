package com.clmb.atelier;

import com.clmb.atelier.grading.GradeRepository;
import com.clmb.atelier.grading.entities.Grade;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.gyms.entities.GymStaff;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.organizations.entities.Organization;
import com.clmb.atelier.organizations.entities.OrganizationAdmin;
import com.clmb.atelier.routes.RouteRepository;
import com.clmb.atelier.routes.entities.Route;
import com.clmb.atelier.walls.WallRepository;
import com.clmb.atelier.walls.entities.Wall;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
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
    @Autowired GradeRepository grades;
    @Value("${atelier.jwt.secret}") String signingSecret;

    @Test
    void listsActiveRoutesAtTheRequestedGymWithScopedAccess() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID organization = organizations.save(new Organization("Climbing Co")).getId();
        organizationAdmins.save(new OrganizationAdmin(organization, owner));
        UUID gym = gyms.save(GymFixtures.gym(organization, "Downtown")).getId();
        UUID sibling = gyms.save(GymFixtures.gym(organization, "North")).getId();
        UUID empty = gyms.save(GymFixtures.gym(organization, "South")).getId();
        gymStaff.save(new GymStaff(gym, employee));
        Wall active = walls.save(new Wall(gym, "Slab", Wall.Discipline.BOULDER));
        Wall archived = new Wall(gym, "Old slab", Wall.Discipline.BOULDER);
        Instant now = Instant.now();
        archived.archive(now);
        archived = walls.save(archived);
        Wall siblingWall = walls.save(new Wall(sibling, "Rope wall", Wall.Discipline.ROPE));
        Route older = routes.save(new Route(active.getId(), null, gradeId("V2"), now.minusSeconds(360),
                "Blue", owner, "Blue"));
        Route newer = routes.save(new Route(active.getId(), "Slab start", gradeId("V4"), now.minusSeconds(60),
                "Blue", owner, "Blue"));
        Route removed = new Route(active.getId(), "Removed", gradeId("V1"), now.minusSeconds(300),
                "Blue", owner, "Blue");
        removed.remove(now);
        routes.save(removed);
        routes.save(new Route(archived.getId(), "Archived wall", gradeId("V3"), now.minusSeconds(600),
                "Blue", owner, "Blue"));
        routes.save(new Route(siblingWall.getId(), "North only", gradeId("V4"), now.minusSeconds(60),
                "Blue", owner, "Blue"));

        String staffToken = token(employee);
        String ownerToken = token(owner);
        mvc.perform(get("/api/gyms/{gymId}/routes", gym).header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer.getId().toString()))
                .andExpect(jsonPath("$[0].wallId").value(active.getId().toString()))
                .andExpect(jsonPath("$[0].wallName").value("Slab"))
                .andExpect(jsonPath("$[0].discipline").value("BOULDER"))
                .andExpect(jsonPath("$[0].gradeId").value(newer.getGradeId().toString()))
                .andExpect(jsonPath("$[0].grade").value("V4"))
                .andExpect(jsonPath("$[1].id").value(older.getId().toString()));
        mvc.perform(get("/api/gyms/{gymId}/routes", gym).header("Authorization", "Bearer " + token()))
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
        String wrongIssuer = token("clmb-atelier", now, key);
        String expired = token(AUTH_ISSUER, now.minusSeconds(1200), key);
        key[0] ^= 1;
        String wrongSignature = token(AUTH_ISSUER, now, key);

        String path = "/api/gyms/" + UUID.randomUUID() + "/routes";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (String invalid : List.of(wrongIssuer, expired, wrongSignature, "opaque-refresh-token")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void accountEndpointsAreAbsentEvenWithAValidToken() throws Exception {
        String bearer = "Bearer " + token();
        for (String endpoint : List.of("/api/auth/signup", "/api/auth/login", "/api/auth/refresh")) {
            mvc.perform(post(endpoint).header("Authorization", bearer)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/users").header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test
    void organizationAdminsCanOnboardGymsAndGrantScopedAccess() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID organizationAdminId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String creator = token(creatorId);
        String organizationAdmin = token(organizationAdminId);
        String employee = token(employeeId);
        String otherAdmin = token();

        mvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anonymous Co\"}"))
                .andExpect(status().isUnauthorized());
        String organizationId = createdId(postJson("/api/organizations", creator, "{\"name\":\"Climbing Co\"}"));
        assertThat(organizationAdmins.existsById(new OrganizationAdmin.Key(UUID.fromString(organizationId), creatorId)))
                .isTrue();
        String otherOrganizationId = createdId(postJson("/api/organizations", otherAdmin, "{\"name\":\"Other Co\"}"));
        String gymsPath = "/api/organizations/" + organizationId + "/gyms";
        postJson(gymsPath, otherAdmin, gymRequest("Unauthorized")).andExpect(status().isForbidden());
        postJson(gymsPath, creator, "{\"name\":\"Missing address and coordinates\"}")
                .andExpect(status().isBadRequest());
        postJson(gymsPath, creator, gymRequest("Missing address").replace("\"100 Main St\"", "null"))
                .andExpect(status().isBadRequest());

        String orgAdminPath = "/api/organizations/" + organizationId + "/admins/" + organizationAdminId;
        grant(orgAdminPath, creator).andExpect(status().isCreated());
        var adminKey = new OrganizationAdmin.Key(UUID.fromString(organizationId), organizationAdminId);
        Instant assignedAt = organizationAdmins.findById(adminKey).orElseThrow().getAssignedAt();
        assertThat(assignedAt).isNotNull();
        grant(orgAdminPath, creator).andExpect(status().isNoContent());
        assertThat(organizationAdmins.findById(adminKey).orElseThrow().getAssignedAt()).isEqualTo(assignedAt);
        String gymId = createdId(postJson(gymsPath, organizationAdmin, gymRequest("Downtown")));
        String siblingGymId = createdId(postJson(gymsPath, creator, gymRequest("North")));
        String otherGymId = createdId(postJson("/api/organizations/" + otherOrganizationId + "/gyms",
                otherAdmin, gymRequest("West")));

        grant("/api/gyms/" + gymId + "/admins/" + employeeId, organizationAdmin).andExpect(status().isNotFound());
        String gymStaffPath = "/api/gyms/" + gymId + "/staff/" + employeeId;
        grant(gymStaffPath, otherAdmin).andExpect(status().isForbidden());
        grant(gymStaffPath, organizationAdmin).andExpect(status().isCreated());
        grant(gymStaffPath, organizationAdmin).andExpect(status().isNoContent());
        grant("/api/organizations/" + organizationId + "/admins/" + employeeId, employee)
                .andExpect(status().isForbidden());

        String wall = "{\"name\":\"Slab\",\"discipline\":\"BOULDER\"}";
        String gymWallsPath = "/api/gyms/" + gymId + "/walls";
        String wallId = createdId(postJson(gymWallsPath, organizationAdmin, wall));
        String siblingWallId = createdId(postJson("/api/gyms/" + siblingGymId + "/walls", organizationAdmin, wall));
        postJson(gymWallsPath, employee, wall).andExpect(status().isForbidden());
        postJson("/api/gyms/" + otherGymId + "/walls", organizationAdmin, wall).andExpect(status().isForbidden());

        String publicGradeId = gradeId("V4").toString();
        String routeRequest = """
                {"gradeId":"%s","colour":"Blue","setterId":"%s","holdColour":"Blue"}
                """.formatted(publicGradeId, organizationAdminId);
        String routePath = gymWallsPath + "/" + wallId + "/routes";
        postJson(routePath, organizationAdmin, routeRequest.replace('"' + publicGradeId + '"', "null"))
                .andExpect(status().isBadRequest());
        String routeId = createdId(postJson(routePath, organizationAdmin, routeRequest)
                .andExpect(jsonPath("$.wallId").value(wallId))
                .andExpect(jsonPath("$.gradeId").value(publicGradeId))
                .andExpect(jsonPath("$.grade").value("V4")));
        Route route = routes.findById(UUID.fromString(routeId)).orElseThrow();
        assertThat(route.getGradeId()).isEqualTo(UUID.fromString(publicGradeId));
        assertThat(route.getSetterId()).isEqualTo(organizationAdminId);
        postJson(gymWallsPath + "/" + siblingWallId + "/routes", organizationAdmin, routeRequest)
                .andExpect(status().isNotFound());
        postJson(routePath, organizationAdmin, routeRequest.replace(publicGradeId, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());

        String systemId = createdId(postJson(gradingPath(gymId), employee, "{\"name\":\"Gym Circuit\"}"));
        String privateGradeId = createdId(postJson(gradingPath(gymId) + "/" + systemId + "/grades", employee,
                "{\"label\":\"Red Circuit\",\"sortOrder\":1}"));
        String privateRoute = routeRequest.replace(publicGradeId, privateGradeId);
        postJson(routePath, organizationAdmin, privateRoute).andExpect(status().isCreated())
                .andExpect(jsonPath("$.grade").value("Red Circuit"));
        postJson("/api/gyms/" + siblingGymId + "/walls/" + siblingWallId + "/routes", organizationAdmin, privateRoute)
                .andExpect(status().isNotFound());

        String sharedSystemId = createdId(postJson(gradingPath(gymId) + "/shared", employee,
                "{\"name\":\"Shared Circuit\"}"));
        String sharedGradeId = createdId(postJson(gradingPath(gymId) + "/" + sharedSystemId + "/grades", employee,
                "{\"label\":\"Shared Red\",\"sortOrder\":1}"));
        String sharedRoute = routeRequest.replace(publicGradeId, sharedGradeId);
        postJson("/api/gyms/" + siblingGymId + "/walls/" + siblingWallId + "/routes", organizationAdmin, sharedRoute)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.grade").value("Shared Red"));
        String otherWallId = createdId(postJson("/api/gyms/" + otherGymId + "/walls", otherAdmin, wall));
        postJson("/api/gyms/" + otherGymId + "/walls/" + otherWallId + "/routes", otherAdmin, sharedRoute)
                .andExpect(status().isNotFound());
    }

    @Test
    void scopesGradingSystemsToThePublicOrganizationOrGym() throws Exception {
        String admin = token();
        String otherAdmin = token();
        UUID staffId = UUID.randomUUID();
        String staff = token(staffId);
        String org = createdId(postJson("/api/organizations", admin, "{\"name\":\"Grading Co\"}"));
        String otherOrg = createdId(postJson("/api/organizations", otherAdmin, "{\"name\":\"Other Grading Co\"}"));
        String gymsPath = "/api/organizations/" + org + "/gyms";
        String gym = createdId(postJson(gymsPath, admin, gymRequest("Downtown")));
        String sibling = createdId(postJson(gymsPath, admin, gymRequest("North")));
        String otherGym = createdId(postJson("/api/organizations/" + otherOrg + "/gyms", otherAdmin, gymRequest("West")));
        String path = gradingPath(gym);
        grant("/api/gyms/" + gym + "/staff/" + staffId, admin).andExpect(status().isCreated());
        grant("/api/gyms/" + otherGym + "/staff/" + staffId, otherAdmin).andExpect(status().isCreated());

        String body = mvc.perform(get(path).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fontainebleau"))
                .andExpect(jsonPath("$[0].organizationId").value(nullValue()))
                .andExpect(jsonPath("$[1].name").value("V-scale"))
                .andExpect(jsonPath("$[1].grades[10].label").value("V9"))
                .andExpect(jsonPath("$[1].grades[11].label").value("V10"))
                .andReturn().getResponse().getContentAsString();
        String publicId = json.readTree(body).get(0).get("id").asString();

        String request = "{\"name\":\"Custom\"}";
        postJson(path, admin, request).andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", "Bearer " + otherAdmin)).andExpect(status().isForbidden());
        String systemId = createdId(postJson(path, staff, request)
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.organizationId").value(org))
                .andExpect(jsonPath("$.gymId").value(gym)));
        String grade = "{\"label\":\"Red\",\"sortOrder\":2}";
        postJson(path + "/" + systemId + "/grades", admin, grade).andExpect(status().isForbidden());
        postJson(path + "/" + publicId + "/grades", staff, grade).andExpect(status().isNotFound());
        postJson(gradingPath(sibling) + "/" + systemId + "/grades", staff, grade).andExpect(status().isForbidden());
        grant("/api/gyms/" + sibling + "/staff/" + staffId, admin).andExpect(status().isCreated());
        postJson(gradingPath(sibling) + "/" + systemId + "/grades", staff, grade).andExpect(status().isNotFound());
        postJson(path + "/" + systemId + "/grades", staff, grade).andExpect(status().isCreated());
        postJson(path + "/" + systemId + "/grades", staff, grade).andExpect(status().isConflict());
        postJson(path + "/" + systemId + "/grades", staff, "{\"label\":\"Blue\",\"sortOrder\":1}")
                .andExpect(status().isCreated());

        mvc.perform(get(path).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(systemId))
                .andExpect(jsonPath("$[0].grades[0].label").value("Blue"))
                .andExpect(jsonPath("$[0].grades[1].label").value("Red"));
        mvc.perform(get(gradingPath(sibling)).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(gradingPath(otherGym)).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        String selectedPath = "/api/gyms/" + gym + "/grading-system";
        mvc.perform(get(selectedPath).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gradingSystemId").value(nullValue()));
        selectGradingSystem(gym, admin, publicId).andExpect(status().isForbidden());
        selectGradingSystem(gym, staff, publicId).andExpect(status().isOk())
                .andExpect(jsonPath("$.gradingSystemId").value(publicId));
        selectGradingSystem(sibling, staff, systemId).andExpect(status().isNotFound());
        selectGradingSystem(otherGym, staff, systemId).andExpect(status().isNotFound());
        selectGradingSystem(gym, staff, systemId).andExpect(status().isOk());
        mvc.perform(get(selectedPath).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gradingSystemId").value(systemId));

        postJson(path + "/shared", admin, "{\"name\":\"Shared\"}").andExpect(status().isForbidden());
        String sharedSystemId = createdId(postJson(path + "/shared", staff, "{\"name\":\"Shared\"}")
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.organizationId").value(org))
                .andExpect(jsonPath("$.gymId").value(nullValue())));
        String siblingPath = gradingPath(sibling);
        postJson(siblingPath + "/" + sharedSystemId + "/grades", staff, grade).andExpect(status().isCreated());
        postJson(gradingPath(otherGym) + "/" + sharedSystemId + "/grades", staff, grade)
                .andExpect(status().isNotFound());
        selectGradingSystem(sibling, staff, sharedSystemId).andExpect(status().isOk());
        selectGradingSystem(otherGym, staff, sharedSystemId).andExpect(status().isNotFound());
        mvc.perform(get(siblingPath).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].id").value(sharedSystemId))
                .andExpect(jsonPath("$[1].grades[0].label").value("Red"));
        mvc.perform(get(gradingPath(otherGym)).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    private ResultActions selectGradingSystem(String gymId, String token, String systemId) throws Exception {
        return mvc.perform(put("/api/gyms/{gymId}/grading-system", gymId).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"gradingSystemId\":\"" + systemId + "\"}"));
    }

    private UUID gradeId(String label) {
        return grades.findAll().stream().filter(grade -> grade.getLabel().equals(label))
                .map(Grade::getId).findFirst().orElseThrow();
    }

    private static String gradingPath(String gymId) {
        return "/api/gyms/" + gymId + "/grading-systems";
    }

    private static String gymRequest(String name) {
        return """
                {"name":"%s","address":"100 Main St","city":"Calgary","postalCode":"T2P 1J9",
                 "country":"CA","latitude":51.05,"longitude":-114.07,"timezone":"America/Edmonton"}
                """.formatted(name);
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

    private String token() {
        return token(UUID.randomUUID());
    }

    private String token(UUID userId) {
        return token(AUTH_ISSUER, Instant.now(), Base64.getDecoder().decode(signingSecret), userId);
    }

    private String token(String issuer, Instant issuedAt, byte[] key) {
        return token(issuer, issuedAt, key, UUID.randomUUID());
    }

    private String token(String issuer, Instant issuedAt, byte[] key, UUID userId) {
        // Match the auth service's access-token claims and signing algorithm.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900))
                .claim("preferred_username", "climber")
                .build();
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
