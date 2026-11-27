package com.clmb.atelier.routes.dtos;

import java.util.UUID;

public record RouteRequest(String name, UUID gradeId, String colour, UUID setterId, String holdColour,
                           Integer difficultyRating) {}
