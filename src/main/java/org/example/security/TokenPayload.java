package org.example.security;

public record TokenPayload(
        long userId,
        String username,
        String role,
        long expiresAtEpochSeconds
) {
}
