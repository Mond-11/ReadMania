package ua.readmania.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.user.User;

@Service
@Profile("prod")
public class SmtpNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@readmania.ua}")
    private String fromEmail;

    public SmtpNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendNewOrderAlertToManagers(Order order) {
        String recipient = "managers@readmania.ua";
        String subject = "Нове замовлення #" + order.getId() + " на суму " + order.getTotalToPay() + " грн";
        String body = "Отримано нове замовлення #" + order.getId()
                + " (Тип: " + order.getType()
                + ", Спосіб оплати: " + order.getPaymentMethod()
                + ", Сума: " + order.getTotalToPay() + " грн)."
                + (order.getInstitutionName() != null ? " Заклад: " + order.getInstitutionName() : "");

        sendMail(recipient, subject, body);
    }

    @Async
    @Override
    public void sendOrderStatusChanged(Order order) {
        String recipient = order.getUser() != null ? order.getUser().getEmail() : "customer@example.com";
        String subject = "Зміна статусу замовлення #" + order.getId() + ": " + order.getStatus();
        String body = "Шановний клієнте, статус вашого замовлення #" + order.getId() + " змінено на: " + order.getStatus();

        sendMail(recipient, subject, body);
    }

    @Async
    @Override
    public void sendChallengeTerminatedEarly(User user, Challenge challenge) {
        String recipient = user.getEmail();
        String subject = "Читацький виклик '" + challenge.getTitle() + "' завершено достроково";
        String body = "Шановний(а) " + (user.getFullName() != null ? user.getFullName() : user.getEmail())
                + ", читацький виклик '" + challenge.getTitle() + "' було завершено достроково менеджером.";

        sendMail(recipient, subject, body);
    }

    @Async
    @Override
    public void sendPasswordResetLink(User user, String rawToken) {
        String recipient = user.getEmail();
        String subject = "Відновлення пароля у ReadMania";
        String body = "Для відновлення пароля перейдіть за посиланням: /reset-password?token=" + rawToken
                + "\nТокен дійсний протягом 1 години.";

        sendMail(recipient, subject, body);
    }

    @Async
    @Override
    public void sendAccountBlocked(User user) {
        String recipient = user.getEmail();
        String subject = "Ваш обліковий запис у ReadMania заблоковано";
        String body = "Ваш обліковий запис було заблоковано адміністрацією сайту. Причина: "
                + (user.getBlockReason() != null ? user.getBlockReason() : "Порушення правил сервісу");

        sendMail(recipient, subject, body);
    }

    @Async
    @Override
    public void sendAccountUnblocked(User user) {
        String recipient = user.getEmail();
        String subject = "Ваш обліковий запис у ReadMania розблоковано";
        String body = "Ваш обліковий запис було успішно розблоковано. Ви знову можете користуватися всіма функціями сайту.";

        sendMail(recipient, subject, body);
    }

    private void sendMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Sent email to {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
