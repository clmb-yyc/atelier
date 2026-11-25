package com.clmb.atelier;

import com.clmb.atelier.access.AccessPlan;
import com.clmb.atelier.access.AccessPlanRepository;
import com.clmb.atelier.access.MemberPass;
import com.clmb.atelier.access.MemberPassRepository;
import com.clmb.atelier.checkins.CheckInRepository;
import com.clmb.atelier.gyms.Gym;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.organizations.Organization;
import com.clmb.atelier.organizations.OrganizationAdmin;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MembershipFlowTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired OrganizationRepository organizations;
    @Autowired OrganizationAdminRepository admins;
    @Autowired GymRepository gyms;
    @Autowired AccessPlanRepository plans;
    @Autowired MemberPassRepository passes;
    @Autowired CheckInRepository checkIns;

    private final UUID admin = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();
    private UUID organizationId;
    private UUID gymId;
    private UUID siblingGymId;
    private UUID otherGymId;
    private String planPath;

    @BeforeEach
    void createGyms() {
        organizationId = organizations.save(new Organization("Climbing Co")).getId();
        UUID otherOrganization = organizations.save(new Organization("Other Co")).getId();
        admins.save(new OrganizationAdmin(organizationId, admin));
        gymId = gyms.save(new Gym(organizationId, "Downtown")).getId();
        siblingGymId = gyms.save(new Gym(organizationId, "North")).getId();
        otherGymId = gyms.save(new Gym(otherOrganization, "West")).getId();
        planPath = "/api/organizations/" + organizationId + "/access-plans";
    }

    @Test
    void issuedPassSurvivesPlanArchivalAndOnlyWorksForItsOwnerAtTheRightGym() throws Exception {
        postJson(planPath, member, Map.of("name", "Unauthorized")).andExpect(status().isForbidden());
        postJson(planPath, admin, Map.of("name", "Other gym", "gymId", otherGymId))
                .andExpect(status().isNotFound());
        UUID plan = createdId(postJson(planPath, admin, Map.of("name", "One visit", "gymId", gymId, "visitLimit", 1)));
        String issuePath = planPath + "/" + plan + "/passes";
        postJson(issuePath, admin, Map.of("accountId", member)).andExpect(status().isBadRequest());
        UUID pass = issuePass(plan);

        mvc.perform(get("/api/me/member-passes").with(jwt().jwt(builder -> builder.subject(member.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(pass.toString()));
        mvc.perform(get("/api/me/member-passes").with(jwt().jwt(builder -> builder.subject(admin.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        deleteAsAdmin(planPath + "/" + plan).andExpect(status().isNoContent());
        postJson(issuePath, admin, Map.of("accountId", member, "expiresAt", Instant.now().plusSeconds(3600)))
                .andExpect(status().isConflict());
        checkIn(gymId, pass, admin).andExpect(status().isNotFound());
        checkIn(siblingGymId, pass, member).andExpect(status().isForbidden());
        checkIn(otherGymId, pass, member).andExpect(status().isForbidden());
        checkIn(gymId, pass, member).andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberPassId").value(pass.toString()));
        checkIn(gymId, pass, member).andExpect(status().isConflict());
        assertThat(checkIns.countByMemberPassId(pass)).isEqualTo(1);
    }

    @Test
    void rejectsClosedIssuanceAndInactivePasses() throws Exception {
        Instant now = Instant.now();
        UUID closedPlan = plans.save(new AccessPlan(organizationId, null, "Closed", now.minusSeconds(60), null)).getId();
        postJson(planPath + "/" + closedPlan + "/passes", admin,
                Map.of("accountId", member, "expiresAt", now.plusSeconds(3600))).andExpect(status().isConflict());

        UUID expired = passes.save(new MemberPass(closedPlan, member, now.minusSeconds(120), now.minusSeconds(60))).getId();
        UUID future = passes.save(new MemberPass(closedPlan, member, now.plusSeconds(60), now.plusSeconds(120))).getId();
        checkIn(gymId, expired, member).andExpect(status().isConflict());
        checkIn(gymId, future, member).andExpect(status().isConflict());

        UUID pass = passes.save(new MemberPass(closedPlan, member, now.minusSeconds(60), now.plusSeconds(3600))).getId();
        checkIn(siblingGymId, pass, member).andExpect(status().isCreated());
        deleteAsAdmin("/api/organizations/" + organizationId + "/member-passes/" + pass)
                .andExpect(status().isNoContent());
        checkIn(siblingGymId, pass, member).andExpect(status().isConflict());
        assertThat(checkIns.countByMemberPassId(pass)).isEqualTo(1);
    }

    @Test
    void concurrentCheckInsCanSpendTheLastVisitOnlyOnce() throws Exception {
        UUID plan = createdId(postJson(planPath, admin, Map.of("name", "Chain pass", "visitLimit", 1)));
        UUID pass = issuePass(plan);
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(checkInWhenReady(start, gymId, pass));
            var second = executor.submit(checkInWhenReady(start, siblingGymId, pass));
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(checkIns.countByMemberPassId(pass)).isEqualTo(1);
    }

    private Callable<Integer> checkInWhenReady(CyclicBarrier start, UUID targetGym, UUID pass) {
        return () -> {
            start.await(10, TimeUnit.SECONDS);
            return checkIn(targetGym, pass, member).andReturn().getResponse().getStatus();
        };
    }

    private UUID issuePass(UUID plan) throws Exception {
        return createdId(postJson(planPath + "/" + plan + "/passes", admin,
                Map.of("accountId", member, "expiresAt", Instant.now().plusSeconds(3600))));
    }

    private ResultActions checkIn(UUID targetGym, UUID pass, UUID actor) throws Exception {
        return postJson("/api/gyms/" + targetGym + "/check-ins", actor, Map.of("memberPassId", pass));
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
