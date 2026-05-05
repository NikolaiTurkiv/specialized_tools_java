package org.example.dto;

import java.time.Instant;

public record OtpConfigResponse(int codeLength, int ttlSeconds, Instant updatedAt) {
}
