package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.dto.GenerateOtpRequest;
import org.example.service.AuthService;
import org.example.service.AuthenticatedUser;
import org.example.service.OtpService;

public class GenerateOtpHandler extends BaseAuthenticatedHandler {
    private final OtpService otpService;

    public GenerateOtpHandler(AuthService authService, OtpService otpService) {
        super(authService);
        this.otpService = otpService;
    }

    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        AuthenticatedUser user = requireUser(exchange);
        GenerateOtpRequest request = readJson(exchange, GenerateOtpRequest.class);
        return new ApiResponse(201, otpService.generate(user.id(), user.username(), request));
    }
}
