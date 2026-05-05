package org.example.dto;

public record AuthResponse(
        String token,
        long expiresAtEpochSeconds,
        String username,
        String role
) {
}
