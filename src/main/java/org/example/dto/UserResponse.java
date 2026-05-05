package org.example.dto;

import java.time.Instant;

public record UserResponse(long id, String username, String role, Instant createdAt) {
}
