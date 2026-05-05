package org.example.api;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ApiServer {
    private final HttpServer httpServer;

    public ApiServer(
            int port,
            HealthHandler healthHandler,
            RegisterHandler registerHandler,
            LoginHandler loginHandler,
            AdminConfigHandler adminConfigHandler,
            AdminUsersHandler adminUsersHandler,
            GenerateOtpHandler generateOtpHandler,
            ValidateOtpHandler validateOtpHandler
    ) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/health", healthHandler);
        httpServer.createContext("/auth/register", registerHandler);
        httpServer.createContext("/auth/login", loginHandler);
        httpServer.createContext("/admin/config", adminConfigHandler);
        httpServer.createContext("/admin/users", adminUsersHandler);
        httpServer.createContext("/admin/users/", adminUsersHandler);
        httpServer.createContext("/otp/generate", generateOtpHandler);
        httpServer.createContext("/otp/validate", validateOtpHandler);
        httpServer.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }
}
