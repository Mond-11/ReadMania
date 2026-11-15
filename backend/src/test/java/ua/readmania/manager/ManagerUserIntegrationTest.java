package ua.readmania.manager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.BaseIntegrationTest;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.order.DeliveryMethod;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.UnblockRequest;
import ua.readmania.domain.user.UnblockRequestRepository;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.auth.LoginRequest;
import ua.readmania.dto.manager.user.BlockUserRequest;
import ua.readmania.dto.manager.user.UnblockUsersRequest;
import ua.readmania.service.notification.LoggingNotificationService;
import ua.readmania.service.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ManagerUserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UnblockRequestRepository unblockRequestRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    private User managerUser;
    private User regularUser1;
    private User regularUser2;
    private User blockedUser;

    @BeforeEach
    void setUp() {
        if (notificationService instanceof LoggingNotificationService loggingNotificationService) {
            loggingNotificationService.clear();
        }

        unblockRequestRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.flush();

        managerUser = new User("manager@readmania.ua", passwordEncoder.encode("ManagerPass123!"), Role.MANAGER, "Головний Менеджер");
        managerUser = userRepository.save(managerUser);

        regularUser1 = new User("ivan.petrenko@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Іван Петренко");
        regularUser1.setBonusBalance(50);
        regularUser1.setLastLoginAt(Instant.now().minusSeconds(3600));
        regularUser1 = userRepository.save(regularUser1);

        regularUser2 = new User("olena.koval@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Олена Коваль");
        regularUser2.setBonusBalance(120);
        regularUser2 = userRepository.save(regularUser2);

        blockedUser = new User("blocked.user@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Заблокований Користувач");
        blockedUser.setStatus(UserStatus.BLOCKED);
        blockedUser.setBlockReason("Порушення правил замовлень");
        blockedUser.setBlockedAt(Instant.now().minusSeconds(7200));
        blockedUser = userRepository.save(blockedUser);
    }

    private MockHttpSession loginAs(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    @Test
    @DisplayName("GET /api/manager/users: повертає список лише звичайних користувачів, підтримує фільтрацію та пагінацію")
    void shouldSearchAndFilterUsers() throws Exception {
        MockHttpSession managerSession = loginAs("manager@readmania.ua", "ManagerPass123!");

        // 1. All users (excluding MANAGER)
        mockMvc.perform(get("/api/manager/users")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(
                        "ivan.petrenko@example.com",
                        "olena.koval@example.com",
                        "blocked.user@example.com"
                )));

        // 2. Filter by email substring
        mockMvc.perform(get("/api/manager/users")
                        .param("email", "petrenko")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("ivan.petrenko@example.com")));

        // 3. Filter by status BLOCKED
        mockMvc.perform(get("/api/manager/users")
                        .param("status", "BLOCKED")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("blocked.user@example.com")))
                .andExpect(jsonPath("$.content[0].status", is("BLOCKED")));

        // 4. Access control: 401 for anonymous, 403 for regular USER
        mockMvc.perform(get("/api/manager/users"))
                .andExpect(status().isUnauthorized());

        MockHttpSession userSession = loginAs("ivan.petrenko@example.com", "UserPass123!");
        mockMvc.perform(get("/api/manager/users").session(userSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/manager/users/{id}: повертає детальну картку користувача з агрегацією замовлень за статусами")
    void shouldReturnUserDetailsWithOrderAggregation() throws Exception {
        MockHttpSession managerSession = loginAs("manager@readmania.ua", "ManagerPass123!");

        Genre genre = genreRepository.findByNameIgnoreCase("Фантастика")
                .orElseGet(() -> genreRepository.save(new Genre("Фантастика")));
        Book book = bookRepository.findByIsbn("9786171234567")
                .orElseGet(() -> bookRepository.save(new Book("Тестова Книга", "9786171234567", genre, BigDecimal.valueOf(300), 10)));

        // Create orders for regularUser1: 1 PAID, 2 DELIVERED, 1 CANCELLED
        createTestOrder(regularUser1, book, OrderStatus.PAID);
        createTestOrder(regularUser1, book, OrderStatus.DELIVERED);
        createTestOrder(regularUser1, book, OrderStatus.DELIVERED);
        createTestOrder(regularUser1, book, OrderStatus.CANCELLED);

        mockMvc.perform(get("/api/manager/users/" + regularUser1.getId())
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(regularUser1.getId().intValue())))
                .andExpect(jsonPath("$.email", is("ivan.petrenko@example.com")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.bonusBalance", is(50)))
                .andExpect(jsonPath("$.totalOrders", is(4)))
                .andExpect(jsonPath("$.ordersByStatus.AWAITING_PAYMENT", is(0)))
                .andExpect(jsonPath("$.ordersByStatus.PAID", is(1)))
                .andExpect(jsonPath("$.ordersByStatus.DELIVERED", is(2)))
                .andExpect(jsonPath("$.ordersByStatus.CANCELLED", is(1)));

        // Non-existent user -> 404
        mockMvc.perform(get("/api/manager/users/999999")
                        .session(managerSession))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/manager/users/{id}/block: блокує активного користувача та надсилає сповіщення")
    void shouldBlockUserSuccessfully() throws Exception {
        MockHttpSession managerSession = loginAs("manager@readmania.ua", "ManagerPass123!");

        BlockUserRequest blockRequest = new BlockUserRequest("Неодноразова несплата замовлених книг");

        mockMvc.perform(post("/api/manager/users/" + regularUser1.getId() + "/block")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(regularUser1.getId().intValue())))
                .andExpect(jsonPath("$.status", is("BLOCKED")))
                .andExpect(jsonPath("$.blockReason", is("Неодноразова несплата замовлених книг")))
                .andExpect(jsonPath("$.blockedAt", notNullValue()));

        User updatedUser = userRepository.findById(regularUser1.getId()).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(updatedUser.getBlockReason()).isEqualTo("Неодноразова несплата замовлених книг");

        if (notificationService instanceof LoggingNotificationService loggingNotificationService) {
            assertThat(loggingNotificationService.getSentEmails())
                    .anyMatch(msg -> msg.recipient().equals("ivan.petrenko@example.com") && msg.subject().contains("заблоковано"));
        }

        // Try to block already blocked user -> 409 Conflict
        mockMvc.perform(post("/api/manager/users/" + regularUser1.getId() + "/block")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isConflict());

        // Try to block manager -> 409 Conflict
        mockMvc.perform(post("/api/manager/users/" + managerUser.getId() + "/block")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/manager/users/unblock-requests та POST /api/manager/users/unblock: перегляд і масове розблокування")
    void shouldListAndUnblockUsers() throws Exception {
        MockHttpSession managerSession = loginAs("manager@readmania.ua", "ManagerPass123!");

        // Create second blocked user with unblock request
        User blockedUser2 = new User("blocked2@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Другий Заблокований");
        blockedUser2.setStatus(UserStatus.BLOCKED);
        blockedUser2.setBlockReason("Підозріла активність");
        blockedUser2.setBlockedAt(Instant.now());
        blockedUser2 = userRepository.save(blockedUser2);

        UnblockRequest request1 = new UnblockRequest(blockedUser, "Прошу вибачення, більше не порушуватиму");
        unblockRequestRepository.save(request1);

        UnblockRequest request2 = new UnblockRequest(blockedUser2, "Це була помилка, зніміть блокування будь ласка");
        unblockRequestRepository.save(request2);

        // 1. Get unblock requests
        mockMvc.perform(get("/api/manager/users/unblock-requests")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[0].justification", notNullValue()));

        // 2. Unblock only blockedUser (leave blockedUser2 in requests)
        UnblockUsersRequest unblockRequest = new UnblockUsersRequest(List.of(blockedUser.getId()));

        mockMvc.perform(post("/api/manager/users/unblock")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unblockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Облікові записи успішно розблоковано")));

        // Verify blockedUser is ACTIVE and unblock_request was removed
        User unblocked = userRepository.findById(blockedUser.getId()).orElseThrow();
        assertThat(unblocked.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(unblocked.getBlockReason()).isNull();
        assertThat(unblocked.getBlockedAt()).isNull();

        if (notificationService instanceof LoggingNotificationService loggingNotificationService) {
            assertThat(loggingNotificationService.getSentEmails())
                    .anyMatch(msg -> msg.recipient().equals("blocked.user@example.com") && msg.subject().contains("розблоковано"));
        }

        // Check unblock requests list now contains only 1 remaining request for blockedUser2
        mockMvc.perform(get("/api/manager/users/unblock-requests")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(blockedUser2.getId().intValue())));
    }

    private Order createTestOrder(User user, Book book, OrderStatus status) {
        Order order = new Order(user, OrderType.REGULAR, PaymentMethod.TRANSFER, BigDecimal.valueOf(300), BigDecimal.ZERO, 0, BigDecimal.valueOf(300));
        order.setDeliveryAddress("м. Київ");
        order.setRecipientName("Тестовий Отримувач");
        order.setRecipientPhone("+380501112233");
        order.setStatus(status);

        OrderItem item = new OrderItem(order, book, book.getTitle(), BigDecimal.valueOf(300), 1);
        order.addItem(item);

        return orderRepository.save(order);
    }
}
