package org.example;

import org.example.api.*;
import org.example.config.AppConfig;
import org.example.dao.OtpCodeDao;
import org.example.dao.OtpConfigDao;
import org.example.dao.UserDao;
import org.example.db.ConnectionFactory;
import org.example.db.DatabaseInitializer;
import org.example.model.DeliveryChannel;
import org.example.notification.EmailNotificationSender;
import org.example.notification.FileNotificationSender;
import org.example.notification.NotificationDispatcher;
import org.example.notification.NotificationSender;
import org.example.notification.SmsNotificationSender;
import org.example.notification.TelegramNotificationSender;
import org.example.security.PasswordHasher;
import org.example.security.TokenService;
import org.example.service.AdminService;
import org.example.service.AuthService;
import org.example.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpApplication {
    private static final Logger log = LoggerFactory.getLogger(OtpApplication.class);

    public void start() {
        AppConfig config = AppConfig.load();
        ConnectionFactory connectionFactory = new ConnectionFactory(
                config.dbUrl(),
                config.dbUser(),
                config.dbPassword()
        );

        new DatabaseInitializer(connectionFactory).initialize();

        UserDao userDao = new UserDao(connectionFactory);
        OtpConfigDao otpConfigDao = new OtpConfigDao(connectionFactory);
        OtpCodeDao otpCodeDao = new OtpCodeDao(connectionFactory);

        PasswordHasher passwordHasher = new PasswordHasher();
        TokenService tokenService = new TokenService(config.tokenSecret(), config.tokenTtlSeconds());
        AuthService authService = new AuthService(userDao, passwordHasher, tokenService);
        AdminService adminService = new AdminService(userDao, otpConfigDao);

        Map<DeliveryChannel, NotificationSender> senders = new EnumMap<>(DeliveryChannel.class);
        senders.put(DeliveryChannel.FILE, new FileNotificationSender(config.otpOutputFile()));
        senders.put(DeliveryChannel.EMAIL, new EmailNotificationSender());
        senders.put(DeliveryChannel.SMS, new SmsNotificationSender());
        senders.put(DeliveryChannel.TELEGRAM, new TelegramNotificationSender());
        NotificationDispatcher notificationDispatcher = new NotificationDispatcher(senders);

        OtpService otpService = new OtpService(otpConfigDao, otpCodeDao, notificationDispatcher);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                otpService::expireCodes,
                config.expirationCheckIntervalSeconds(),
                config.expirationCheckIntervalSeconds(),
                TimeUnit.SECONDS
        );

        try {
            ApiServer apiServer = new ApiServer(
                    config.httpPort(),
                    new HealthHandler(),
                    new RegisterHandler(authService),
                    new LoginHandler(authService),
                    new AdminConfigHandler(authService, adminService),
                    new AdminUsersHandler(authService, adminService),
                    new GenerateOtpHandler(authService, otpService),
                    new ValidateOtpHandler(authService, otpService)
            );

            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(apiServer, scheduler)));
            apiServer.start();
            log.info("OTP service started on http://localhost:{}", config.httpPort());
        } catch (IOException e) {
            scheduler.shutdownNow();
            throw new IllegalStateException("Failed to start HTTP server", e);
        }
    }

    private void shutdown(ApiServer apiServer, ScheduledExecutorService scheduler) {
        apiServer.stop();
        scheduler.shutdownNow();
        log.info("OTP service stopped");
    }
}
