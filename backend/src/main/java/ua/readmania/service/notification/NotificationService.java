package ua.readmania.service.notification;

import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.user.User;

public interface NotificationService {

    void sendNewOrderAlertToManagers(Order order);

    void sendOrderStatusChanged(Order order);

    void sendChallengeTerminatedEarly(User user, Challenge challenge);

    void sendPasswordResetLink(User user, String rawToken);

    void sendAccountBlocked(User user);

    void sendAccountUnblocked(User user);
}
