package com.clmb.atelier.organizations.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_admins")
public class OrganizationAdmin {
    @Embeddable
    public record Key(
            @Column(name = "organization_id") UUID organizationId,
            @Column(name = "user_id") UUID userId
    ) {}

    @EmbeddedId
    private Key id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    protected OrganizationAdmin() {}

    public OrganizationAdmin(UUID organizationId, UUID userId) {
        id = new Key(organizationId, userId);
        assignedAt = Instant.now();
    }

    public Key getId() {
        return id;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }
}
