package com.clmb.atelier.passes.dtos;

import java.time.Instant;
import java.util.UUID;

public record IssueUserPassRequest(UUID userId, Instant startsAt, Instant expiresAt) {}
