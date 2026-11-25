package com.clmb.atelier.routes;

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

    @Column(nullable = false)
    private String grade;

    @Column(name = "set_at", nullable = false)
    private Instant setAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected Route() {}

    public Route(UUID wallId, String name, String grade, Instant setAt) {
        this.wallId = wallId;
        this.name = name;
        this.grade = grade;
        this.setAt = setAt;
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

    public String getGrade() {
        return grade;
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
}
