package ua.readmania.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.AccountBlockedException;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.LoginRateLimitedException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.user.PasswordResetToken;
import ua.readmania.domain.user.PasswordResetTokenRepository;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.UnblockRequest;
import ua.readmania.domain.user.UnblockRequestRepository;
import ua.readmania.domain.user.UnblockRequestStatus;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.auth.CreateUnblockRequest;
import ua.readmania.dto.auth.LoginRequest;
import ua.readmania.dto.auth.PasswordResetConfirmRequest;
import ua.readmania.dto.auth.PasswordResetRequest;
import ua.readmania.dto.auth.RegisterRequest;
import ua.readmania.dto.auth.UserResponse;
import ua.readmania.service.notification.NotificationService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UnblockRequestRepository unblockRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       UnblockRequestRepository unblockRequestRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.unblockRequestRepository = unblockRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("Паролі не збігаються");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Користувач із такою електронною адресою вже існує");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setBonusBalance(0);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());

        user = userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getStatus() == UserStatus.BLOCKED) {
                String reason = user.getBlockReason() != null && !user.getBlockReason().isBlank()
                        ? user.getBlockReason()
                        : "Обліковий запис заблоковано";
                throw new AccountBlockedException(reason, user.getBlockReason(), user.getBlockedAt());
            }

            if (user.getLockedUntil() != null) {
                if (Instant.now().isBefore(user.getLockedUntil())) {
                    throw new LoginRateLimitedException(
                            "Обліковий запис тимчасово заблоковано через перевищення ліміту спроб входу (5 спроб). Спробуйте через 15 хвилин.",
                            user.getLockedUntil()
                    );
                } else {
                    user.setLockedUntil(null);
                    user.setFailedLoginAttempts(0);
                    userRepository.save(user);
                }
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            User user = userOpt.orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizedEmail).orElseThrow());
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(Instant.now());
            user = userRepository.save(user);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            return UserResponse.fromEntity(user);
        } catch (AuthenticationException ex) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= 5) {
                    Instant lockedUntil = Instant.now().plus(Duration.ofMinutes(15));
                    user.setLockedUntil(lockedUntil);
                    userRepository.save(user);
                    throw new LoginRateLimitedException(
                            "Перевищено ліміт спроб входу (5 спроб). Обліковий запис тимчасово заблоковано на 15 хвилин.",
                            lockedUntil
                    );
                }
                userRepository.save(user);
            }
            throw new BadCredentialsException("Невірний email або пароль", ex);
        }
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String tokenStr = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken(user, tokenStr, Instant.now().plus(Duration.ofHours(1)));
            passwordResetTokenRepository.save(token);

            notificationService.sendPasswordResetLink(user, tokenStr);
        }
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("Паролі не збігаються");
        }

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Токен скидання пароля не знайдено або він недійсний"));

        if (token.isExpired() || token.isUsed()) {
            throw new BusinessRuleException("Токен скидання пароля застарів або вже був використаний");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        token.setUsedAt(Instant.now());

        userRepository.save(user);
        passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void submitUnblockRequest(CreateUnblockRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Користувача з такою електронною адресою не знайдено"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Невірний пароль");
        }

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new BusinessRuleException("Обліковий запис не заблокований");
        }

        Optional<UnblockRequest> existingOpt = unblockRequestRepository.findFirstByUserIdAndStatus(user.getId(), UnblockRequestStatus.PENDING);
        if (existingOpt.isPresent()) {
            UnblockRequest unblockRequest = existingOpt.get();
            unblockRequest.setReason(request.getReason());
            unblockRequest.setCreatedAt(Instant.now());
            unblockRequestRepository.save(unblockRequest);
        } else {
            UnblockRequest unblockRequest = new UnblockRequest(user, request.getReason());
            unblockRequestRepository.save(unblockRequest);
        }
    }
}
