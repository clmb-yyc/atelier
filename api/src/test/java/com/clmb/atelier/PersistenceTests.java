package com.clmb.atelier;

import com.clmb.atelier.passes.PassRepository;
import com.clmb.atelier.passes.UserPassRepository;
import com.clmb.atelier.passes.entities.Pass;
import com.clmb.atelier.passes.entities.UserPass;
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

import java.math.BigDecimal;
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
    @Autowired PassRepository plans;
    @Autowired UserPassRepository passes;
    @Autowired CheckInRepository checkIns;
    @Autowired EntityManager entityManager;

    @Test
    void archivingPreservesRoutesPassesAndCheckInHistory() {
        Organization organization = organizations.save(new Organization("Climbing Co"));
        Gym gym = gyms.save(GymFixtures.gym(organization.getId(), "Downtown"));
        UUID memberId = UUID.randomUUID();
        var admin = admins.save(new OrganizationAdmin(organization.getId(), UUID.randomUUID()));
        var employee = staff.save(new GymStaff(gym.getId(), UUID.randomUUID()));
        Wall wall = walls.save(new Wall(gym.getId(), "Slab", Wall.Discipline.BOULDER));
        Route route = routes.save(new Route(wall.getId(), null, "V4", NOW, "Blue", employee.getId().userId(),
                "Blue", 4));
        Pass plan = plans.save(new Pass(organization.getId(), gym.getId(), "10 visits",
                Pass.PassType.VISIT_PASS, null, 10, BigDecimal.TEN, "CAD", false));
        UserPass pass = passes.save(new UserPass(plan.getId(), memberId, NOW, NOW.plusSeconds(3600),
                NOW, employee.getId().userId()));
        checkIns.save(new CheckIn(gym.getId(), pass.getId(), NOW, employee.getId().userId()));

        wall.archive(NOW);
        plan.archive(NOW);
        entityManager.flush();
        entityManager.clear();

        assertThat(admins.existsById(admin.getId())).isTrue();
        assertThat(staff.existsById(employee.getId())).isTrue();
        assertThat(routes.findById(route.getId()).orElseThrow().getWallId()).isEqualTo(wall.getId());
        assertThat(walls.findById(wall.getId()).orElseThrow().getArchivedAt()).isEqualTo(NOW);
        UserPass stored = passes.findById(pass.getId()).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(memberId);
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        Pass storedPlan = plans.findById(stored.getPassId()).orElseThrow();
        assertThat(storedPlan.getArchivedAt()).isEqualTo(NOW);
        assertThat(storedPlan.getVisitLimit()).isEqualTo(10);
        assertThat(checkIns.countByUserPassId(pass.getId())).isEqualTo(1);
    }

    @Test
    void rejectsAPassForAnotherOrganizationsGym() {
        Organization first = organizations.save(new Organization("Climbing Co"));
        Organization second = organizations.save(new Organization("Another Gym Co"));
        Gym gym = gyms.save(GymFixtures.gym(second.getId(), "Downtown"));

        assertThatThrownBy(() -> plans.saveAndFlush(new Pass(first.getId(), gym.getId(), "10 visits",
                Pass.PassType.VISIT_PASS, null, 10, BigDecimal.TEN, "CAD", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
