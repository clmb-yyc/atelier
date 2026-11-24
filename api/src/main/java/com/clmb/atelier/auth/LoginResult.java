package com.clmb.atelier.auth;

public record LoginResult(String token, long expiresIn, String refreshToken, long refreshExpiresIn) {}
