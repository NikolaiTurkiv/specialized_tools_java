package org.example.dto;

import java.time.Instant;

public record OtpResponse(
        long otpId,
        String operationId,
        String channel,
        String status,
        Instant expiresAt,
        String message
) {
}
