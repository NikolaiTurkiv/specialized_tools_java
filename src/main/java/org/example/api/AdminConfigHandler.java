package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.dto.UpdateOtpConfigRequest;
import org.example.service.AdminService;
import org.example.service.AuthService;

public class AdminConfigHandler extends BaseAuthenticatedHandler {
    private final AdminService adminService;

    public AdminConfigHandler(AuthService authService, AdminService adminService) {
        super(authService);
        this.adminService = adminService;
    }

    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) throws Exception {
        requireAdmin(exchange);
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            return new ApiResponse(200, adminService.getConfig());
        }
        if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            UpdateOtpConfigRequest request = readJson(exchange, UpdateOtpConfigRequest.class);
            return new ApiResponse(200, adminService.updateConfig(request));
        }
        throw new org.example.exception.ApiException(405, "Method is not allowed");
    }
}
