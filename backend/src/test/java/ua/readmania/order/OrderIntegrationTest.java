package ua.readmania.order;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.BaseIntegrationTest;
import ua.readmania.domain.cart.CartItemRepository;
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeCompletionRepository;
import ua.readmania.domain.challenge.ChallengeRepository;
import ua.readmania.domain.challenge.ChallengeStatus;
import ua.readmania.domain.gift.GiftLog;
import ua.readmania.domain.gift.GiftLogRepository;
import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionRepository;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.domain.loyalty.BonusTransaction;
import ua.readmania.domain.loyalty.BonusTransactionRepository;
import ua.readmania.domain.loyalty.BonusTransactionType;
import ua.readmania.domain.loyalty.Rating;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.DeliveryMethod;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderItemRepository;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderStatusHistoryRepository;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.cart.AddToCartRequest;
import ua.readmania.dto.cart.UpdateCartItemRequest;
import ua.readmania.dto.order.BatchOrderActionRequest;
import ua.readmania.dto.order.CreateGiftOrderRequest;
import ua.readmania.dto.order.CreateOrderRequest;
import ua.readmania.dto.order.OrderContactDto;
import ua.readmania.dto.order.OrderItemRequest;
import ua.readmania.dto.order.OrderPreviewRequest;
import ua.readmania.dto.order.UpdateOrderStatusRequest;

