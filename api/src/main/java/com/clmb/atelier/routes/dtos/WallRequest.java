package com.clmb.atelier.routes.dtos;

import com.clmb.atelier.walls.entities.Wall;

public record WallRequest(String name, Wall.Discipline discipline) {}
