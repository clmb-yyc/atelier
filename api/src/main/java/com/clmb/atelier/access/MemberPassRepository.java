package com.clmb.atelier.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberPassRepository extends JpaRepository<MemberPass, UUID> {}
