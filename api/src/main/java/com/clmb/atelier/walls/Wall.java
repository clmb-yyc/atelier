package com.clmb.atelier.walls;

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

    protected Wall() {}

    public Wall(UUID gymId, String name, Discipline discipline) {
        this.gymId = gymId;
        this.name = name;
        this.discipline = discipline;
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
}
