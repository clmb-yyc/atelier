package com.clmb.atelier.walls;

import com.clmb.atelier.walls.entities.Wall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WallRepository extends JpaRepository<Wall, UUID> {
    Optional<Wall> findByIdAndGymIdAndArchivedAtIsNull(UUID id, UUID gymId);
}
