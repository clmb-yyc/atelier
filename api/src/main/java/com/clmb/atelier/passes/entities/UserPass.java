package com.clmb.atelier.passes.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_passes")
public class UserPass {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pass_id", nullable = false)
    private UUID passId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected UserPass() {}

    public UserPass(UUID passId, UUID accountId, Instant startsAt, Instant expiresAt) {
        this.passId = passId;
        this.accountId = accountId;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPassId() {
        return passId;
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
