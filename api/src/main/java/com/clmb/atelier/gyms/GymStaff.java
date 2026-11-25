package com.clmb.atelier.gyms;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "gym_staff")
public class GymStaff {
    @Embeddable
    public record Key(
            @Column(name = "gym_id") UUID gymId,
            @Column(name = "account_id") UUID accountId
    ) {}

    @EmbeddedId
    private Key id;

    protected GymStaff() {}

    public GymStaff(UUID gymId, UUID accountId) {
        id = new Key(gymId, accountId);
    }

    public Key getId() {
        return id;
    }
}
