package ua.readmania.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.auth.UserResponse;
import ua.readmania.dto.user.ChangePasswordRequest;
import ua.readmania.dto.user.UpdateProfileRequest;
import ua.readmania.security.CustomUserDetails;
import ua.readmania.service.notification.NotificationService;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SecurityContextRepository securityContextRepository,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(String currentEmail, UpdateProfileRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = userRepository.findByEmailIgnoreCase(currentEmail)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new BusinessRuleException("Для оновлення профілю необхідно обов'язково вказати поточний пароль");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Невірний поточний пароль");
        }

        boolean emailChanged = false;
        if (request.getEmail() != null && !request.getEmail().trim().equalsIgnoreCase(currentEmail)) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new ConflictException("Електронна адреса вже використовується іншим користувачем");
            }
            user.setEmail(newEmail);
            emailChanged = true;
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getDeliveryAddress() != null) {
            user.setDeliveryAddress(request.getDeliveryAddress().trim());
        }

        user = userRepository.save(user);

        if (emailChanged) {
            CustomUserDetails updatedPrincipal = new CustomUserDetails(user);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    updatedPrincipal,
                    null,
                    updatedPrincipal.getAuthorities()
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(newAuth);
            SecurityContextHolder.setContext(context);
            if (httpRequest != null && httpResponse != null) {
                securityContextRepository.saveContext(context, httpRequest, httpResponse);
            }
        }

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("Новий пароль та підтвердження не збігаються");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Невірний поточний пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user = userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse blockUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача з ID " + userId + " не знайдено"));

        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason(reason);
        user.setBlockedAt(Instant.now());
        user = userRepository.save(user);

        notificationService.sendAccountBlocked(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача з ID " + userId + " не знайдено"));

        user.setStatus(UserStatus.ACTIVE);
        user.setBlockReason(null);
        user.setBlockedAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user = userRepository.save(user);

        notificationService.sendAccountUnblocked(user);
        return UserResponse.fromEntity(user);
    }
}
