package com.clmb.atelier.passes.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
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

    @Column(columnDefinition = "text")
    private String description;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "is_renewable", nullable = false)
    private Boolean isRenewable;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Pass() {}

    public Pass(UUID organizationId, UUID gymId, String name, PassType passType, Instant validUntil,
                Integer visitLimit, BigDecimal price, String currency, boolean isRenewable) {
        this.organizationId = organizationId;
        this.gymId = gymId;
        this.name = name;
        this.passType = passType;
        this.validUntil = validUntil;
        this.visitLimit = visitLimit;
        this.price = price;
        this.currency = currency;
        this.isRenewable = isRenewable;
        this.createdAt = Instant.now();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Boolean getRenewable() {
        return isRenewable;
    }

    public void setRenewable(Boolean isRenewable) {
        this.isRenewable = isRenewable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
