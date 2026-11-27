package com.clmb.atelier.grading;

import com.clmb.atelier.grading.entities.GradingSystem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradingSystemRepository extends JpaRepository<GradingSystem, UUID> {
    List<GradingSystem> findByVisibilityOrGymIdOrderByNameAscIdAsc(GradingSystem.Visibility visibility, UUID gymId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GradingSystem> findByIdAndGymId(UUID id, UUID gymId);
}
