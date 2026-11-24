package com.clmb.atelier.auth;

import com.clmb.atelier.users.User;

import java.util.UUID;

public record RegisteredUser(UUID id, String username, User.Role role) {}
