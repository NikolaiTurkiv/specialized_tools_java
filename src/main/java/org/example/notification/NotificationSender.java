package org.example.notification;

import org.example.model.DeliveryChannel;

public interface NotificationSender {
    DeliveryChannel channel();

    void send(String username, String destination, String operationId, String code);
}
