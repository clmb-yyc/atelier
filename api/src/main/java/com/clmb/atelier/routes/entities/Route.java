package com.clmb.atelier.routes.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wall_id", nullable = false)
    private UUID wallId;

    private String name;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "set_at", nullable = false)
    private Instant setAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String colour;

    @Column(name = "setter_id", nullable = false)
    private UUID setterId;

    @Column(name = "hold_colour", nullable = false)
    private String holdColour;

    private String style;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "removal_reason", columnDefinition = "text")
    private String removalReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Route() {}

    public Route(UUID wallId, String name, UUID gradeId, Instant setAt, String colour, UUID setterId,
                 String holdColour) {
        this.wallId = wallId;
        this.name = name;
        this.gradeId = gradeId;
        this.setAt = setAt;
        this.colour = colour;
        this.setterId = setterId;
        this.holdColour = holdColour;
        this.createdAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWallId() {
        return wallId;
    }

    public String getName() {
        return name;
    }

    public UUID getGradeId() {
        return gradeId;
    }

    public Instant getSetAt() {
        return setAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void remove(Instant at) {
        removedAt = at;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public UUID getSetterId() {
        return setterId;
    }

    public void setSetterId(UUID setterId) {
        this.setterId = setterId;
    }

    public String getHoldColour() {
        return holdColour;
    }

    public void setHoldColour(String holdColour) {
        this.holdColour = holdColour;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }
}
