package org.example.model;

import java.time.Instant;

public record OtpConfig(
        int id,
        int codeLength,
        int ttlSeconds,
        Instant updatedAt
) {
}
