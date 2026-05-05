package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.exception.ApiException;
import org.example.model.UserRole;
import org.example.service.AuthService;
import org.example.service.AuthenticatedUser;

public abstract class BaseAuthenticatedHandler extends JsonHandler {
    private final AuthService authService;

    protected BaseAuthenticatedHandler(AuthService authService) {
        this.authService = authService;
    }

    protected AuthenticatedUser requireUser(HttpExchange exchange) {
        return authService.requireAuthenticatedUser(authorizationHeader(exchange));
    }

    protected AuthenticatedUser requireAdmin(HttpExchange exchange) {
        AuthenticatedUser user = requireUser(exchange);
        if (user.role() != UserRole.ADMIN) {
            throw new ApiException(403, "Administrator role is required");
        }
        return user;
    }
}
