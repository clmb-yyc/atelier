package com.clmb.atelier.walls;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WallRepository extends JpaRepository<Wall, UUID> {}
