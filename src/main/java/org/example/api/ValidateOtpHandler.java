package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.dto.ValidateOtpRequest;
import org.example.service.AuthService;
import org.example.service.AuthenticatedUser;
import org.example.service.OtpService;

public class ValidateOtpHandler extends BaseAuthenticatedHandler {
    private final OtpService otpService;

    public ValidateOtpHandler(AuthService authService, OtpService otpService) {
        super(authService);
        this.otpService = otpService;
    }

    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        AuthenticatedUser user = requireUser(exchange);
        ValidateOtpRequest request = readJson(exchange, ValidateOtpRequest.class);
        return new ApiResponse(200, otpService.validate(user.id(), request));
    }
}
