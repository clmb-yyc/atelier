package com.clmb.atelier.grading;

import com.clmb.atelier.grading.entities.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    List<Grade> findByGradingSystemIdInOrderBySortOrderAscLabelAsc(List<UUID> gradingSystemIds);

    boolean existsByGradingSystemIdAndLabel(UUID gradingSystemId, String label);
}
