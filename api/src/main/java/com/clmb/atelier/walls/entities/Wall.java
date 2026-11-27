package com.clmb.atelier.walls.entities;

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
@Table(name = "walls")
public class Wall {
    public enum Discipline { BOULDER, ROPE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "gym_id", nullable = false)
    private UUID gymId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Discipline discipline;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "height_m")
    private Double heightM;

    @Column(name = "angle_degrees")
    private Double angleDegrees;

    private String location;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Wall() {}

    public Wall(UUID gymId, String name, Discipline discipline) {
        this.gymId = gymId;
        this.name = name;
        this.discipline = discipline;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGymId() {
        return gymId;
    }

    public String getName() {
        return name;
    }

    public Discipline getDiscipline() {
        return discipline;
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

    public Double getHeightM() {
        return heightM;
    }

    public void setHeightM(Double heightM) {
        this.heightM = heightM;
    }

    public Double getAngleDegrees() {
        return angleDegrees;
    }

    public void setAngleDegrees(Double angleDegrees) {
        this.angleDegrees = angleDegrees;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
