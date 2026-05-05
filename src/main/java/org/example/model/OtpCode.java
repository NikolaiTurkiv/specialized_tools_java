package org.example.model;

import java.time.Instant;

public record OtpCode(
        long id,
        long userId,
        String operationId,
        String code,
        OtpStatus status,
        DeliveryChannel deliveryChannel,
        String destination,
        Instant expiresAt,
        Instant createdAt,
        Instant usedAt
) {
}
