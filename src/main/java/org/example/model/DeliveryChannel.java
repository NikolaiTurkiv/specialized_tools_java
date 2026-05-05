package org.example.model;

public enum DeliveryChannel {
    EMAIL,
    SMS,
    TELEGRAM,
    FILE;

    public static DeliveryChannel fromString(String value) {
        return DeliveryChannel.valueOf(value.trim().toUpperCase());
    }
}
