package org.example.service;

import org.example.dao.OtpCodeDao;
import org.example.dao.OtpConfigDao;
import org.example.dto.GenerateOtpRequest;
import org.example.dto.MessageResponse;
import org.example.dto.OtpResponse;
import org.example.dto.ValidateOtpRequest;
import org.example.exception.ApiException;
import org.example.model.DeliveryChannel;
import org.example.model.OtpCode;
import org.example.model.OtpConfig;
import org.example.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Instant;

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpConfigDao otpConfigDao;
    private final OtpCodeDao otpCodeDao;
    private final NotificationDispatcher notificationDispatcher;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpConfigDao otpConfigDao,
            OtpCodeDao otpCodeDao,
            NotificationDispatcher notificationDispatcher
    ) {
        this.otpConfigDao = otpConfigDao;
        this.otpCodeDao = otpCodeDao;
        this.notificationDispatcher = notificationDispatcher;
    }

    public OtpResponse generate(long userId, String username, GenerateOtpRequest request) {
        if (request.operationId() == null || request.operationId().isBlank()) {
            throw new ApiException(400, "operationId is required");
        }
        if (request.channel() == null || request.channel().isBlank()) {
            throw new ApiException(400, "channel is required");
        }

        DeliveryChannel channel = parseChannel(request.channel());
        validateDestination(channel, request.destination());
        OtpConfig config = otpConfigDao.get();
        String code = generateNumericCode(config.codeLength());
        Instant expiresAt = Instant.now().plusSeconds(config.ttlSeconds());

        OtpCode otpCode = otpCodeDao.create(
                userId,
                request.operationId().trim(),
                code,
                channel,
                request.destination(),
                expiresAt
        );

        notificationDispatcher.send(channel, username, request.destination(), request.operationId().trim(), code);

        return new OtpResponse(
                otpCode.id(),
                otpCode.operationId(),
                otpCode.deliveryChannel().name(),
                otpCode.status().name(),
                otpCode.expiresAt(),
                channel == DeliveryChannel.FILE
                        ? "OTP code saved to project root file"
                        : "OTP code sent"
        );
    }

    public MessageResponse validate(long userId, ValidateOtpRequest request) {
        if (request.operationId() == null || request.operationId().isBlank()) {
            throw new ApiException(400, "operationId is required");
        }
        if (request.code() == null || request.code().isBlank()) {
            throw new ApiException(400, "code is required");
        }

        OtpCode otpCode = otpCodeDao.findActive(userId, request.operationId().trim(), request.code().trim())
                .orElseThrow(() -> new ApiException(404, "Active OTP code not found"));

        if (otpCode.expiresAt().isBefore(Instant.now())) {
            expireCodes();
            throw new ApiException(400, "OTP code has expired");
        }

        otpCodeDao.markUsed(otpCode.id());
        return new MessageResponse("OTP code is valid and marked as USED");
    }

    public void expireCodes() {
        int updated = otpCodeDao.expireDueCodes();
        if (updated > 0) {
            log.info("Expired {} OTP codes", updated);
        }
    }

    private DeliveryChannel parseChannel(String rawChannel) {
        try {
            return DeliveryChannel.fromString(rawChannel);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "channel must be EMAIL, SMS, TELEGRAM or FILE");
        }
    }

    private void validateDestination(DeliveryChannel channel, String destination) {
        if (channel == DeliveryChannel.FILE) {
            return;
        }

        if (destination == null || destination.isBlank()) {
            throw new ApiException(400, "destination is required for channel " + channel.name());
        }

        if (channel == DeliveryChannel.EMAIL && !isValidEmail(destination)) {
            throw new ApiException(400, "destination must be a valid email address");
        }

        if (channel == DeliveryChannel.SMS && !isValidPhoneNumber(destination)) {
            throw new ApiException(400, "destination must be a valid phone number");
        }
    }

    private boolean isValidEmail(String value) {
        int atIndex = value.indexOf('@');
        int dotIndex = value.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < value.length() - 1;
    }

    private boolean isValidPhoneNumber(String value) {
        return value.matches("^\\+?[0-9]{10,15}$");
    }

    private String generateNumericCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }
}
