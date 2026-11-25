package com.clmb.atelier.organizations;

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
            @Column(name = "account_id") UUID accountId
    ) {}

    @EmbeddedId
    private Key id;

    protected OrganizationAdmin() {}

    public OrganizationAdmin(UUID organizationId, UUID accountId) {
        id = new Key(organizationId, accountId);
    }

    public Key getId() {
        return id;
    }
}
