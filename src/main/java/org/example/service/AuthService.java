package org.example.service;

import java.util.Locale;
import org.example.dao.UserDao;
import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.exception.ApiException;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.security.PasswordHasher;
import org.example.security.TokenPayload;
import org.example.security.TokenService;

public class AuthService {
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthService(UserDao userDao, PasswordHasher passwordHasher, TokenService tokenService) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public User register(RegisterRequest request) {
        validateCredentials(request.username(), request.password());
        UserRole role = parseRole(request.role());

        if (userDao.findByUsername(request.username().trim()).isPresent()) {
            throw new ApiException(409, "User with this username already exists");
        }

        if (role == UserRole.ADMIN && userDao.adminExists()) {
            throw new ApiException(409, "Administrator already exists");
        }

        String passwordHash = passwordHasher.hash(request.password());
        return userDao.create(request.username().trim(), passwordHash, role);
    }

    public AuthResponse login(LoginRequest request) {
        validateCredentials(request.username(), request.password());

        User user = userDao.findByUsername(request.username().trim())
                .orElseThrow(() -> new ApiException(401, "Invalid username or password"));

        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new ApiException(401, "Invalid username or password");
        }

        String token = tokenService.generate(user.id(), user.username(), user.role().name());
        TokenPayload payload = tokenService.verify(token);

        return new AuthResponse(
                token,
                payload.expiresAtEpochSeconds(),
                user.username(),
                user.role().name()
        );
    }

    public AuthenticatedUser requireAuthenticatedUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(401, "Authorization header must contain Bearer token");
        }

        String token = authorizationHeader.substring("Bearer ".length());
        TokenPayload payload = tokenService.verify(token);
        User user = userDao.findById(payload.userId())
                .orElseThrow(() -> new ApiException(401, "User from token no longer exists"));

        return new AuthenticatedUser(user.id(), user.username(), user.role());
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new ApiException(400, "Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new ApiException(400, "Password is required");
        }
        if (username.trim().length() < 3) {
            throw new ApiException(400, "Username must contain at least 3 characters");
        }
        if (password.length() < 6) {
            throw new ApiException(400, "Password must contain at least 6 characters");
        }
    }

    private UserRole parseRole(String rawRole) {
        try {
            return UserRole.fromString(rawRole == null ? UserRole.USER.name() : rawRole);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "Role must be ADMIN or USER");
        }
    }
}
