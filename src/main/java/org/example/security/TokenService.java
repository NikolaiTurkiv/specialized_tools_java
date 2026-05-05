package org.example.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.example.exception.ApiException;

public class TokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long ttlSeconds;

    public TokenService(String secret, long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public String generate(long userId, String username, String role) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        TokenPayload payload = new TokenPayload(userId, username, role, expiresAt);

        try {
            String encodedPayload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = sign(encodedPayload);
            return encodedPayload + "." + signature;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to generate token", e);
        }
    }

    public TokenPayload verify(String token) {
        if (token == null || !token.contains(".")) {
            throw new ApiException(401, "Missing or invalid token");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new ApiException(401, "Invalid token format");
        }

        String payloadPart = parts[0];
        String expectedSignature = sign(payloadPart);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(401, "Token signature is invalid");
        }

        try {
            TokenPayload payload = objectMapper.readValue(DECODER.decode(payloadPart), TokenPayload.class);
            if (payload.expiresAtEpochSeconds() < Instant.now().getEpochSecond()) {
                throw new ApiException(401, "Token has expired");
            }
            return payload;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(401, "Failed to parse token");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }
}
