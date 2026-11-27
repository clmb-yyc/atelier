package com.clmb.atelier;

import com.clmb.atelier.access.AccessPlanRepository;
import com.clmb.atelier.access.MemberPassRepository;
import com.clmb.atelier.access.entities.AccessPlan;
import com.clmb.atelier.access.entities.MemberPass;
import com.clmb.atelier.checkins.CheckInRepository;
import com.clmb.atelier.checkins.entities.CheckIn;
import com.clmb.atelier.gyms.GymRepository;
import com.clmb.atelier.gyms.GymStaffRepository;
import com.clmb.atelier.gyms.entities.Gym;
import com.clmb.atelier.gyms.entities.GymStaff;
import com.clmb.atelier.organizations.OrganizationAdminRepository;
import com.clmb.atelier.organizations.OrganizationRepository;
import com.clmb.atelier.organizations.entities.Organization;
import com.clmb.atelier.organizations.entities.OrganizationAdmin;
import com.clmb.atelier.routes.RouteRepository;
import com.clmb.atelier.routes.entities.Route;
import com.clmb.atelier.walls.WallRepository;
import com.clmb.atelier.walls.entities.Wall;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenceTests {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired OrganizationRepository organizations;
    @Autowired OrganizationAdminRepository admins;
    @Autowired GymRepository gyms;
    @Autowired GymStaffRepository staff;
    @Autowired WallRepository walls;
    @Autowired RouteRepository routes;
    @Autowired AccessPlanRepository plans;
    @Autowired MemberPassRepository passes;
    @Autowired CheckInRepository checkIns;
    @Autowired EntityManager entityManager;

    @Test
    void archivingPreservesRoutesPassesAndCheckInHistory() {
        Organization organization = organizations.save(new Organization("Climbing Co"));
        Gym gym = gyms.save(new Gym(organization.getId(), "Downtown"));
        UUID memberId = UUID.randomUUID();
        var admin = admins.save(new OrganizationAdmin(organization.getId(), UUID.randomUUID()));
        var employee = staff.save(new GymStaff(gym.getId(), UUID.randomUUID()));
        Wall wall = walls.save(new Wall(gym.getId(), "Slab", Wall.Discipline.BOULDER));
        Route route = routes.save(new Route(wall.getId(), null, "V4", NOW));
        AccessPlan plan = plans.save(new AccessPlan(organization.getId(), gym.getId(), "10 visits",
                AccessPlan.PlanType.VISIT_PASS, null, 10));
        MemberPass pass = passes.save(new MemberPass(plan.getId(), memberId, NOW, NOW.plusSeconds(3600)));
        checkIns.save(new CheckIn(gym.getId(), pass.getId(), NOW));

        wall.archive(NOW);
        plan.archive(NOW);
        entityManager.flush();
        entityManager.clear();

        assertThat(admins.existsById(admin.getId())).isTrue();
        assertThat(staff.existsById(employee.getId())).isTrue();
        assertThat(routes.findById(route.getId()).orElseThrow().getWallId()).isEqualTo(wall.getId());
        assertThat(walls.findById(wall.getId()).orElseThrow().getArchivedAt()).isEqualTo(NOW);
        MemberPass stored = passes.findById(pass.getId()).orElseThrow();
        assertThat(stored.getAccountId()).isEqualTo(memberId);
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        AccessPlan storedPlan = plans.findById(stored.getAccessPlanId()).orElseThrow();
        assertThat(storedPlan.getArchivedAt()).isEqualTo(NOW);
        assertThat(storedPlan.getVisitLimit()).isEqualTo(10);
        assertThat(checkIns.countByMemberPassId(pass.getId())).isEqualTo(1);
    }

    @Test
    void rejectsAnAccessPlanForAnotherOrganizationsGym() {
        Organization first = organizations.save(new Organization("Climbing Co"));
        Organization second = organizations.save(new Organization("Another Gym Co"));
        Gym gym = gyms.save(new Gym(second.getId(), "Downtown"));

        assertThatThrownBy(() -> plans.saveAndFlush(new AccessPlan(first.getId(), gym.getId(), "10 visits",
                AccessPlan.PlanType.VISIT_PASS, null, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
