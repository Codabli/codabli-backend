package com.codabli.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType) {
}
