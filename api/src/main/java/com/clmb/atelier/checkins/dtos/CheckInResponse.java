package com.clmb.atelier.checkins.dtos;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(UUID id, UUID gymId, UUID userPassId, Instant checkedInAt) {}
