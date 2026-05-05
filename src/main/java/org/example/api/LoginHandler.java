package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.service.AuthService;

public class LoginHandler extends JsonHandler {
    private final AuthService authService;

    public LoginHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        LoginRequest request = readJson(exchange, LoginRequest.class);
        AuthResponse response = authService.login(request);
        return new ApiResponse(200, response);
    }
}