@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.simple.JdbcClient jdbcClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private BonusTransactionRepository bonusTransactionRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeCompletionRepository challengeCompletionRepository;

    @Autowired
    private GiftLogRepository giftLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ua.readmania.service.notification.LoggingNotificationService loggingNotificationService;

    private User testUser;
    private User managerUser;
    private Book testBook1;
    private Book testBook2;
    private Genre sciFiGenre;
    private Institution testInstitution;

    @BeforeEach
    void setUp() {
        loggingNotificationService.clear();
        jdbcClient.sql("""
            TRUNCATE TABLE 
                unblock_requests,
                password_reset_tokens,
                gift_log,
                challenge_completions,
                bonus_transactions,
                challenges,
                ratings,
                order_status_history,
                order_items,
                orders,
                cart_items,
                book_covers,
                book_authors,
                books,
                authors,
                genres,
                institutions,
                users
            CASCADE
        """).update();

        sciFiGenre = genreRepository.save(new Genre("Наукова фантастика"));
        Genre fantasyGenre = genreRepository.save(new Genre("Фентезі"));

        Author lem = authorRepository.save(new Author("Станіслав Лем"));
        Author tolkien = authorRepository.save(new Author("Дж. Р. Р. Толкін"));

        testBook1 = new Book(
                "Соляріс",
                "978-966-03-0001-1",
                new BigDecimal("300.00"),
                10,
                sciFiGenre,
                Set.of(lem)
        );
        testBook1.setDescription("Культовий роман");
        testBook1 = bookRepository.save(testBook1);

        testBook2 = new Book(
                "Гобіт",
                "978-966-03-0002-2",
                new BigDecimal("250.00"),
                5,
                fantasyGenre,
                Set.of(tolkien)
        );
        testBook2.setDescription("Казкова повість");
        testBook2 = bookRepository.save(testBook2);

        testInstitution = institutionRepository.save(new Institution(
                "Дитячий будинок 'Пролісок'",
                InstitutionType.ORPHANAGE,
                "м. Київ, вул. Лісова, 15",
                "Оксана Петрівна",
                "+380441112233"
        ));

        testUser = new User(
                "buyer@example.com",
                passwordEncoder.encode("Password123!"),
                Role.USER,
                "Іван Франко"
        );
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setPhone("+380501234567");
        testUser.setDeliveryAddress("м. Київ, вул. Хрещатик, 1, кв. 10");
        testUser.setBonusBalance(100);
        testUser = userRepository.save(testUser);

        managerUser = new User(
                "manager@example.com",
                passwordEncoder.encode("ManagerPass123!"),
                Role.MANAGER,
                "Олена Керівник"
        );
        managerUser.setStatus(UserStatus.ACTIVE);
        managerUser = userRepository.save(managerUser);
    }

    @Test
    @DisplayName("C1.1-C1.3: Керування кошиком, контроль наявності та прапорець available")
    @WithMockUser(username = "buyer@example.com", roles = {"USER"})
    void shouldManageCartItemsAndVerifyStockAndAvailability() throws Exception {
        // 1. Додавання 2 шт. Соляріс
        AddToCartRequest addReq = new AddToCartRequest(testBook1.getId(), 2);
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPrice").value(600.00))
                .andExpect(jsonPath("$.allAvailable").value(true))
                .andExpect(jsonPath("$.items[0].bookId").value(testBook1.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].available").value(true));

        // 2. Спроба додати більше, ніж залишок на складі (залишок 10, вже є 2, додаємо ще 9 -> разом 11 > 10)
        AddToCartRequest exceedReq = new AddToCartRequest(testBook1.getId(), 9);
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exceedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("перевищує наявний залишок")));

        // 3. Зміна кількості в кошику на 4 шт.
        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(4);
        mockMvc.perform(put("/api/cart/items/" + testBook1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.totalPrice").value(1200.00));

        // 4. Отримання кошика
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems").value(4));

        // 5. Видалення товару з кошика
        mockMvc.perform(delete("/api/cart/items/" + testBook1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("C8: Попередній розрахунок замовлення з обмеженням знижки бонусами до 30%")
    @WithMockUser(username = "buyer@example.com", roles = {"USER"})
    void shouldPreviewOrderWithBonusCalculation() throws Exception {
        // itemsTotal = 2 * 300 = 600 грн.
        // 30% від 600 грн = 180 грн -> максимум 18 балів (1 бал = 10 грн).
        // У користувача 100 балів, але запитуємо 50 -> має використатись лише 18 балів, знижка 180 грн, до сплати 420 грн.
        OrderPreviewRequest previewReq = new OrderPreviewRequest(
                List.of(new OrderItemRequest(testBook1.getId(), 2)),
                50
        );

        mockMvc.perform(post("/api/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(600.00))
                .andExpect(jsonPath("$.userBonusBalance").value(100))
                .andExpect(jsonPath("$.maxBonusPoints").value(18))
                .andExpect(jsonPath("$.pointsUsed").value(18))
                .andExpect(jsonPath("$.discountAmount").value(180.00))
                .andExpect(jsonPath("$.totalToPay").value(420.00))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("C2, C8: Оформлення звичайного замовлення зі списанням залишків, бонусів та фіксацією знімків")
    @WithMockUser(username = "buyer@example.com", roles = {"USER"})
    void shouldCreateRegularOrderAndDeductStockAndBonusAndClearCart() throws Exception {
        // Додаємо в кошик
        cartItemRepository.save(new ua.readmania.domain.cart.CartItem(testUser, testBook1, 2));

        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setDeliveryMethod(DeliveryMethod.COURIER);
        orderReq.setAddress("м. Київ, вул. Хрещатик, 1, кв. 10");
        orderReq.setContacts(new OrderContactDto("Іван Франко", "+380501234567"));
        orderReq.setPaymentMethod(PaymentMethod.TRANSFER);
        orderReq.setBonusPoints(10); // 10 балів = 100 грн знижки (100 <= 180 max)

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("REGULAR"))
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.itemsTotal").value(600.00))
                .andExpect(jsonPath("$.pointsUsed").value(10))
                .andExpect(jsonPath("$.discountAmount").value(100.00))
                .andExpect(jsonPath("$.totalToPay").value(500.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].titleSnapshot").value("Соляріс"))
                .andExpect(jsonPath("$.items[0].priceSnapshot").value(300.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn();

        // Перевіряємо списання залишку книги: 10 - 2 = 8
        Book updatedBook = bookRepository.findById(testBook1.getId()).orElseThrow();
        assertEquals(8, updatedBook.getStock());

        // Перевіряємо списання бонусів: 100 - 10 = 90
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(90, updatedUser.getBonusBalance());

        // Перевіряємо запис транзакції SPENT
        List<BonusTransaction> txs = bonusTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertEquals(1, txs.size());
        assertEquals(BonusTransactionType.SPENT, txs.get(0).getType());
        assertEquals(-10, txs.get(0).getPoints());

        // Перевіряємо очищення кошика
        assertTrue(cartItemRepository.findByUserId(testUser.getId()).isEmpty());

        // Перевіряємо надсилання сповіщення менеджерам
        assertFalse(loggingNotificationService.getSentEmails().isEmpty());
        assertEquals("managers@readmania.ua", loggingNotificationService.getLastSentEmail().recipient());
        assertTrue(loggingNotificationService.getLastSentEmail().subject().contains("Нове замовлення"));
    }

    @Test
    @DisplayName("C2: Оформлення благодійного замовлення (GIFT) з прив'язкою закладу та оплатою TRANSFER")
    @WithMockUser(username = "buyer@example.com", roles = {"USER"})
    void shouldCreateGiftOrderAndRequireTransferPaymentMethod() throws Exception {
        CreateGiftOrderRequest giftReq = new CreateGiftOrderRequest(
                testInstitution.getId(),
                List.of(new OrderItemRequest(testBook2.getId(), 3)),
                PaymentMethod.TRANSFER,
                0
        );

        mockMvc.perform(post("/api/orders/gift")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(giftReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GIFT"))
                .andExpect(jsonPath("$.institutionId").value(testInstitution.getId()))
                .andExpect(jsonPath("$.institutionName").value("Дитячий будинок 'Пролісок'"))
                .andExpect(jsonPath("$.institutionContactPerson").value(testInstitution.getContactPerson()))
                .andExpect(jsonPath("$.deliveryMethod").doesNotExist())
                .andExpect(jsonPath("$.itemsTotal").value(750.00))
                .andExpect(jsonPath("$.totalToPay").value(750.00));

        Book updatedBook = bookRepository.findById(testBook2.getId()).orElseThrow();
        assertEquals(2, updatedBook.getStock()); // 5 - 3 = 2
    }

    @Test
    @DisplayName("C3: Скасування замовлення покупцем (AWAITING_PAYMENT + TRANSFER) повертає залишки та бонуси")
    @WithMockUser(username = "buyer@example.com", roles = {"USER"})
    void shouldCancelOwnAwaitingPaymentOrderAndRestoreStockAndRefundBonus() throws Exception {
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setItems(List.of(new OrderItemRequest(testBook1.getId(), 2)));
        orderReq.setDeliveryMethod(DeliveryMethod.COURIER);
        orderReq.setAddress("м. Київ, вул. Хрещатик, 1");
        orderReq.setContacts(new OrderContactDto("Іван Франко", "+380501234567"));
        orderReq.setPaymentMethod(PaymentMethod.TRANSFER);
        orderReq.setBonusPoints(10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(responseJson).get("id").asLong();

        // Залишок 8, баланс 90
        assertEquals(8, bookRepository.findById(testBook1.getId()).orElseThrow().getStock());
        assertEquals(90, userRepository.findById(testUser.getId()).orElseThrow().getBonusBalance());

        // Скасування користувачем
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Залишок повернуто: 8 + 2 = 10
        assertEquals(10, bookRepository.findById(testBook1.getId()).orElseThrow().getStock());

        // Бонуси повернуто (REFUND): 90 + 10 = 100
        assertEquals(100, userRepository.findById(testUser.getId()).orElseThrow().getBonusBalance());

        List<BonusTransaction> refundTxs = bonusTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertTrue(refundTxs.stream().anyMatch(t -> t.getType() == BonusTransactionType.REFUND && t.getPoints() == 10));

        // Перевіряємо сповіщення про зміну статусу на CANCELLED
        assertFalse(loggingNotificationService.getSentEmails().isEmpty());
        assertEquals("buyer@example.com", loggingNotificationService.getLastSentEmail().recipient());
        assertTrue(loggingNotificationService.getLastSentEmail().subject().contains("CANCELLED"));
    }

    @Test
    @DisplayName("M3: Машина станів менеджера та захист від гонки з expectedStatus (409 Conflict)")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void shouldManageOrderStateMachineAndDetectConflict() throws Exception {
        // Створюємо замовлення
        Order order = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, new BigDecimal("600.00"), BigDecimal.ZERO, 0);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.addItem(new OrderItem(order, testBook1, "Соляріс", new BigDecimal("300.00"), 2));
        order = orderRepository.save(order);

        // 1. Успішний перехід менеджером: AWAITING_PAYMENT -> PAID
        UpdateOrderStatusRequest updateReq1 = new UpdateOrderStatusRequest(
                OrderStatus.AWAITING_PAYMENT,
                OrderStatus.PAID,
                "Оплату переказом підтверджено"
        );

        mockMvc.perform(patch("/api/manager/orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // 2. Інший менеджер пробує змінити, вважаючи, що статус все ще AWAITING_PAYMENT -> 409 Conflict
        UpdateOrderStatusRequest staleReq = new UpdateOrderStatusRequest(
                OrderStatus.AWAITING_PAYMENT,
                OrderStatus.PAID,
                "Спроба зміни застарілого статусу"
        );

        mockMvc.perform(patch("/api/manager/orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("не відповідає очікуваному")));

        // 3. Неприпустимий перехід (наприклад з AWAITING_PAYMENT у DELIVERED при оплаті TRANSFER) -> 409 Conflict
        Order transferOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, new BigDecimal("300.00"), BigDecimal.ZERO, 0);
        transferOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        transferOrder.addItem(new OrderItem(transferOrder, testBook1, "Соляріс", new BigDecimal("300.00"), 1));
        transferOrder = orderRepository.save(transferOrder);

        UpdateOrderStatusRequest invalidTransition = new UpdateOrderStatusRequest(
                OrderStatus.AWAITING_PAYMENT,
                OrderStatus.DELIVERED,
                "Стрибок статусу"
        );

        mockMvc.perform(patch("/api/manager/orders/" + transferOrder.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransition)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.detail", containsString("Неприпустимий перехід статусу")));

        // 4. Дозволений перехід: з AWAITING_PAYMENT у DELIVERED при оплаті ON_DELIVERY (CASH_ON_DELIVERY)
        Order codOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.ON_DELIVERY, new BigDecimal("300.00"), BigDecimal.ZERO, 0);
        codOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        codOrder.addItem(new OrderItem(codOrder, testBook1, "Соляріс", new BigDecimal("300.00"), 1));
        codOrder = orderRepository.save(codOrder);

        UpdateOrderStatusRequest codDeliveredReq = new UpdateOrderStatusRequest(
                OrderStatus.AWAITING_PAYMENT,
                OrderStatus.DELIVERED,
                "Оплачено і доставлено кур'єром"
        );

        mockMvc.perform(patch("/api/manager/orders/" + codOrder.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(codDeliveredReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @DisplayName("C7, C8, M9: Побічні дії при DELIVERED: нарахування бонусів, виконання викликів та gift_log")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void shouldExecuteSideEffectsWhenOrderBecomesDelivered() throws Exception {
        // Створюємо активний читацький виклик: прочитати 2 книги наукової фантастики, нагорода 50 балів
        Challenge sciFiChallenge = new Challenge(
                "Фантастичний марафон",
                "Прочитайте 2 книги жанру наукової фантастики",
                2,
                sciFiGenre,
                Instant.now().plus(30, ChronoUnit.DAYS),
                50
        );
        sciFiChallenge.setStatus(ChallengeStatus.ACTIVE);
        sciFiChallenge = challengeRepository.save(sciFiChallenge);

        // Благодійне замовлення на 600 грн (2 книги наукової фантастики)
        Order order = new Order(testUser, OrderType.GIFT, PaymentMethod.TRANSFER, new BigDecimal("600.00"), BigDecimal.ZERO, 0);
        order.setStatus(OrderStatus.PAID);
        order.setInstitution(testInstitution);
        order.setInstitutionName(testInstitution.getName());
        order.setInstitutionContactPerson(testInstitution.getContactPerson());
        order.addItem(new OrderItem(order, testBook1, "Соляріс", new BigDecimal("300.00"), 2));
        order = orderRepository.save(order);

        int initialBalance = testUser.getBonusBalance(); // 100

        // Менеджер переводить у DELIVERED
        UpdateOrderStatusRequest deliveredReq = new UpdateOrderStatusRequest(
                OrderStatus.PAID,
                OrderStatus.DELIVERED,
                "Замовлення доставлено в заклад"
        );

        mockMvc.perform(patch("/api/manager/orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveredReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // 1. Нарахування бонусів за замовлення: floor(600 / 100) = 6 балів
        // 2. Нарахування нагороди за виклик: 50 балів
        // Разом новий баланс: 100 + 6 + 50 = 156
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(initialBalance + 6 + 50, updatedUser.getBonusBalance());

        assertTrue(bonusTransactionRepository.findByOrderIdAndType(order.getId(), BonusTransactionType.ORDER_REWARD).isPresent());
        assertTrue(challengeCompletionRepository.existsByUserIdAndChallengeId(testUser.getId(), sciFiChallenge.getId()));

        // 3. Перевіряємо запис у gift_log
        List<GiftLog> giftLogs = giftLogRepository.findByOrderId(order.getId());
        assertEquals(1, giftLogs.size());
        assertEquals("Соляріс", giftLogs.get(0).getBookTitle());
        assertEquals(2, giftLogs.get(0).getQuantity());
        assertEquals("Дитячий будинок 'Пролісок'", giftLogs.get(0).getInstitutionName());
    }

    @Test
    @DisplayName("7.2.2.6, 7.2.2.7: Спеціальні дії менеджера: скасування прострочених (>7 днів) та очищення архіву (>3 років) за списком ID")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void shouldHandleOverdueAndArchivedOrders() throws Exception {
        // Замовлення, неоплачене 8 днів тому
        Order overdueOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, new BigDecimal("300.00"), BigDecimal.ZERO, 0);
        overdueOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        overdueOrder.setCreatedAt(Instant.now().minus(8, ChronoUnit.DAYS));
        overdueOrder.addItem(new OrderItem(overdueOrder, testBook1, "Соляріс", new BigDecimal("300.00"), 1));
        overdueOrder = orderRepository.save(overdueOrder);

        // Архівне замовлення 4 роки тому
        Order archivedOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.ON_DELIVERY, new BigDecimal("300.00"), BigDecimal.ZERO, 0);
        archivedOrder.setStatus(OrderStatus.DELIVERED);
        archivedOrder.setCreatedAt(Instant.now().minus(4 * 365, ChronoUnit.DAYS));
        archivedOrder.setStatusChangedAt(Instant.now().minus(4 * 365, ChronoUnit.DAYS));
        archivedOrder.addItem(new OrderItem(archivedOrder, testBook1, "Соляріс", new BigDecimal("300.00"), 1));
        archivedOrder = orderRepository.save(archivedOrder);

        // 1. Скасування прострочених за списком ID
        mockMvc.perform(post("/api/manager/orders/cancel-unpaid-overdue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchOrderActionRequest(List.of(overdueOrder.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledCount").value(1));

        Order checkedOverdue = orderRepository.findById(overdueOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, checkedOverdue.getStatus());

        // 2. Очищення архівних за списком ID
        mockMvc.perform(delete("/api/manager/orders/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchOrderActionRequest(List.of(archivedOrder.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1));

        assertFalse(orderRepository.existsById(archivedOrder.getId()));
    }

    @Test
    @DisplayName("Перевірка userRating у OrderItemResponse та конфлікту при скасуванні замовлення Користувачем")
    @WithMockUser(username = "buyer@example.com", roles = {"USER"})
    void shouldReturnUserRatingInOrderItemsAndConflictOnInvalidCancel() throws Exception {
        // Оцінюємо книгу testBook1 оцінкою 5
        ratingRepository.save(new Rating(testUser, testBook1, null, (short) 5));

        Order order = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, new BigDecimal("550.00"), BigDecimal.ZERO, 0);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.addItem(new OrderItem(order, testBook1, "Соляріс", new BigDecimal("300.00"), 1));
        order.addItem(new OrderItem(order, testBook2, "Гобіт", new BigDecimal("250.00"), 1));
        order = orderRepository.save(order);

        // Перевіряємо деталі замовлення: testBook1 має userRating = 5, testBook2 має userRating = null
        mockMvc.perform(get("/api/orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userRating").value(5))
                .andExpect(jsonPath("$.items[1].userRating").doesNotExist());

        // Змінюємо статус на PAID
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Спроба скасувати замовлення у статусі PAID користувачем -> 409 Conflict з currentStatus = PAID
        mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("PAID"));
    }

    @Test
    @DisplayName("Масові дії менеджера: валідація умов списку та повернення 409 Conflict при невідповідності")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void shouldReturn409WhenBatchActionItemsDoNotMatchCriteria() throws Exception {
        // Замовлення, створене 1 день тому (не є простроченим)
        Order recentOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, new BigDecimal("300.00"), BigDecimal.ZERO, 0);
        recentOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        recentOrder.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        recentOrder = orderRepository.save(recentOrder);

        // Спроба скасування непростроченого замовлення через cancel-unpaid-overdue -> 409 Conflict
        mockMvc.perform(post("/api/manager/orders/cancel-unpaid-overdue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchOrderActionRequest(List.of(recentOrder.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("не відповідає критеріям")));

        // Спроба архівування неархівного замовлення -> 409 Conflict
        mockMvc.perform(delete("/api/manager/orders/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchOrderActionRequest(List.of(recentOrder.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("не відповідає критеріям")));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("C2 Concurrency: Одночасне замовлення останнього примірника двома користувачами (Optimistic Locking -> 409 Conflict)")
    void shouldHandleConcurrentOrdersForLastBookCopyUsingOptimisticLocking() throws Exception {
        // Створюємо книгу з останнім примірником (stock = 1)
        Genre genre = genreRepository.findAll().get(0);
        Book lastCopyBook = new Book(
                "Унікальний раритет",
                "978-966-03-9999-9",
                new BigDecimal("500.00"),
                1,
                genre,
                Set.of()
        );
        lastCopyBook.setDescription("Останній примірник");
        lastCopyBook = bookRepository.save(lastCopyBook);

        User buyer2 = new User(
                "buyer2@example.com",
                passwordEncoder.encode("Password123!"),
                Role.USER,
                "Другий Покупець"
        );
        buyer2.setStatus(UserStatus.ACTIVE);
        final User buyer2Saved = userRepository.save(buyer2);
        final String buyer2Email = buyer2Saved.getEmail();
        final String buyer1Email = testUser.getEmail();

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setItems(List.of(new OrderItemRequest(lastCopyBook.getId(), 1)));
        orderReq.setDeliveryMethod(DeliveryMethod.COURIER);
        orderReq.setAddress("м. Київ, вул. Тестова, 1");
        orderReq.setContacts(new OrderContactDto("Тестер", "+380501234567"));
        orderReq.setPaymentMethod(PaymentMethod.TRANSFER);

        String reqJson = objectMapper.writeValueAsString(orderReq);

        Long bookId = lastCopyBook.getId();

        Runnable task1 = () -> {
            try {
                startLatch.await();
                MvcResult res = mockMvc.perform(post("/api/orders")
                                .header("X-Forwarded-User", buyer1Email)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(buyer1Email).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reqJson))
                        .andReturn();

                int status = res.getResponse().getStatus();
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                }
            } catch (Exception e) {
                // ignore
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                MvcResult res = mockMvc.perform(post("/api/orders")
                                .header("X-Forwarded-User", buyer2Email)
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(buyer2Email).roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reqJson))
                        .andReturn();

                int status = res.getResponse().getStatus();
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                }
            } catch (Exception e) {
                // ignore
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        startLatch.countDown(); // одночасний старт
        doneLatch.await();
        executor.shutdown();

        // Рівно одне замовлення повинно пройти успішно (201), а друге отримати 409 Conflict
        assertEquals(1, successCount.get(), "Одне замовлення має пройти успішно");
        assertEquals(1, conflictCount.get(), "Друге замовлення має бути відхилено з 409 Conflict");

        Book bookAfter = bookRepository.findById(bookId).orElseThrow();
        assertEquals(0, bookAfter.getStock(), "Залишок книги має стати 0");
    }
}
