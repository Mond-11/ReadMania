package ua.readmania.service.notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.user.User;

@Service
@Profile({"local", "test", "default"})
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    private final List<SentEmail> sentEmails = new CopyOnWriteArrayList<>();

    @Override
    public void sendNewOrderAlertToManagers(Order order) {
        String recipient = "managers@readmania.ua";
        String subject = "Нове замовлення #" + order.getId() + " на суму " + order.getTotalToPay() + " грн";
        String body = "Отримано нове замовлення #" + order.getId()
                + " (Тип: " + order.getType()
                + ", Спосіб оплати: " + order.getPaymentMethod()
                + ", Сума: " + order.getTotalToPay() + " грн)."
                + (order.getInstitutionName() != null ? " Заклад: " + order.getInstitutionName() : "");

        recordEmail(recipient, subject, body);
    }

    @Override
    public void sendOrderStatusChanged(Order order) {
        String recipient = order.getUser() != null ? order.getUser().getEmail() : "customer@example.com";
        String subject = "Зміна статусу замовлення #" + order.getId() + ": " + order.getStatus();
        String body = "Шановний клієнте, статус вашого замовлення #" + order.getId() + " змінено на: " + order.getStatus();

        recordEmail(recipient, subject, body);
    }

    @Override
    public void sendChallengeTerminatedEarly(User user, Challenge challenge) {
        String recipient = user.getEmail();
        String subject = "Читацький виклик '" + challenge.getTitle() + "' завершено достроково";
        String body = "Шановний(а) " + (user.getFullName() != null ? user.getFullName() : user.getEmail())
                + ", читацький виклик '" + challenge.getTitle() + "' було завершено достроково менеджером.";

        recordEmail(recipient, subject, body);
    }

    @Override
    public void sendPasswordResetLink(User user, String rawToken) {
        String recipient = user.getEmail();
        String subject = "Відновлення пароля у ReadMania";
        String body = "Для відновлення пароля перейдіть за посиланням: /reset-password?token=" + rawToken
                + " (Токен дійсний протягом 1 години: " + rawToken + ")";

        recordEmail(recipient, subject, body);
    }

    @Override
    public void sendAccountBlocked(User user) {
        String recipient = user.getEmail();
        String subject = "Ваш обліковий запис у ReadMania заблоковано";
        String body = "Ваш обліковий запис було заблоковано адміністрацією сайту. Причина: "
                + (user.getBlockReason() != null ? user.getBlockReason() : "Порушення правил сервісу");

        recordEmail(recipient, subject, body);
    }

    @Override
    public void sendAccountUnblocked(User user) {
        String recipient = user.getEmail();
        String subject = "Ваш обліковий запис у ReadMania розблоковано";
        String body = "Ваш обліковий запис було успішно розблоковано. Ви знову можете користуватися всіма функціями сайту.";

        recordEmail(recipient, subject, body);
    }

    private void recordEmail(String recipient, String subject, String body) {
        SentEmail email = new SentEmail(recipient, subject, body, Instant.now());
        sentEmails.add(email);
        log.info("[NOTIFICATION LOG] To: {} | Subject: {} | Body: {}", recipient, subject, body);
    }

    public List<SentEmail> getSentEmails() {
        return Collections.unmodifiableList(new ArrayList<>(sentEmails));
    }

    public SentEmail getLastSentEmail() {
        if (sentEmails.isEmpty()) {
            return null;
        }
        return sentEmails.get(sentEmails.size() - 1);
    }

    public void clear() {
        sentEmails.clear();
    }
}
