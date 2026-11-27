package com.clmb.atelier.routes.dtos;

import java.time.Instant;
import java.util.UUID;

public record RouteResponse(UUID id, UUID wallId, String name, UUID gradeId, String grade, Instant setAt) {}
