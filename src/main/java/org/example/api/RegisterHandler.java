package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.dto.RegisterRequest;
import org.example.dto.UserResponse;
import org.example.model.User;
import org.example.service.AuthService;

public class RegisterHandler extends JsonHandler {
    private final AuthService authService;

    public RegisterHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        RegisterRequest request = readJson(exchange, RegisterRequest.class);
        User user = authService.register(request);
        UserResponse response = new UserResponse(
                user.id(),
                user.username(),
                user.role().name(),
                user.createdAt()
        );
        return new ApiResponse(201, response);
    }
}
