package com.clmb.atelier.organizations.dtos;

public record GymRequest(
        String name,
        String address,
        String city,
        String postalCode,
        String country,
        Double latitude,
        Double longitude,
        String timezone
) {}
