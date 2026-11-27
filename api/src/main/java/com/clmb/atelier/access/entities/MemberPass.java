package com.clmb.atelier.access.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_passes")
public class MemberPass {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "access_plan_id", nullable = false)
    private UUID accessPlanId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected MemberPass() {}

    public MemberPass(UUID accessPlanId, UUID accountId, Instant startsAt, Instant expiresAt) {
        this.accessPlanId = accessPlanId;
        this.accountId = accountId;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccessPlanId() {
        return accessPlanId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revoke(Instant at) {
        revokedAt = at;
    }
}
