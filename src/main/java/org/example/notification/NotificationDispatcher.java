package org.example.notification;

import java.util.Map;
import org.example.exception.ApiException;
import org.example.model.DeliveryChannel;

public class NotificationDispatcher {
    private final Map<DeliveryChannel, NotificationSender> senders;

    public NotificationDispatcher(Map<DeliveryChannel, NotificationSender> senders) {
        this.senders = Map.copyOf(senders);
    }

    public void send(DeliveryChannel channel, String username, String destination, String operationId, String code) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new ApiException(400, "Channel %s is not configured yet".formatted(channel.name()));
        }
        sender.send(username, destination, operationId, code);
    }
}
