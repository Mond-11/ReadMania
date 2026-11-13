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

    @BeforeEach
    void setUp() {
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
                .andExpect(status().isUnauthorized());

        User lockedUser = userRepository.findByEmailIgnoreCase("bruteforce@example.com").orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(Instant.now());

        // Subsequent login attempt while locked is rejected with 400 Bad Request Problem Details
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Порушення бізнес-правил")));
    }

    @Test
    @DisplayName("G4, M6: Blocked user login returns 403 Forbidden with reason in Problem Details")
    void shouldRejectLoginForBlockedUserWith403() throws Exception {
        User user = new User();
        user.setEmail("blocked@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason("Порушення правил спільноти");
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("blocked@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.detail", is("Порушення правил спільноти")));
    }

    @Test
    @DisplayName("M6: AccountStatusFilter blocks active session immediately when account is set to BLOCKED")
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
        userRepository.save(user);

        // Next request in same session is immediately blocked with 403 Problem Details
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.detail", is("Блокування через підозрілу активність")));
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
                null
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
}
