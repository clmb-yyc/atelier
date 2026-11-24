package com.clmb.atelier.auth;

public record AccessToken(String accessToken, String tokenType, long expiresIn) {}
