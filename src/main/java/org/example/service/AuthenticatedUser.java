package org.example.service;

import org.example.model.UserRole;

public record AuthenticatedUser(long id, String username, UserRole role) {
}
