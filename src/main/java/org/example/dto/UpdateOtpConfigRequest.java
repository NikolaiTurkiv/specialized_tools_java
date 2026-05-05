package org.example.dto;

public record UpdateOtpConfigRequest(Integer codeLength, Integer ttlSeconds) {
}
