package com.clmb.atelier.gyms;

import com.clmb.atelier.gyms.entities.GymAdmin;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface GymAdminRepository extends Repository<GymAdmin, GymAdmin.Key> {
    boolean existsById(GymAdmin.Key id);

    @Modifying
    @Query(value = """
            INSERT INTO gym_admins (gym_id, user_id)
            VALUES (:gymId, :userId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int grant(@Param("gymId") UUID gymId, @Param("userId") UUID userId);
}
