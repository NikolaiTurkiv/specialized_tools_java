package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import java.time.Instant;
import java.util.Map;

public class HealthHandler extends JsonHandler {
    @Override
    protected ApiResponse handleRequest(HttpExchange exchange) {
        requireMethod(exchange, "GET");
        return new ApiResponse(200, Map.of("status", "UP", "time", Instant.now().toString()));
    }
}
