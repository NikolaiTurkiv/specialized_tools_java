package org.example.notification;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.config.PropertiesLoader;
import org.example.exception.ApiException;
import org.example.model.DeliveryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
//у меня отрабатывает только с мобильным интернетом, SMTP-диалог не начинается на wifi, провайдер скорее всего блокирует
public class EmailNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailNotificationSender() {
        PropertiesLoader loader = new PropertiesLoader("email.properties");
        Properties mailProperties = loadMailProperties(loader);
        this.username = loader.get("email.username", "");
        this.password = loader.get("email.password", "");
        this.fromEmail = loader.get("email.from", "");
        this.session = Session.getInstance(mailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public void send(String username, String destination, String operationId, String code) {
        ensureConfigured();

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(destination));
            message.setSubject("Your OTP Code");
            message.setText(buildMessageBody(username, operationId, code));

            Transport.send(message);
            log.info("OTP email sent to {} for operation {}", destination, operationId);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {} for operation {}", destination, operationId, e);
            throw new ApiException(502, "Failed to send OTP email: " + e.getMessage());
        }
    }

    private Properties loadMailProperties(PropertiesLoader loader) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", loader.get("mail.smtp.host", ""));
        properties.setProperty("mail.smtp.port", loader.get("mail.smtp.port", "587"));
        properties.setProperty("mail.smtp.auth", loader.get("mail.smtp.auth", "true"));
        properties.setProperty("mail.smtp.starttls.enable", loader.get("mail.smtp.starttls.enable", "true"));
        return properties;
    }

    private void ensureConfigured() {
        if (username.isBlank() || password.isBlank() || fromEmail.isBlank()) {
            throw new ApiException(500, "Email channel is not configured. Fill src/main/resources/email.properties");
        }
    }

    private String buildMessageBody(String username, String operationId, String code) {
        return """
                Hello, %s!

                Your verification code for operation %s is: %s

                This code is one-time only.
                """
                .formatted(username, operationId, code);
    }
}
