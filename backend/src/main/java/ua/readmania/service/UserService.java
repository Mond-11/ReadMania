package ua.readmania.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.common.BusinessRuleException;
import ua.readmania.common.ConflictException;
import ua.readmania.common.NotFoundException;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.auth.UserResponse;
import ua.readmania.dto.user.ChangePasswordRequest;
import ua.readmania.dto.user.UpdateProfileRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(currentEmail)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));

        if (request.getEmail() != null && !request.getEmail().trim().equalsIgnoreCase(currentEmail)) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new BusinessRuleException("Для зміни email необхідно обов'язково надати поточний пароль");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessRuleException("Невірний поточний пароль");
            }
            String newEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new ConflictException("Електронна адреса вже використовується іншим користувачем");
            }
            user.setEmail(newEmail);
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
}
