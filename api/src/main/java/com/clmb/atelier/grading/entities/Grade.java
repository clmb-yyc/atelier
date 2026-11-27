package com.clmb.atelier.grading.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grades", uniqueConstraints = @UniqueConstraint(
        name = "grades_system_label_unique", columnNames = {"grading_system_id", "label"}))
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "grading_system_id", nullable = false)
    private UUID gradingSystemId;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Grade() {}

    public Grade(UUID gradingSystemId, String label, String description, int sortOrder) {
        this.gradingSystemId = gradingSystemId;
        this.label = label;
        this.description = description;
        this.sortOrder = sortOrder;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGradingSystemId() {
        return gradingSystemId;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
