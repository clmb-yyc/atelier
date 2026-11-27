package com.clmb.atelier.routes.dtos;

import com.clmb.atelier.walls.entities.Wall;

import java.util.UUID;

public record WallResponse(UUID id, UUID gymId, String name, Wall.Discipline discipline) {}
