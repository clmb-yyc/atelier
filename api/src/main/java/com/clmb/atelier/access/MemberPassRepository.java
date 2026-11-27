package com.clmb.atelier.access;

import com.clmb.atelier.access.entities.AccessPlan;
import com.clmb.atelier.access.entities.MemberPass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberPassRepository extends JpaRepository<MemberPass, UUID> {
    List<MemberPass> findAllByAccountIdOrderByExpiresAtDesc(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pass from MemberPass pass
            where pass.id = :id and exists (
                select plan.id from AccessPlan plan
                where plan.id = pass.accessPlanId and plan.organizationId = :organizationId
            )
            """)
    Optional<MemberPass> findInOrganizationForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    @Query("""
            select pass.id as id, plan.planType as planType
            from MemberPass pass, AccessPlan plan
            where pass.accessPlanId = plan.id and plan.organizationId = :organizationId
                and pass.accountId = :accountId and pass.revokedAt is null
                and pass.startsAt < :before and pass.expiresAt > :after
                and (plan.planType = :membershipType or plan.visitLimit is null or plan.visitLimit >
                    (select count(checkIn) from CheckIn checkIn where checkIn.memberPassId = pass.id))
            order by pass.id
            """)
    List<OverlappingPass> findUsableOverlaps(@Param("organizationId") UUID organizationId,
                                             @Param("accountId") UUID accountId,
                                             @Param("after") Instant after, @Param("before") Instant before,
                                             @Param("membershipType") AccessPlan.PlanType membershipType);

    interface OverlappingPass {
        UUID getId();

        AccessPlan.PlanType getPlanType();
    }
}
