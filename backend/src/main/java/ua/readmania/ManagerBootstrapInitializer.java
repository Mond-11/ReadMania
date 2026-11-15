package ua.readmania;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;

@Component
public class ManagerBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ManagerBootstrapInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-manager.email:}")
    private String initialManagerEmail;

    @Value("${app.initial-manager.password:}")
    private String initialManagerPassword;

    public ManagerBootstrapInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (initialManagerEmail == null || initialManagerEmail.isBlank() ||
                initialManagerPassword == null || initialManagerPassword.isBlank()) {
            return;
        }

        String email = initialManagerEmail.trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Initial manager account ({}) already exists, skipping creation.", email);
            return;
        }

        User manager = new User(
                email,
                passwordEncoder.encode(initialManagerPassword),
                Role.MANAGER,
                "Тестовий Менеджер"
        );
        manager.setStatus(UserStatus.ACTIVE);
        manager.setBonusBalance(0);
        userRepository.save(manager);
        log.info("Initial manager account ({}) created successfully.", email);
    }
}
