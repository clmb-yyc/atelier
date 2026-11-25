package com.clmb.atelier.access;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessPlanRepository extends JpaRepository<AccessPlan, UUID> {
    List<AccessPlan> findAllByOrganizationIdOrderByName(UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from AccessPlan plan where plan.id = :id and plan.organizationId = :organizationId")
    Optional<AccessPlan> findForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
