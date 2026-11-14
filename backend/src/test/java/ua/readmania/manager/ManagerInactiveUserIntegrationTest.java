package ua.readmania.manager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import ua.readmania.domain.cart.CartItem;
import ua.readmania.domain.cart.CartItemRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.BonusTransactionType;
import ua.readmania.domain.loyalty.Rating;
import ua.readmania.domain.loyalty.RatingRepository;
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
import ua.readmania.dto.manager.user.DeleteInactiveUsersRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ManagerInactiveUserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BonusTransactionRepository bonusTransactionRepository;

    @Autowired
    private UnblockRequestRepository unblockRequestRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User managerUser;
    private User activeRecentUser;
    private User activeOldUserNoLogin;
    private User activeOldUserWithOldLogin;
    private User blockedOldUser;
    private User blockedRecentUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        Genre genre = genreRepository.findByNameIgnoreCase("Фантастика")
                .orElseGet(() -> genreRepository.save(new Genre("Фантастика")));
        testBook = bookRepository.findByIsbn("978-0-123456-47-2")
                .orElseGet(() -> bookRepository.save(new Book("Дюна", "978-0-123456-47-2", genre, BigDecimal.valueOf(350), 10, "Видавництво", 2023)));

        managerUser = userRepository.findByEmailIgnoreCase("manager.inactive.test@example.com")
                .orElseGet(() -> userRepository.save(new User("manager.inactive.test@example.com", passwordEncoder.encode("ManagerPass123!"), Role.MANAGER, "Менеджер Тест")));

        activeRecentUser = new User("active.recent@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Активний Нещодавній");
        activeRecentUser.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        activeRecentUser.setLastLoginAt(Instant.now().minus(1, ChronoUnit.DAYS));
        activeRecentUser = userRepository.save(activeRecentUser);

        activeOldUserNoLogin = new User("active.old.nologin@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Старий Без Входу");
        activeOldUserNoLogin.setCreatedAt(Instant.now().minus(800, ChronoUnit.DAYS));
        activeOldUserNoLogin.setLastLoginAt(null);
        activeOldUserNoLogin = userRepository.save(activeOldUserNoLogin);

        activeOldUserWithOldLogin = new User("active.old.login@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Старий Зі Старим Входом");
        activeOldUserWithOldLogin.setCreatedAt(Instant.now().minus(900, ChronoUnit.DAYS));
        activeOldUserWithOldLogin.setLastLoginAt(Instant.now().minus(750, ChronoUnit.DAYS));
        activeOldUserWithOldLogin = userRepository.save(activeOldUserWithOldLogin);

        blockedOldUser = new User("blocked.old@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Заблокований Давно");
        blockedOldUser.setStatus(UserStatus.BLOCKED);
        blockedOldUser.setBlockedAt(Instant.now().minus(800, ChronoUnit.DAYS));
        blockedOldUser.setBlockReason("Порушення");
        blockedOldUser = userRepository.save(blockedOldUser);

        blockedRecentUser = new User("blocked.recent@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Заблокований Нещодавно");
        blockedRecentUser.setStatus(UserStatus.BLOCKED);
        blockedRecentUser.setBlockedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        blockedRecentUser.setBlockReason("Порушення");
        blockedRecentUser = userRepository.save(blockedRecentUser);
    }

    private MockHttpSession loginAsManager() throws Exception {
        LoginRequest loginRequest = new LoginRequest("manager.inactive.test@example.com", "ManagerPass123!");
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("GET /api/manager/users/inactive - повертає лише неактивних понад 2 роки користувачів")
    void testGetInactiveUsers() throws Exception {
        MockHttpSession session = loginAsManager();

        mockMvc.perform(get("/api/manager/users/inactive")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.email == 'active.old.nologin@example.com')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'active.old.login@example.com')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'blocked.old@example.com')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'active.recent@example.com')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.email == 'blocked.recent@example.com')]").doesNotExist());
    }

    @Test
    @DisplayName("DELETE /api/manager/users/inactive - повертає 409 Conflict якщо користувач не відповідає критерію неактивності")
    void testDeleteInactiveUsers_ConflictOnActiveRecentUser() throws Exception {
        MockHttpSession session = loginAsManager();

        DeleteInactiveUsersRequest request = new DeleteInactiveUsersRequest(List.of(
                activeOldUserNoLogin.getId(),
                activeRecentUser.getId() // Не підходить
        ));

        mockMvc.perform(delete("/api/manager/users/inactive")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Перевіряємо, що транзакція відкотилася і нікого не видалено
        assertThat(userRepository.existsById(activeOldUserNoLogin.getId())).isTrue();
        assertThat(userRepository.existsById(activeRecentUser.getId())).isTrue();
    }

    @Test
    @DisplayName("DELETE /api/manager/users/inactive - успішне видалення з анонімізацією замовлень та оцінок")
    void testDeleteInactiveUsers_SuccessWithAnonymization() throws Exception {
        MockHttpSession session = loginAsManager();

        // Створюємо замовлення для activeOldUserNoLogin
        Order order = new Order(activeOldUserNoLogin, OrderType.REGULAR, PaymentMethod.TRANSFER, BigDecimal.valueOf(350), BigDecimal.ZERO, 0, BigDecimal.valueOf(350));
        order.setRecipientName("Іван Іванов");
        order.setRecipientPhone("+380501112233");
        order.setDeliveryAddress("вул. Хрещатик, 1");
        order.setStatus(OrderStatus.DELIVERED);
        OrderItem item = new OrderItem(order, testBook, testBook.getTitle(), BigDecimal.valueOf(350), 1);
        order.addItem(item);
        order = orderRepository.save(order);

        // Оцінка
        Rating rating = new Rating(activeOldUserNoLogin, testBook, order, (short) 5);
        rating = ratingRepository.save(rating);

        // Бонусна транзакція
        BonusTransaction tx = new BonusTransaction(activeOldUserNoLogin, 50, BonusTransactionType.ORDER_REWARD, order, null, null);
        bonusTransactionRepository.save(tx);

        // Кошик
        CartItem cartItem = new CartItem(activeOldUserNoLogin, testBook, 2);
        cartItemRepository.save(cartItem);

        // Unblock request для blockedOldUser
        UnblockRequest unblockRequest = new UnblockRequest(blockedOldUser, "Прошу розблокувати");
        unblockRequestRepository.save(unblockRequest);

        Long oldUserId1 = activeOldUserNoLogin.getId();
        Long oldUserId2 = blockedOldUser.getId();
        Long orderId = order.getId();
        Long ratingId = rating.getId();

        DeleteInactiveUsersRequest request = new DeleteInactiveUsersRequest(List.of(oldUserId1, oldUserId2));

        mockMvc.perform(delete("/api/manager/users/inactive")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Неактивні облікові записи успішно видалено"));

        entityManager.clear();

        // Перевірка: користувачів видалено з users
        assertThat(userRepository.findById(oldUserId1)).isEmpty();
        assertThat(userRepository.findById(oldUserId2)).isEmpty();

        // Перевірка: замовлення анонімізовано
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getUser()).isNull();
        assertThat(updatedOrder.getRecipientName()).isEqualTo("Видалений користувач");
        assertThat(updatedOrder.getRecipientPhone()).isNull();
        assertThat(updatedOrder.getDeliveryAddress()).isNull();
        assertThat(updatedOrder.getItems()).hasSize(1);

        // Перевірка: оцінка відв'язана від користувача, але залишилася
        Rating updatedRating = ratingRepository.findById(ratingId).orElseThrow();
        assertThat(updatedRating.getUser()).isNull();
        assertThat(updatedRating.getBook().getId()).isEqualTo(testBook.getId());

        // Перевірка: кошик, бонуси, запити на розблокування видалено
        assertThat(cartItemRepository.findByUserId(oldUserId1)).isEmpty();
        assertThat(bonusTransactionRepository.findByUserIdOrderByCreatedAtDesc(oldUserId1)).isEmpty();
        assertThat(unblockRequestRepository.findByUserIdOrderByCreatedAtDesc(oldUserId2)).isEmpty();
    }
}
