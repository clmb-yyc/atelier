package com.clmb.atelier.grading;

import com.clmb.atelier.grading.entities.GradingSystem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradingSystemRepository extends JpaRepository<GradingSystem, UUID> {
    String PRIVATE_SCOPE = """
            (system.visibility = PRIVATE and system.organization.id = :organizationId
                and (system.gymId is null or system.gymId = :gymId))
            """;
    String AVAILABLE_SCOPE = "(system.visibility = PUBLIC or " + PRIVATE_SCOPE + ")";

    @Query("select system from GradingSystem system where " + AVAILABLE_SCOPE + " order by system.name, system.id")
    List<GradingSystem> findAvailableAtGym(UUID gymId, UUID organizationId);

    @Query("select system from GradingSystem system where system.id = :systemId and " + AVAILABLE_SCOPE)
    Optional<GradingSystem> findAvailableAtGym(UUID systemId, UUID gymId, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select system from GradingSystem system where system.id = :systemId and " + PRIVATE_SCOPE)
    Optional<GradingSystem> findEditableAtGym(UUID systemId, UUID gymId, UUID organizationId);
}
