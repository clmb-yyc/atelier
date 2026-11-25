package com.clmb.atelier.checkins;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "gym_id", nullable = false)
    private UUID gymId;

    @Column(name = "member_pass_id", nullable = false)
    private UUID memberPassId;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    protected CheckIn() {}

    public CheckIn(UUID gymId, UUID memberPassId, Instant checkedInAt) {
        this.gymId = gymId;
        this.memberPassId = memberPassId;
        this.checkedInAt = checkedInAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGymId() {
        return gymId;
    }

    public UUID getMemberPassId() {
        return memberPassId;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }
}
