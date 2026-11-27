package com.clmb.atelier.gyms.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "gym_admins")
public class GymAdmin {
    @Embeddable
    public record Key(
            @Column(name = "gym_id") UUID gymId,
            @Column(name = "user_id") UUID userId
    ) {}

    @EmbeddedId
    private Key id;

    protected GymAdmin() {}
}
