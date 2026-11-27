package com.clmb.atelier.checkins.entities;

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

    @Column(name = "user_pass_id", nullable = false)
    private UUID userPassId;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(name = "checked_in_by", nullable = false)
    private UUID checkedInBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    protected CheckIn() {}

    public CheckIn(UUID gymId, UUID userPassId, Instant checkedInAt, UUID checkedInBy) {
        this.gymId = gymId;
        this.userPassId = userPassId;
        this.checkedInAt = checkedInAt;
        this.checkedInBy = checkedInBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGymId() {
        return gymId;
    }

    public UUID getUserPassId() {
        return userPassId;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(Instant checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public UUID getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(UUID checkedInBy) {
        this.checkedInBy = checkedInBy;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public void setVoidedAt(Instant voidedAt) {
        this.voidedAt = voidedAt;
    }
}
