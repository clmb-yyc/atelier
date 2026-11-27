package com.clmb.atelier.passes.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passes")
public class Pass {
    public enum PassType { MEMBERSHIP, DAY_PASS, VISIT_PASS }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "gym_id", updatable = false)
    private UUID gymId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", length = 16, updatable = false)
    private PassType passType;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "visit_limit", updatable = false)
    private Integer visitLimit;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Pass() {}

    public Pass(UUID organizationId, UUID gymId, String name, PassType passType, Instant validUntil,
                      Integer visitLimit) {
        this.organizationId = organizationId;
        this.gymId = gymId;
        this.name = name;
        this.passType = passType;
        this.validUntil = validUntil;
        this.visitLimit = visitLimit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getGymId() {
        return gymId;
    }

    public String getName() {
        return name;
    }

    public PassType getPassType() {
        return passType;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public Integer getVisitLimit() {
        return visitLimit;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void archive(Instant at) {
        archivedAt = at;
    }
}
