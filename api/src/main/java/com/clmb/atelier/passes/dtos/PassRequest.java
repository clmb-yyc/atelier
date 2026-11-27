package com.clmb.atelier.passes.dtos;

import com.clmb.atelier.passes.entities.Pass;

import java.time.Instant;
import java.util.UUID;

public record PassRequest(String name, UUID gymId, Pass.PassType passType, Instant validUntil,
                          Integer visitLimit) {}
