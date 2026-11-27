package com.clmb.atelier;

import com.clmb.atelier.gyms.entities.Gym;

import java.util.UUID;

final class GymFixtures {
    private GymFixtures() {}

    static Gym gym(UUID organizationId, String name) {
        return new Gym(organizationId, name, "100 Main St", "Calgary", "T2P 1J9", "CA",
                51.05, -114.07, "America/Edmonton");
    }
}
