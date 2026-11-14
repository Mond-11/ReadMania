package ua.readmania;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.domain.user.PasswordResetToken;
import ua.readmania.domain.user.PasswordResetTokenRepository;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.auth.LoginRequest;
import ua.readmania.dto.auth.PasswordResetConfirmRequest;
import ua.readmania.dto.auth.PasswordResetRequest;
import ua.readmania.dto.auth.RegisterRequest;
import ua.readmania.dto.user.ChangePasswordRequest;
import ua.readmania.dto.user.UpdateProfileRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ua.readmania.domain.user.UnblockRequestRepository unblockRequestRepository;

    @Autowired
    private ua.readmania.service.notification.LoggingNotificationService loggingNotificationService;

    @BeforeEach
    void setUp() {
        loggingNotificationService.clear();
        unblockRequestRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("G2: Successful registration creates user with BCrypt password and lowercase email")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Reader.Test@Example.COM",
                "Secret123!",
                "Secret123!",
                "Олександр Читач"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("reader.test@example.com")))
                .andExpect(jsonPath("$.fullName", is("Олександр Читач")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.bonusBalance", is(0)));

        User user = userRepository.findByEmailIgnoreCase("reader.test@example.com").orElseThrow();
        assertThat(user.getEmail()).isEqualTo("reader.test@example.com");
        assertThat(passwordEncoder.matches("Secret123!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("G2: Registration rejects duplicate email with 409 Conflict Problem Details")
    void shouldRejectDuplicateEmailOnRegistration() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setPasswordHash(passwordEncoder.encode("Pass123!"));
        existingUser.setRole(Role.USER);
        existingUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest(
                "EXISTING@example.com",
                "NewSecret123!",
                "NewSecret123!",
                "Дублікат"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.title", is("Конфлікт даних")));
    }

    @Test
    @DisplayName("G2: Registration rejects password mismatch with 400 Bad Request Problem Details")
    void shouldRejectPasswordMismatchOnRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "mismatch@example.com",
                "Secret123!",
                "Different123!",
                "Mismatch"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Порушення бізнес-правил")));
    }

    @Test
    @DisplayName("U3: Registration with invalid email or empty password returns 400 with errors array")
    void shouldReturnValidationErrorsOnInvalidRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "not-an-email",
                "",
                "123",
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Помилка валідації")))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("G3: Login establishes session, updates lastLoginAt, and resets failed attempts")
    void shouldLoginSuccessfullyAndEstablishSession() throws Exception {
        User user = new User();
        user.setEmail("login@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(2);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("LOGIN@example.com", "Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("login@example.com")))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        User updatedUser = userRepository.findByEmailIgnoreCase("login@example.com").orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);

        // Access /api/auth/me with established session
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("login@example.com")));
    }

    @Test
    @DisplayName("G3: Bad credentials increments failedLoginAttempts and returns 401 Problem Details")
    void shouldIncrementFailedAttemptsOnBadCredentials() throws Exception {
        User user = new User();
        user.setEmail("badpass@example.com");
        user.setPasswordHash(passwordEncoder.encode("CorrectPassword123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("badpass@example.com", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Unauthorized")));

        User updatedUser = userRepository.findByEmailIgnoreCase("badpass@example.com").orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("G3: Account is locked for 15 minutes after 5 consecutive failed login attempts")
    void shouldLockAccountAfter5FailedLoginAttempts() throws Exception {
        User user = new User();
        user.setEmail("bruteforce@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(4);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("bruteforce@example.com", "WrongPassword!");

        // 5th failed attempt triggers 15 min lock
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", is("urn:readmania:error:login-rate-limited")))
                .andExpect(jsonPath("$.retryAfter").exists());

        User lockedUser = userRepository.findByEmailIgnoreCase("bruteforce@example.com").orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(Instant.now());

        // Subsequent login attempt while locked is rejected with 403 Forbidden Problem Details
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", is("urn:readmania:error:login-rate-limited")))
                .andExpect(jsonPath("$.retryAfter").exists());
    }

    @Test
    @DisplayName("G4, M6: Blocked user login returns 403 Forbidden with reason and type in Problem Details")
    void shouldRejectLoginForBlockedUserWith403() throws Exception {
        User user = new User();
        user.setEmail("blocked@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason("Порушення правил спільноти");
        user.setBlockedAt(Instant.now());
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("blocked@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.type", is("urn:readmania:error:account-blocked")))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.detail", is("Порушення правил спільноти")))
                .andExpect(jsonPath("$.blockReason", is("Порушення правил спільноти")))
                .andExpect(jsonPath("$.blockedAt").exists());
    }

    @Test
    @DisplayName("M6: AccountStatusFilter blocks active session immediately when account is set to BLOCKED with urn:readmania:error:account-blocked")
    void shouldInvalidateActiveSessionWhenUserBecomesBlocked() throws Exception {
        User user = new User();
        user.setEmail("active-then-blocked@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("active-then-blocked@example.com", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Active request works
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk());

        // Now manager blocks this user in database
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason("Блокування через підозрілу активність");
        user.setBlockedAt(Instant.now());
        userRepository.save(user);

        // Next request in same session is immediately blocked with 403 Problem Details and custom properties
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.type", is("urn:readmania:error:account-blocked")))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.detail", is("Блокування через підозрілу активність")))
                .andExpect(jsonPath("$.blockReason", is("Блокування через підозрілу активність")))
                .andExpect(jsonPath("$.blockedAt").exists());
    }

    @Test
    @DisplayName("C10: Logout invalidates session and subsequent /me returns 401")
    void shouldLogoutSuccessfullyAndInvalidateSession() throws Exception {
        User user = new User();
        user.setEmail("logout@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("logout@example.com", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        // Accessing /me after logout returns 401
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("G3: Password reset request generates 1-hour token and confirm updates password")
    void shouldRequestAndConfirmPasswordResetSuccessfully() throws Exception {
        User user = new User();
        user.setEmail("reset@example.com");
        user.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Request reset
        PasswordResetRequest request = new PasswordResetRequest("reset@example.com");
        MvcResult requestResult = mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        PasswordResetToken token = passwordResetTokenRepository.findAll().getFirst();
        assertThat(token.getUser().getEmail()).isEqualTo("reset@example.com");
        assertThat(token.isExpired()).isFalse();
        assertThat(token.isUsed()).isFalse();

        // Confirm reset
        PasswordResetConfirmRequest confirmRequest = new PasswordResetConfirmRequest(
                token.getToken(),
                "NewPassword123!",
                "NewPassword123!"
        );

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // Login with new password succeeds
        LoginRequest newLogin = new LoginRequest("reset@example.com", "NewPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C5: Profile update and password change with verification of current password")
    void shouldUpdateProfileAndChangePassword() throws Exception {
        User user = new User();
        user.setEmail("profile@example.com");
        user.setPasswordHash(passwordEncoder.encode("CurrentPass123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFullName("Старе Ім'я");
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("profile@example.com", "CurrentPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Update profile details
        UpdateProfileRequest updateProfileRequest = new UpdateProfileRequest(
                "Нове Ім'я",
                "+380501234567",
                "м. Київ, вул. Хрещатик, 1",
                null,
                "CurrentPass123!"
        );

        mockMvc.perform(put("/api/account")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Нове Ім'я")))
                .andExpect(jsonPath("$.phone", is("+380501234567")))
                .andExpect(jsonPath("$.deliveryAddress", is("м. Київ, вул. Хрещатик, 1")));

        // Change password with valid current password
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                "CurrentPass123!",
                "BrandNewPass123!",
                "BrandNewPass123!"
        );

        mockMvc.perform(put("/api/account/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk());

        // Changing password with wrong current password is rejected with 400 Bad Request
        ChangePasswordRequest badChangeRequest = new ChangePasswordRequest(
                "WrongCurrentPass!",
                "AnotherPass123!",
                "AnotherPass123!"
        );

        mockMvc.perform(put("/api/account/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badChangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Порушення бізнес-правил")));
    }

    @Test
    @DisplayName("Patch 2: Password reset request returns fixed message without token and sends email via NotificationService")
    void shouldSendPasswordResetNotificationAndNotExposeTokenInResponse() throws Exception {
        User user = new User();
        user.setEmail("user_reset@example.com");
        user.setPasswordHash(passwordEncoder.encode("Pass123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PasswordResetRequest request = new PasswordResetRequest("user_reset@example.com");

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Якщо обліковий запис існує, лист надіслано")))
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(loggingNotificationService.getSentEmails()).hasSize(1);
        var sentEmail = loggingNotificationService.getLastSentEmail();
        assertThat(sentEmail.recipient()).isEqualTo("user_reset@example.com");
        assertThat(sentEmail.subject()).contains("Відновлення пароля");

        // Non-existent email returns same message for security
        loggingNotificationService.clear();
        PasswordResetRequest nonExistentRequest = new PasswordResetRequest("nonexistent@example.com");
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Якщо обліковий запис існує, лист надіслано")))
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(loggingNotificationService.getSentEmails()).isEmpty();
    }

    @Test
    @DisplayName("Patch 2: Unblock request submission for blocked user creates/updates request and validates password")
    void shouldSubmitUnblockRequestAndHandleDuplicateOrInvalid() throws Exception {
        User user = new User();
        user.setEmail("blocked_appeal@example.com");
        user.setPasswordHash(passwordEncoder.encode("SecretPass123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason("Порушення умов сервісу");
        user.setBlockedAt(Instant.now());
        userRepository.save(user);

        // 1. Wrong password rejected
        ua.readmania.dto.auth.CreateUnblockRequest wrongPassReq = new ua.readmania.dto.auth.CreateUnblockRequest(
                "blocked_appeal@example.com",
                "WrongPass!",
                "Прошу розблокувати мій акаунт"
        );

        mockMvc.perform(post("/api/auth/unblock-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Невірний пароль")));

        // 2. Correct submission creates unblock request
        ua.readmania.dto.auth.CreateUnblockRequest validReq = new ua.readmania.dto.auth.CreateUnblockRequest(
                "blocked_appeal@example.com",
                "SecretPass123!",
                "Прошу розблокувати мій акаунт, я зрозумів помилку"
        );

        mockMvc.perform(post("/api/auth/unblock-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Запит на розблокування успішно надіслано")));

        var requests = unblockRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().getReason()).isEqualTo("Прошу розблокувати мій акаунт, я зрозумів помилку");
        assertThat(requests.getFirst().getStatus()).isEqualTo(ua.readmania.domain.user.UnblockRequestStatus.PENDING);

        // 3. Second submission updates reason of existing pending request
        ua.readmania.dto.auth.CreateUnblockRequest updateReq = new ua.readmania.dto.auth.CreateUnblockRequest(
                "blocked_appeal@example.com",
                "SecretPass123!",
                "Оновлена причина: більше так не буду"
        );

        mockMvc.perform(post("/api/auth/unblock-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        var updatedRequests = unblockRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(updatedRequests).hasSize(1);
        assertThat(updatedRequests.getFirst().getReason()).isEqualTo("Оновлена причина: більше так не буду");

        // 4. Request for active user is rejected
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/unblock-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Обліковий запис не заблокований")));
    }

    @Test
    @DisplayName("Patch 2: Profile email update requires password, checks lowercase uniqueness, and refreshes session SecurityContext")
    void shouldUpdateEmailWithPasswordVerificationAndRefreshSessionSecurityContext() throws Exception {
        User user1 = new User();
        user1.setEmail("initial_email@example.com");
        user1.setPasswordHash(passwordEncoder.encode("CurrentPass123!"));
        user1.setRole(Role.USER);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setFullName("Тарас Шевченко");
        userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("existing_target@example.com");
        user2.setPasswordHash(passwordEncoder.encode("Pass123!"));
        user2.setRole(Role.USER);
        user2.setStatus(UserStatus.ACTIVE);
        userRepository.save(user2);

        // Login as user1
        LoginRequest loginRequest = new LoginRequest("initial_email@example.com", "CurrentPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // 1. Conflict on existing email
        UpdateProfileRequest conflictReq = new UpdateProfileRequest(
                "Тарас Шевченко",
                null,
                null,
                "EXISTING_TARGET@example.com",
                "CurrentPass123!"
        );

        mockMvc.perform(put("/api/account")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Конфлікт даних")));

        // 2. Missing current password
        UpdateProfileRequest missingPassReq = new UpdateProfileRequest(
                "Тарас Шевченко",
                null,
                null,
                "new_unique_email@example.com",
                null
        );

        mockMvc.perform(put("/api/account")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingPassReq)))
                .andExpect(status().isBadRequest());

        // 3. Successful email update
        UpdateProfileRequest validEmailChange = new UpdateProfileRequest(
                "Тарас Шевченко",
                "+380991112233",
                "м. Львів, пл. Ринок, 1",
                "NEW_UNIQUE_EMAIL@example.com",
                "CurrentPass123!"
        );

        mockMvc.perform(put("/api/account")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailChange)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("new_unique_email@example.com")))
                .andExpect(jsonPath("$.deliveryAddress", is("м. Львів, пл. Ринок, 1")));

        // 4. Verify session security context updated to new email
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("new_unique_email@example.com")));
    }
}
