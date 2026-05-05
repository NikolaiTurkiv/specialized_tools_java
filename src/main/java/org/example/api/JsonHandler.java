package org.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.example.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JsonHandler implements HttpHandler {
    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        Instant startedAt = Instant.now();
        int statusCode = 500;

        try {
            ApiResponse response = handleRequest(exchange);
            statusCode = response.statusCode();
            writeJson(exchange, statusCode, response.body());
        } catch (ApiException e) {
            statusCode = e.statusCode();
            writeJson(exchange, statusCode, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unhandled error while processing request", e);
            writeJson(exchange, 500, Map.of("error", "Internal server error"));
        } finally {
            long tookMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info("{} {} -> {} ({} ms)",
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    statusCode,
                    tookMs);
            exchange.close();
        }
    }

    protected abstract ApiResponse handleRequest(HttpExchange exchange) throws Exception;

    protected void requireMethod(HttpExchange exchange, String method) {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new ApiException(405, "Method " + exchange.getRequestMethod() + " is not allowed");
        }
    }

    protected <T> T readJson(HttpExchange exchange, Class<T> bodyType) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            return objectMapper.readValue(requestBody, bodyType);
        }
    }

    protected String authorizationHeader(HttpExchange exchange) {
        return exchange.getRequestHeaders().getFirst("Authorization");
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }
}
