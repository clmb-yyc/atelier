package com.clmb.atelier.passes;

import com.clmb.atelier.passes.entities.Pass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassRepository extends JpaRepository<Pass, UUID> {
    List<Pass> findAllByOrganizationIdOrderByName(UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pass from Pass pass where pass.id = :id and pass.organizationId = :organizationId")
    Optional<Pass> findForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
