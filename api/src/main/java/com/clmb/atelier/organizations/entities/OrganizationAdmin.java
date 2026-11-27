package com.clmb.atelier.organizations.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    protected OrganizationAdmin() {}

    public OrganizationAdmin(UUID organizationId, UUID userId) {
        id = new Key(organizationId, userId);
    }

    public Key getId() {
        return id;
    }
}
