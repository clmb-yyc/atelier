package com.clmb.atelier;

import com.clmb.atelier.passes.PassRepository;
import com.clmb.atelier.passes.UserPassRepository;
import com.clmb.atelier.passes.entities.Pass;
import com.clmb.atelier.passes.entities.UserPass;
import com.clmb.atelier.checkins.CheckInRepository;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.gyms.entities.GymStaff;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.organizations.entities.Organization;
import com.clmb.atelier.organizations.entities.OrganizationAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PassFlowTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired OrganizationRepository organizations;
    @Autowired OrganizationAdminRepository admins;
    @Autowired GymRepository gyms;
    @Autowired GymStaffRepository gymStaff;
    @Autowired PassRepository plans;
    @Autowired UserPassRepository passes;
    @Autowired CheckInRepository checkIns;

    private final UUID admin = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();
    private final UUID staff = UUID.randomUUID();
    private final UUID otherStaff = UUID.randomUUID();
    private UUID organizationId;
    private UUID gymId;
    private UUID siblingGymId;
    private UUID otherGymId;
    private String passPath;

    @BeforeEach
    void createGyms() {
        organizationId = organizations.save(new Organization("Climbing Co")).getId();
        UUID otherOrganization = organizations.save(new Organization("Other Co")).getId();
        admins.save(new OrganizationAdmin(organizationId, admin));
        gymId = gyms.save(GymFixtures.gym(organizationId, "Downtown")).getId();
        siblingGymId = gyms.save(GymFixtures.gym(organizationId, "North")).getId();
        otherGymId = gyms.save(GymFixtures.gym(otherOrganization, "West")).getId();
        gymStaff.save(new GymStaff(otherGymId, otherStaff));
        passPath = "/api/organizations/" + organizationId + "/passes";
    }

    @Test
    void gymStaffCanUseMembersPassAtTheirGymAfterPlanArchival() throws Exception {
        postJson(passPath, member, Map.of("name", "Unauthorized")).andExpect(status().isForbidden());
        postJson(passPath, admin, Map.of("name", "Missing price", "passType", "DAY_PASS",
                "currency", "CAD", "isRenewable", false)).andExpect(status().isBadRequest());
        postJson(passPath, admin, Map.of("name", "Other gym", "gymId", otherGymId, "passType", "DAY_PASS",
                "price", 15, "currency", "CAD", "isRenewable", false))
                .andExpect(status().isNotFound());
        UUID plan = createdId(postJson(passPath, admin,
                Map.of("name", "One visit", "gymId", gymId, "passType", "DAY_PASS", "visitLimit", 1,
                        "price", 15, "currency", "CAD", "isRenewable", false)));
        String issuePath = passPath + "/" + plan + "/user-passes";
        postJson(issuePath, admin, Map.of("userId", member)).andExpect(status().isBadRequest());
        UUID pass = issueUserPass(plan);
        UserPass storedPass = passes.findById(pass).orElseThrow();
        assertThat(storedPass.getIssuedBy()).isEqualTo(admin);
        assertThat(storedPass.getPurchasedAt()).isNotNull();

        mvc.perform(get("/api/me/user-passes").with(jwt().jwt(builder -> builder.subject(member.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(pass.toString()));
        mvc.perform(get("/api/me/user-passes").with(jwt().jwt(builder -> builder.subject(admin.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        deleteAsAdmin(passPath + "/" + plan).andExpect(status().isNoContent());
        postJson(issuePath, admin, Map.of("userId", member, "expiresAt", Instant.now().plusSeconds(3600)))
                .andExpect(status().isConflict());
        checkIn(gymId, pass, member).andExpect(status().isForbidden());
        checkIn(gymId, pass, staff).andExpect(status().isForbidden());
        assignStaff(gymId, member, staff).andExpect(status().isForbidden());
        assignStaff(gymId, admin, staff).andExpect(status().isCreated());
        assertThat(gymStaff.findById(new GymStaff.Key(gymId, staff)).orElseThrow().getAssignedAt()).isNotNull();
        assignStaff(gymId, admin, staff).andExpect(status().isNoContent());
        assignStaff(gymId, staff, member).andExpect(status().isForbidden());
        assignStaff(siblingGymId, staff, member).andExpect(status().isForbidden());

        checkIn(siblingGymId, pass, staff).andExpect(status().isForbidden());
        checkIn(siblingGymId, pass, admin).andExpect(status().isForbidden());
        checkIn(otherGymId, pass, otherStaff).andExpect(status().isNotFound());
        UUID checkInId = createdId(checkIn(gymId, pass, staff)
                .andExpect(jsonPath("$.userPassId").value(pass.toString())));
        assertThat(checkIns.findById(checkInId).orElseThrow().getCheckedInBy()).isEqualTo(staff);
        checkIn(gymId, pass, staff).andExpect(status().isConflict());
        assertThat(checkIns.countByUserPassId(pass)).isEqualTo(1);
    }

    @Test
    void rejectsClosedIssuanceAndInactivePasses() throws Exception {
        Instant now = Instant.now();
        UUID closedPlan = plans.save(new Pass(organizationId, null, "Closed", Pass.PassType.MEMBERSHIP,
                now.minusSeconds(60), null, BigDecimal.TEN, "CAD", true)).getId();
        postJson(passPath + "/" + closedPlan + "/user-passes", admin,
                Map.of("userId", member, "expiresAt", now.plusSeconds(3600))).andExpect(status().isConflict());

        UUID expired = passes.save(new UserPass(closedPlan, member, now.minusSeconds(120), now.minusSeconds(60),
                now.minusSeconds(120), admin)).getId();
        UUID future = passes.save(new UserPass(closedPlan, member, now.plusSeconds(60), now.plusSeconds(120),
                now, admin)).getId();
        checkIn(gymId, expired, admin).andExpect(status().isConflict());
        checkIn(gymId, future, admin).andExpect(status().isConflict());

        UUID pass = passes.save(new UserPass(closedPlan, member, now.minusSeconds(60), now.plusSeconds(3600),
                now.minusSeconds(60), admin)).getId();
        checkIn(siblingGymId, pass, admin).andExpect(status().isCreated());
        deleteAsAdmin("/api/organizations/" + organizationId + "/user-passes/" + pass)
                .andExpect(status().isNoContent());
        checkIn(siblingGymId, pass, admin).andExpect(status().isConflict());
        assertThat(checkIns.countByUserPassId(pass)).isEqualTo(1);
    }

    @Test
    void concurrentCheckInsCanSpendTheLastVisitOnlyOnce() throws Exception {
        UUID plan = createdId(postJson(passPath, admin,
                Map.of("name", "Chain pass", "passType", "VISIT_PASS", "visitLimit", 1,
                        "price", 15, "currency", "CAD", "isRenewable", false)));
        UUID pass = issueUserPass(plan);
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(checkInWhenReady(start, gymId, pass));
            var second = executor.submit(checkInWhenReady(start, siblingGymId, pass));
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(checkIns.countByUserPassId(pass)).isEqualTo(1);
    }

    @Test
    void overlappingMembershipBlocksNewPassesAndAnotherMembership() throws Exception {
        UUID membership = createPass("Monthly", "MEMBERSHIP");
        UUID dayPass = createPass("Day pass", "DAY_PASS");
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Map<String, ?> issue = Map.of("userId", member, "expiresAt", expiresAt);
        UUID currentPass = createdId(postJson(passPath + "/" + membership + "/user-passes", admin, issue));

        for (UUID plan : List.of(membership, dayPass)) {
            postJson(passPath + "/" + plan + "/user-passes", admin, issue)
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EXISTING_MEMBERSHIP"))
                    .andExpect(jsonPath("$.conflictingUserPassIds[0]").value(currentPass.toString()));
        }

        postJson(passPath + "/" + dayPass + "/user-passes", admin, Map.of("userId", member,
                        "startsAt", expiresAt.plusSeconds(1), "expiresAt", expiresAt.plusSeconds(3600)))
                .andExpect(status().isCreated());
    }

    @Test
    void unusedDayPassNeedsReviewBeforeIssuingMembership() throws Exception {
        UUID dayPass = createPass("Day pass", "DAY_PASS");
        UUID membership = createPass("Monthly", "MEMBERSHIP");
        UUID currentPass = issueUserPass(dayPass);
        assignStaff(gymId, admin, staff).andExpect(status().isCreated());
        String issueAtGym = "/api/gyms/" + gymId + "/passes/" + membership + "/user-passes";
        Map<String, ?> request = Map.of("userId", member, "expiresAt", Instant.now().plusSeconds(3600));

        postJson(issueAtGym, staff, request)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EXISTING_PASSES_REVIEW"))
                .andExpect(jsonPath("$.conflictingUserPassIds[0]").value(currentPass.toString()));
        postJson(issueAtGym, otherStaff, request).andExpect(status().isForbidden());
        assertThat(passes.findAllByUserIdOrderByExpiresAtDesc(member)).hasSize(1);
    }

    @Test
    void concurrentMembershipRequestsForDifferentPlansCannotBothSucceed() throws Exception {
        UUID monthly = createPass("Monthly", "MEMBERSHIP");
        UUID yearly = createPass("Yearly", "MEMBERSHIP");
        Map<String, ?> request = Map.of("userId", member, "expiresAt", Instant.now().plusSeconds(3600));
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(issueWhenReady(start, monthly, request));
            var second = executor.submit(issueWhenReady(start, yearly, request));
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(passes.findAllByUserIdOrderByExpiresAtDesc(member)).hasSize(1);
    }

    private Callable<Integer> issueWhenReady(CyclicBarrier start, UUID plan, Map<String, ?> request) {
        return () -> {
            start.await(10, TimeUnit.SECONDS);
            return postJson(passPath + "/" + plan + "/user-passes", admin, request).andReturn().getResponse().getStatus();
        };
    }

    private UUID createPass(String name, String passType) throws Exception {
        return createdId(postJson(passPath, admin, Map.of("name", name, "passType", passType,
                "price", 15, "currency", "CAD", "isRenewable", false)));
    }

    private Callable<Integer> checkInWhenReady(CyclicBarrier start, UUID targetGym, UUID pass) {
        return () -> {
            start.await(10, TimeUnit.SECONDS);
            return checkIn(targetGym, pass, admin).andReturn().getResponse().getStatus();
        };
    }

    private UUID issueUserPass(UUID plan) throws Exception {
        return createdId(postJson(passPath + "/" + plan + "/user-passes", admin,
                Map.of("userId", member, "expiresAt", Instant.now().plusSeconds(3600))));
    }

    private ResultActions checkIn(UUID targetGym, UUID pass, UUID actor) throws Exception {
        return postJson("/api/gyms/" + targetGym + "/check-ins", actor, Map.of("userPassId", pass));
    }

    private ResultActions assignStaff(UUID targetGym, UUID actor, UUID account) throws Exception {
        return mvc.perform(put("/api/gyms/" + targetGym + "/staff/" + account)
                .with(jwt().jwt(builder -> builder.subject(actor.toString()))));
    }

    private ResultActions postJson(String path, UUID actor, Map<String, ?> body) throws Exception {
        return mvc.perform(post(path).with(jwt().jwt(builder -> builder.subject(actor.toString())))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions deleteAsAdmin(String path) throws Exception {
        return mvc.perform(delete(path).with(jwt().jwt(builder -> builder.subject(admin.toString()))));
    }

    private UUID createdId(ResultActions response) throws Exception {
        String body = response.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }
}
