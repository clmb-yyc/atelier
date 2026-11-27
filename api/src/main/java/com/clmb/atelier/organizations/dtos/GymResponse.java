package com.clmb.atelier.organizations.dtos;

import java.util.UUID;

public record GymResponse(UUID id, UUID organizationId, String name) {}
