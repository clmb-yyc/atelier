package com.clmb.atelier.grading;

import com.clmb.atelier.grading.entities.Grade;
import com.clmb.atelier.grading.entities.GradingSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    List<Grade> findByGradingSystemIdInOrderBySortOrderAscLabelAsc(List<UUID> gradingSystemIds);

    boolean existsByGradingSystemIdAndLabel(UUID gradingSystemId, String label);

    @Query("""
            select grade from Grade grade, GradingSystem system
            where grade.id = :gradeId and grade.gradingSystemId = system.id
                and (system.visibility = :publicVisibility or system.gymId = :gymId)
            """)
    Optional<Grade> findAvailableAtGym(UUID gradeId, UUID gymId, GradingSystem.Visibility publicVisibility);
}
