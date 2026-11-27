package com.clmb.atelier.gyms;

import com.clmb.atelier.gyms.entities.Gym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GymRepository extends JpaRepository<Gym, UUID> {}
