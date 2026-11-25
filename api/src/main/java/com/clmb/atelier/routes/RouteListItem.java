package com.clmb.atelier.routes;

import com.clmb.atelier.walls.Wall;

import java.time.Instant;
import java.util.UUID;

public record RouteListItem(UUID id, UUID wallId, String wallName, Wall.Discipline discipline,
                            String name, String grade, Instant setAt) {}
