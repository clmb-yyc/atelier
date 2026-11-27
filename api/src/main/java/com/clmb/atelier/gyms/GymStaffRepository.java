package com.clmb.atelier.gyms;

import com.clmb.atelier.gyms.entities.GymStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface GymStaffRepository extends JpaRepository<GymStaff, GymStaff.Key> {
    @Modifying
    @Query(value = """
            INSERT INTO gym_staff (gym_id, account_id)
            VALUES (:gymId, :accountId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int grant(@Param("gymId") UUID gymId, @Param("accountId") UUID accountId);
}
