package com.clmb.atelier.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_plans")
public class AccessPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "gym_id", updatable = false)
    private UUID gymId;

    @Column(nullable = false)
    private String name;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "visit_limit", updatable = false)
    private Integer visitLimit;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected AccessPlan() {}

    public AccessPlan(UUID organizationId, UUID gymId, String name, Instant validUntil, Integer visitLimit) {
        this.organizationId = organizationId;
        this.gymId = gymId;
        this.name = name;
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
