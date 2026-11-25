package com.clmb.atelier.checkins;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    long countByMemberPassId(UUID memberPassId);
}
