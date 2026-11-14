package ua.readmania.service.notification;

import java.time.Instant;

public record SentEmail(
        String recipient,
        String subject,
        String body,
        Instant sentAt
) {
}
