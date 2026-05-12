package org.example.notification;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.example.config.PropertiesLoader;
import org.example.exception.ApiException;
import org.example.model.DeliveryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelegramNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationSender.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String botToken;
    private final String chatId;
    private final String apiBaseUrl;

    public TelegramNotificationSender() {
        PropertiesLoader loader = new PropertiesLoader("telegram.properties");
        this.botToken = loader.get("telegram.bot.token", "");
        this.chatId = loader.get("telegram.chat.id", "");
        this.apiBaseUrl = loader.get("telegram.api.base-url", "https://api.telegram.org");
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.TELEGRAM;
    }

    @Override
    public void send(String username, String destination, String operationId, String code) {
        ensureConfigured();

        String message = buildMessage(username, destination, operationId, code);
        String url = "%s/bot%s/sendMessage?chat_id=%s&text=%s".formatted(
                apiBaseUrl,
                botToken,
                chatId,
                urlEncode(message)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Telegram API returned status {} with body {}", response.statusCode(), response.body());
                throw new ApiException(502, "Failed to send Telegram message. Status: " + response.statusCode());
            }
            log.info("OTP Telegram message sent for operation {}", operationId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "Telegram request was interrupted");
        } catch (IOException e) {
            log.error("Failed to send Telegram message for operation {}", operationId, e);
            throw new ApiException(502, "Failed to send Telegram message: " + e.getMessage());
        }
    }

    private void ensureConfigured() {
        if (botToken.isBlank() || chatId.isBlank()) {
            throw new ApiException(500, "Telegram channel is not configured. Fill src/main/resources/telegram.properties");
        }
    }

    private String buildMessage(String username, String destination, String operationId, String code) {
        return """
                Hello, %s!

                Recipient: %s
                Operation: %s
                OTP code: %s
                """
                .formatted(username, destination, operationId, code);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
