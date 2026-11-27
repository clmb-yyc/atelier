package com.clmb.atelier.routes.dtos;

import com.clmb.atelier.walls.entities.Wall;

import java.time.Instant;
import java.util.UUID;

public record RouteListItem(UUID id, UUID wallId, String wallName, Wall.Discipline discipline,
                            String name, UUID gradeId, String grade, Instant setAt) {}
