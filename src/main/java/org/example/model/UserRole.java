package org.example.model;

public enum UserRole {
    ADMIN,
    USER;

    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}
