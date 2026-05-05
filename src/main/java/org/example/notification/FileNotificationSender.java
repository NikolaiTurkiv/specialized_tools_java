package org.example.notification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.example.model.DeliveryChannel;

public class FileNotificationSender implements NotificationSender {
    private final Path filePath;

    public FileNotificationSender(String filePath) {
        this.filePath = Path.of(filePath).toAbsolutePath();
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.FILE;
    }

    @Override
    public void send(String username, String destination, String operationId, String code) {
        String line = "%s | user=%s | operation=%s | code=%s%n"
                .formatted(Instant.now(), username, operationId, code);
        try {
            Files.writeString(
                    filePath,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write OTP to file " + filePath, e);
        }
    }
}
