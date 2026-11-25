package com.clmb.atelier.access;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberPassRepository extends JpaRepository<MemberPass, UUID> {
    List<MemberPass> findAllByAccountIdOrderByExpiresAtDesc(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pass from MemberPass pass where pass.id = :id and pass.accountId = :accountId")
    Optional<MemberPass> findOwnedForUpdate(@Param("id") UUID id, @Param("accountId") UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pass from MemberPass pass
            where pass.id = :id and exists (
                select plan.id from AccessPlan plan
                where plan.id = pass.accessPlanId and plan.organizationId = :organizationId
            )
            """)
    Optional<MemberPass> findInOrganizationForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
