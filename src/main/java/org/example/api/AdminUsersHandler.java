package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.dto.MessageResponse;
import org.example.service.AdminService;
import org.example.service.AuthService;

public class AdminUsersHandler extends BaseAuthenticatedHandler {
    private final AdminService adminService;

    public AdminUsersHandler(AuthService authService, AdminService adminService) {
        super(authService);
        this.adminService = adminService;
    }

    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) {
        requireAdmin(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && "/admin/users".equals(path)) {
            return new ApiResponse(200, adminService.getUsers());
        }

        if ("DELETE".equalsIgnoreCase(method) && path.startsWith("/admin/users/")) {
            long userId = Long.parseLong(path.substring("/admin/users/".length()));
            adminService.deleteUser(userId);
            return new ApiResponse(200, new MessageResponse("User deleted successfully"));
        }

        throw new org.example.exception.ApiException(405, "Method or path is not allowed");
    }
}
