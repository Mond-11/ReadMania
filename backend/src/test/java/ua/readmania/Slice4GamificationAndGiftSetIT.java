package ua.readmania;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
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
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeCompletion;
import ua.readmania.domain.challenge.ChallengeCompletionRepository;
import ua.readmania.domain.challenge.ChallengeRepository;
import ua.readmania.domain.challenge.ChallengeStatus;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.BonusTransactionType;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.DeliveryMethod;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderItemRepository;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.auth.LoginRequest;
import ua.readmania.dto.challenge.CreateChallengeRequest;
import ua.readmania.dto.challenge.UpdateChallengeRequest;
import ua.readmania.dto.giftset.GenerateGiftSetRequest;
import ua.readmania.dto.giftset.GiftSetBookItem;
import ua.readmania.dto.giftset.GiftSetResponse;
import ua.readmania.dto.giftset.ReplaceGiftSetBookRequest;
import ua.readmania.dto.rating.CreateRatingRequest;

@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
public class Slice4GamificationAndGiftSetIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.simple.JdbcClient jdbcClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private BonusTransactionRepository bonusTransactionRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeCompletionRepository challengeCompletionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ua.readmania.service.notification.LoggingNotificationService loggingNotificationService;

    private User testUser;
    private User managerUser;
    private Genre fictionGenre;
    private Genre fantasyGenre;
    private Author testAuthor;
    private Book book1;
    private Book book2;
    private Book book3;

    @BeforeEach
    void setUp() {
        loggingNotificationService.clear();
        jdbcClient.sql("TRUNCATE TABLE bonus_transactions, ratings, challenge_completions, challenges, order_status_history, order_items, gift_log, orders, cart_items, book_authors, book_covers, books, authors, genres, password_reset_tokens, unblock_requests, users CASCADE").update();

        testUser = new User();
        testUser.setEmail("reader@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser.setFullName("Тарас Читач");
        testUser.setRole(Role.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setBonusBalance(10);
        testUser = userRepository.save(testUser);

        managerUser = new User();
        managerUser.setEmail("manager_gamification@example.com");
        managerUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        managerUser.setFullName("Головний Менеджер");
        managerUser.setRole(Role.MANAGER);
        managerUser.setStatus(UserStatus.ACTIVE);
        managerUser = userRepository.save(managerUser);

        fictionGenre = genreRepository.save(new Genre("Художня література"));
        fantasyGenre = genreRepository.save(new Genre("Фентезі"));

        testAuthor = authorRepository.save(new Author("Сергій Жадан"));

        book1 = new Book("Інтернат", "978-617-679-456-1", fictionGenre, new BigDecimal("250.00"), 10);
        book1.setDescription("Роман про війну");
        book1.setAuthors(Set.of(testAuthor));
        book1 = bookRepository.save(book1);

        book2 = new Book("Ворошиловград", "978-617-679-456-2", fictionGenre, new BigDecimal("300.00"), 15);
        book2.setDescription("Культовий роман");
        book2.setAuthors(Set.of(testAuthor));
        book2 = bookRepository.save(book2);

        book3 = new Book("Месопотамія", "978-617-679-456-3", fantasyGenre, new BigDecimal("150.00"), 5);
        book3.setDescription("Збірка оповідань");
        book3.setAuthors(Set.of(testAuthor));
        book3 = bookRepository.save(book3);
    }

    private MockHttpSession loginAs(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    // ==========================================
    // C6: Оцінювання видань (RatingService)
    // ==========================================

    @Test
    @DisplayName("C6: Успішне оцінювання книги з отриманого замовлення (DELIVERED) із нарахуванням +1 бонусного бала")
    void shouldAddRatingAndAwardBonusPointForDeliveredOrderBook() throws Exception {
        MockHttpSession session = loginAs("reader@example.com", "Password123!");

        // Створюємо доставлене замовлення для цього користувача
        Order order = new Order();
        order.setUser(testUser);
        order.setType(OrderType.REGULAR);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setPaymentMethod(PaymentMethod.ON_DELIVERY);
        order.setItemsTotal(new BigDecimal("250.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setTotalToPay(new BigDecimal("250.00"));
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);

        OrderItem item = new OrderItem(order, book1, book1.getTitle(), book1.getPrice(), 1);
        orderItemRepository.save(item);

        CreateRatingRequest ratingRequest = new CreateRatingRequest(book1.getId(), order.getId(), 5);

        mockMvc.perform(post("/api/ratings")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId", is(book1.getId().intValue())))
                .andExpect(jsonPath("$.value", is(5)));

        // Перевіряємо баланс бонусів (+1 бал)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getBonusBalance()).isEqualTo(11);

        // Перевіряємо запис у журналі бонусів
        boolean txExists = bonusTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()).stream()
                .anyMatch(tx -> tx.getType() == BonusTransactionType.RATING_REWARD && tx.getPoints() == 1);
        assertThat(txExists).isTrue();
    }

    @Test
    @DisplayName("C6: Помилка 400 при спробі оцінити книгу без доставленого замовлення")
    void shouldFailRatingWhenOrderIsNotDeliveredOrUserDidNotBuyBook() throws Exception {
        MockHttpSession session = loginAs("reader@example.com", "Password123!");

        // Замовлення ще у статусі AWAITING_PAYMENT
        Order order = new Order();
        order.setUser(testUser);
        order.setType(OrderType.REGULAR);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setPaymentMethod(PaymentMethod.ON_DELIVERY);
        order.setItemsTotal(new BigDecimal("250.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setTotalToPay(new BigDecimal("250.00"));
        order = orderRepository.save(order);

        OrderItem item = new OrderItem(order, book1, book1.getTitle(), book1.getPrice(), 1);
        orderItemRepository.save(item);

        CreateRatingRequest ratingRequest = new CreateRatingRequest(book1.getId(), order.getId(), 4);

        mockMvc.perform(post("/api/ratings")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("C6: Помилка 409 Conflict при повторній спробі оцінити ту саму книгу")
    void shouldFailRatingWhenAlreadyRatedBook() throws Exception {
        MockHttpSession session = loginAs("reader@example.com", "Password123!");

        Order order = new Order();
        order.setUser(testUser);
        order.setType(OrderType.REGULAR);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setPaymentMethod(PaymentMethod.ON_DELIVERY);
        order.setItemsTotal(new BigDecimal("250.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setTotalToPay(new BigDecimal("250.00"));
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);

        OrderItem item = new OrderItem(order, book1, book1.getTitle(), book1.getPrice(), 1);
        orderItemRepository.save(item);

        CreateRatingRequest ratingRequest = new CreateRatingRequest(book1.getId(), null, 5);

        // Перша оцінка - успіх
        mockMvc.perform(post("/api/ratings")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isCreated());

        // Друга оцінка - 409 Conflict
        mockMvc.perform(post("/api/ratings")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("C6: Перегляд власних оцінок та публічних оцінок книги")
    void shouldAllowViewingOfBookRatingsAndUserRatings() throws Exception {
        MockHttpSession session = loginAs("reader@example.com", "Password123!");

        Order order = new Order();
        order.setUser(testUser);
        order.setType(OrderType.REGULAR);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setPaymentMethod(PaymentMethod.ON_DELIVERY);
        order.setItemsTotal(new BigDecimal("250.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setTotalToPay(new BigDecimal("250.00"));
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);

        orderItemRepository.save(new OrderItem(order, book1, book1.getTitle(), book1.getPrice(), 1));

        CreateRatingRequest ratingRequest = new CreateRatingRequest(book1.getId(), null, 5);
        mockMvc.perform(post("/api/ratings")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isCreated());

        // Перегляд власних оцінок
        mockMvc.perform(get("/api/ratings/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookTitle", is("Інтернат")));

        // Публічний перегляд оцінок книги (без витоку userId та персональних даних)
        mockMvc.perform(get("/api/ratings/book/" + book1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].value", is(5)))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].userFullName").doesNotExist());
    }

    // ==========================================
    // C7, M4: Читацькі виклики (ChallengeService)
    // ==========================================

    @Test
    @DisplayName("M4.1: Створення читацького виклику менеджером та 403 для звичайного користувача")
    void shouldCreateChallengeByManagerAndForbidUser() throws Exception {
        MockHttpSession managerSession = loginAs("manager_gamification@example.com", "Password123!");
        MockHttpSession userSession = loginAs("reader@example.com", "Password123!");

        CreateChallengeRequest request = new CreateChallengeRequest(
                "Зимовий книжковий марафон",
                "Прочитайте 3 книги художньої літератури",
                3,
                fictionGenre.getId(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                50
        );

        // Звичайний користувач отримує 403
        mockMvc.perform(post("/api/manager/challenges")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Менеджер успішно створює (201 Created)
        mockMvc.perform(post("/api/manager/challenges")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Зимовий книжковий марафон")))
                .andExpect(jsonPath("$.requiredBooks", is(3)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("M4.2: Редагування виклику менеджером та автоматичний перерахунок прогресу користувача")
    void shouldUpdateChallengeAndRecalculateProgressAutomatically() throws Exception {
        MockHttpSession managerSession = loginAs("manager_gamification@example.com", "Password123!");
        MockHttpSession userSession = loginAs("reader@example.com", "Password123!");

        Challenge challenge = new Challenge(
                "Осінній виклик",
                "Опис",
                5,
                fictionGenre,
                Instant.now().plus(10, ChronoUnit.DAYS),
                30
        );
        challenge.setStatus(ChallengeStatus.ACTIVE);
        challenge = challengeRepository.save(challenge);

        // Користувач має доставлене замовлення на 2 книги
        Order order = new Order();
        order.setUser(testUser);
        order.setType(OrderType.REGULAR);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setPaymentMethod(PaymentMethod.ON_DELIVERY);
        order.setItemsTotal(new BigDecimal("550.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setTotalToPay(new BigDecimal("550.00"));
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);

        orderItemRepository.save(new OrderItem(order, book1, book1.getTitle(), book1.getPrice(), 1));
        orderItemRepository.save(new OrderItem(order, book2, book2.getTitle(), book2.getPrice(), 1));

        // Перевіряємо прогрес користувача (2 з 5)
        mockMvc.perform(get("/api/challenges/" + challenge.getId()).session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userProgress", is(2)))
                .andExpect(jsonPath("$.requiredBooks", is(5)));

        // Менеджер змінює умову на 2 книги
        UpdateChallengeRequest updateRequest = new UpdateChallengeRequest(
                "Осінній виклик (оновлено)",
                "Опис новий",
                2,
                fictionGenre.getId(),
                Instant.now().plus(15, ChronoUnit.DAYS),
                40
        );

        mockMvc.perform(put("/api/manager/challenges/" + challenge.getId())
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredBooks", is(2)));

        // Прогрес користувача динамічно відображається відносно нової кількості
        mockMvc.perform(get("/api/challenges/" + challenge.getId()).session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userProgress", is(2)))
                .andExpect(jsonPath("$.requiredBooks", is(2)));
    }

    @Test
    @DisplayName("M4.3: Дострокове завершення виклику менеджером та фіксація сповіщень")
    void shouldFinishChallengeEarlyAndNotifyUsersWithProgress() throws Exception {
        MockHttpSession managerSession = loginAs("manager_gamification@example.com", "Password123!");

        Challenge challenge = new Challenge(
                "Марафон читання",
                "Опис",
                5,
                null,
                Instant.now().plus(10, ChronoUnit.DAYS),
                30
        );
        challenge.setStatus(ChallengeStatus.ACTIVE);
        challenge = challengeRepository.save(challenge);

        // Додаємо доставлене замовлення для користувача
        Order order = new Order();
        order.setUser(testUser);
        order.setType(OrderType.REGULAR);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setPaymentMethod(PaymentMethod.ON_DELIVERY);
        order.setItemsTotal(new BigDecimal("250.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPointsUsed(0);
        order.setTotalToPay(new BigDecimal("250.00"));
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);
        orderItemRepository.save(new OrderItem(order, book1, book1.getTitle(), book1.getPrice(), 1));

        // Дострокове завершення виклику
        mockMvc.perform(post("/api/manager/challenges/" + challenge.getId() + "/finish")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.notifiedUsersCount", is(1)));

        assertThat(loggingNotificationService.getSentEmails()).hasSize(1);
        assertThat(loggingNotificationService.getLastSentEmail().recipient()).isEqualTo("reader@example.com");
        assertThat(loggingNotificationService.getLastSentEmail().subject()).contains("завершено достроково");

        // Повторна спроба редагування неактивного виклику повинна повернути 400
        UpdateChallengeRequest updateRequest = new UpdateChallengeRequest(
                "Спроба оновити",
                "Опис",
                3,
                null,
                Instant.now().plus(10, ChronoUnit.DAYS),
                20
        );
        mockMvc.perform(put("/api/manager/challenges/" + challenge.getId())
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C7: Користувач бачить виконані виклики, навіть якщо строк виклику вже минув")
    void shouldListCompletedChallengesThatAreNoLongerActive() throws Exception {
        MockHttpSession userSession = loginAs("reader@example.com", "Password123!");

        Challenge expired = new Challenge(
                "Літній марафон",
                "Опис",
                1,
                null,
                Instant.now().plus(1, ChronoUnit.DAYS),
                15
        );
        expired.setStatus(ChallengeStatus.EXPIRED);
        expired = challengeRepository.save(expired);
        challengeCompletionRepository.save(new ChallengeCompletion(testUser, expired, Instant.now()));

        mockMvc.perform(get("/api/challenges").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Літній марафон')].completed").value(true))
                .andExpect(jsonPath("$[?(@.name == 'Літній марафон')].status").value("EXPIRED"));

        // Гість бачить лише активні виклики
        mockMvc.perform(get("/api/challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Літній марафон')]").isEmpty());
    }

    // ==========================================
    // C9: Генератор подарункового набору (GiftSetService)
    // ==========================================

    @Test
    @DisplayName("C9: Успішна автогенерація подарункового набору під вказаний бюджет (кратний 50 грн)")
    void shouldGenerateGiftSetSuccessfullyWithinBudget() throws Exception {
        GenerateGiftSetRequest request = new GenerateGiftSetRequest(
                new BigDecimal("550.00"),
                List.of(fictionGenre.getId(), fantasyGenre.getId()),
                2
        );

        mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.budget", is(550.00)))
                .andExpect(jsonPath("$.remainingBudget").isNumber());
    }

    @Test
    @DisplayName("C9: Помилка валідації 400, якщо бюджет не є кратним 50 грн")
    void shouldFailGiftSetGenerationWhenBudgetNotMultipleOf50() throws Exception {
        GenerateGiftSetRequest request = new GenerateGiftSetRequest(
                new BigDecimal("325.00"),
                List.of(fictionGenre.getId()),
                2
        );

        mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("кратним 50 грн")));
    }

    @Test
    @DisplayName("C9: Відмова через причину 1 — Недостатньо книг у наявності за обраними жанрами")
    void shouldFailGiftSetGenerationWhenInsufficientBooksInGenres() throws Exception {
        GenerateGiftSetRequest request = new GenerateGiftSetRequest(
                new BigDecimal("500.00"),
                List.of(fantasyGenre.getId()), // Тільки 1 книга в наявності (book3)
                3                              // Запитуємо 3 книги
        );

        mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Недостатньо книг у наявності")));
    }

    @Test
    @DisplayName("C9: Відмова через причину 2 — Вказаного бюджету недостатньо за обраними критеріями")
    void shouldFailGiftSetGenerationWhenBudgetTooLow() throws Exception {
        GenerateGiftSetRequest request = new GenerateGiftSetRequest(
                new BigDecimal("50.00"), // Бюджет 50 грн
                List.of(fictionGenre.getId()), // Найдешевша книга коштує 250 грн
                1
        );

        mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("недостатньо для формування подарункового набору")));
    }

    @Test
    @DisplayName("C9: Заміна книги в наборі на альтернативну в межах залишкового бюджету")
    void shouldReplaceBookInGiftSetWithinBudget() throws Exception {
        ReplaceGiftSetBookRequest replaceRequest = new ReplaceGiftSetBookRequest(
                List.of(book1.getId(), book3.getId()),
                book3.getId(),
                List.of(fictionGenre.getId(), fantasyGenre.getId()),
                new BigDecimal("550.00")
        );

        // Перевіряємо кандидатів на заміну
        mockMvc.perform(post("/api/gift-sets/replace-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replaceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates", hasSize(1)))
                .andExpect(jsonPath("$.candidates[0].id", is(book2.getId().intValue())));

        // Виконуємо заміну
        mockMvc.perform(post("/api/gift-sets/replace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replaceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalPrice", is(550.00)))
                .andExpect(jsonPath("$.remainingBudget", is(0.00)));
    }

    @Test
    @DisplayName("C9: Повторна генерація з excludeBookIds повертає відмінний набір книг (bookCount = 1 та bookCount = 2)")
    void shouldRegenerateDifferentGiftSetWhenExcludeBookIdsProvided() throws Exception {
        // Тест для bookCount = 1
        GenerateGiftSetRequest firstRequest1 = new GenerateGiftSetRequest(
                new BigDecimal("700.00"),
                List.of(fictionGenre.getId(), fantasyGenre.getId()),
                1
        );

        var firstResult1 = mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn();

        GiftSetResponse firstSet1 = objectMapper.readValue(firstResult1.getResponse().getContentAsString(), GiftSetResponse.class);
        List<Long> firstSet1Ids = firstSet1.getItems().stream().map(GiftSetBookItem::getId).toList();

        GenerateGiftSetRequest secondRequest1 = new GenerateGiftSetRequest(
                new BigDecimal("700.00"),
                List.of(fictionGenre.getId(), fantasyGenre.getId()),
                1,
                firstSet1Ids
        );

        var secondResult1 = mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn();

        GiftSetResponse secondSet1 = objectMapper.readValue(secondResult1.getResponse().getContentAsString(), GiftSetResponse.class);
        List<Long> secondSet1Ids = secondSet1.getItems().stream().map(GiftSetBookItem::getId).toList();

        assertThat(secondSet1Ids).isNotEqualTo(firstSet1Ids);

        // Тест для bookCount = 2
        GenerateGiftSetRequest firstRequest2 = new GenerateGiftSetRequest(
                new BigDecimal("700.00"),
                List.of(fictionGenre.getId(), fantasyGenre.getId()),
                2
        );

        var firstResult2 = mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();

        GiftSetResponse firstSet2 = objectMapper.readValue(firstResult2.getResponse().getContentAsString(), GiftSetResponse.class);
        List<Long> firstSet2Ids = firstSet2.getItems().stream().map(GiftSetBookItem::getId).toList();

        GenerateGiftSetRequest secondRequest2 = new GenerateGiftSetRequest(
                new BigDecimal("700.00"),
                List.of(fictionGenre.getId(), fantasyGenre.getId()),
                2,
                firstSet2Ids
        );

        var secondResult2 = mockMvc.perform(post("/api/gift-sets/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();

        GiftSetResponse secondSet2 = objectMapper.readValue(secondResult2.getResponse().getContentAsString(), GiftSetResponse.class);
        List<Long> secondSet2Ids = secondSet2.getItems().stream().map(GiftSetBookItem::getId).toList();

        assertThat(secondSet2Ids).isNotEqualTo(firstSet2Ids);
    }
}
