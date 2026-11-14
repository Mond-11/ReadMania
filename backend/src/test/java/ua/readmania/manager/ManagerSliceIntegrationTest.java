package ua.readmania.manager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.BaseIntegrationTest;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.gift.GiftLog;
import ua.readmania.domain.gift.GiftLogRepository;
import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionRepository;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.domain.loyalty.Rating;
import ua.readmania.domain.loyalty.RatingRepository;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.auth.CreateUnblockRequest;
import ua.readmania.dto.auth.LoginRequest;
import ua.readmania.dto.institution.DeleteInstitutionsRequest;
import ua.readmania.dto.manager.user.BlockUserRequest;
import ua.readmania.dto.manager.user.DeleteInactiveUsersRequest;
import ua.readmania.dto.manager.user.UnblockUsersRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ManagerSliceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private GiftLogRepository giftLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User manager;
    private User testUser;
    private Genre testGenre;
    private Book testBook;

    @BeforeEach
    void setUp() {
        manager = userRepository.findByEmailIgnoreCase("slice5.manager@example.com")
                .orElseGet(() -> userRepository.save(new User("slice5.manager@example.com", passwordEncoder.encode("ManagerPass123!"), Role.MANAGER, "Менеджер Слайсу")));

        testUser = userRepository.findByEmailIgnoreCase("slice5.user@example.com")
                .orElseGet(() -> userRepository.save(new User("slice5.user@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Користувач Слайсу")));

        testGenre = genreRepository.findByNameIgnoreCase("Фентезі Слайс")
                .orElseGet(() -> genreRepository.save(new Genre("Фентезі Слайс")));

        testBook = bookRepository.findByIsbn("978-617-12-9999-1")
                .orElseGet(() -> bookRepository.save(new Book("Володар Перснів", "978-617-12-9999-1", testGenre, BigDecimal.valueOf(450), 20, "А-БА-БА-ГА-ЛА-МА-ГА", 2023)));
    }

    private MockHttpSession loginAs(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    @Test
    @DisplayName("Сценарій 1: Блокування активного користувача -> 403 account-blocked -> апеляція -> розблокування менеджером")
    void testBlockUserImmediate403UnblockAppealAndRestoreAccess() throws Exception {
        // 1. Користувач логіниться і має доступ
        MockHttpSession userSession = loginAs("slice5.user@example.com", "UserPass123!");
        mockMvc.perform(get("/api/account").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("slice5.user@example.com"));

        // 2. Менеджер блокує користувача
        MockHttpSession managerSession = loginAs("slice5.manager@example.com", "ManagerPass123!");
        BlockUserRequest blockRequest = new BlockUserRequest("Систематичне спамування у відгуках");
        mockMvc.perform(post("/api/manager/users/" + testUser.getId() + "/block")
                        .session(managerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.blockReason").value("Систематичне спамування у відгуках"));

        // 3. У поточній відкритій сесії користувача наступний запит миттєво відхиляється з 403 Forbidden
        mockMvc.perform(get("/api/account").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:readmania:error:account-blocked"))
                .andExpect(jsonPath("$.blockReason").value("Систематичне спамування у відгуках"));

        // 4. Заблокований користувач подає апеляцію через публічний ендпоінт
        CreateUnblockRequest appeal = new CreateUnblockRequest(
                "slice5.user@example.com",
                "UserPass123!",
                "Прошу вибачення, це було непорозуміння, більше не повториться"
        );
        mockMvc.perform(post("/api/auth/unblock-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appeal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("успішно")));

        // 5. Менеджер переглядає чергу запитів на розблокування
        mockMvc.perform(get("/api/manager/users/unblock-requests")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("slice5.user@example.com"))
                .andExpect(jsonPath("$[0].justification").value("Прошу вибачення, це було непорозуміння, більше не повториться"));

        // 6. Менеджер пакетно розблоковує користувача
        UnblockUsersRequest unblockRequest = new UnblockUsersRequest(List.of(testUser.getId()));
        mockMvc.perform(post("/api/manager/users/unblock")
                        .session(managerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unblockRequest)))
                .andExpect(status().isOk());

        // 7. Користувач успішно логіниться і знову має доступ
        MockHttpSession newSession = loginAs("slice5.user@example.com", "UserPass123!");
        mockMvc.perform(get("/api/account").session(newSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Сценарій 2: Видалення неактивного користувача (>2 років) зберігає замовлення та оцінки для аналітики")
    void testInactiveUserDeletionPreservesOrdersAndRatingsAnalytics() throws Exception {
        // Створюємо користувача з last_login_at > 2 років тому
        User inactiveUser = new User("inactive.author@example.com", passwordEncoder.encode("Pass12345!"), Role.USER, "Давній Читач");
        inactiveUser.setCreatedAt(Instant.now().minus(800, ChronoUnit.DAYS));
        inactiveUser.setLastLoginAt(Instant.now().minus(750, ChronoUnit.DAYS));
        inactiveUser = userRepository.save(inactiveUser);

        // Створюємо замовлення та позицію
        Order order = new Order(inactiveUser, OrderType.REGULAR, PaymentMethod.TRANSFER, BigDecimal.valueOf(450), BigDecimal.ZERO, 0, BigDecimal.valueOf(450));
        order.setStatus(OrderStatus.DELIVERED);
        order.setRecipientName("Давній Читач");
        order.setRecipientPhone("+380991112233");
        order.setDeliveryAddress("вул. Симоненка, 5");
        OrderItem item = new OrderItem(order, testBook, testBook.getTitle(), BigDecimal.valueOf(450), 2);
        order.addItem(item);
        order = orderRepository.save(order);

        // Виставляємо оцінку
        Rating rating = new Rating(inactiveUser, testBook, order, (short) 5);
        ratingRepository.save(rating);

        Long inactiveUserId = inactiveUser.getId();
        Long orderId = order.getId();

        // Менеджер викликає видалення неактивних акаунтів
        MockHttpSession managerSession = loginAs("slice5.manager@example.com", "ManagerPass123!");
        DeleteInactiveUsersRequest deleteRequest = new DeleteInactiveUsersRequest(List.of(inactiveUserId));

        mockMvc.perform(delete("/api/manager/users/inactive")
                        .session(managerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk());

        // Перевіряємо, що запис користувача видалено з users
        assertThat(userRepository.findById(inactiveUserId)).isEmpty();

        // Перевіряємо, що замовлення збережено і анонімізовано
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getUser()).isNull();
        assertThat(updatedOrder.getRecipientName()).isEqualTo("Видалений користувач");
        assertThat(updatedOrder.getRecipientPhone()).isNull();
        assertThat(updatedOrder.getDeliveryAddress()).isNull();

        // Перевіряємо, що аналітика demand бачить це замовлення (2 примірники)
        mockMvc.perform(get("/api/manager/analytics/demand")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].orderedQuantity").value(2))
                .andExpect(jsonPath("$.byGenres[?(@.name == 'Фентезі Слайс')].orderedQuantity").value(2));

        // Перевіряємо, що аналітика ratings бачить оцінку 5.0
        mockMvc.perform(get("/api/manager/analytics/ratings")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].averageRating").value(5.0))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].ratingsCount").value(1))
                .andExpect(jsonPath("$.byGenres[?(@.name == 'Фентезі Слайс')].averageRating").value(5.0));
    }

    @Test
    @DisplayName("Сценарій 3: Заборона видалення закладу з активним замовленням (409) та видалення закладу із завершеним замовленням зі збереженням gift_log")
    void testInstitutionDeletionRestrictionsAndGiftLogRetention() throws Exception {
        MockHttpSession managerSession = loginAs("slice5.manager@example.com", "ManagerPass123!");

        // Заклад А: має активне замовлення
        Institution institutionA = institutionRepository.save(new Institution(
                "Інтернат №1", InstitutionType.BOARDING_SCHOOL, "м. Львів, вул. Личаківська, 10", "Директор Іван"
        ));
        Order activeOrder = new Order(testUser, OrderType.GIFT, PaymentMethod.TRANSFER, BigDecimal.valueOf(450), BigDecimal.ZERO, 0, BigDecimal.valueOf(450));
        activeOrder.setInstitution(institutionA);
        activeOrder.setInstitutionName(institutionA.getName());
        activeOrder.setInstitutionContactPerson(institutionA.getContactPerson());
        activeOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        orderRepository.save(activeOrder);

        // Спроба видалити заклад А -> 409 Conflict
        DeleteInstitutionsRequest deleteReqA = new DeleteInstitutionsRequest(List.of(institutionA.getId()));
        mockMvc.perform(delete("/api/manager/institutions")
                        .session(managerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReqA)))
                .andExpect(status().isConflict());

        assertThat(institutionRepository.existsById(institutionA.getId())).isTrue();

        // Заклад Б: має завершене замовлення та запис у gift_log
        Institution institutionB = institutionRepository.save(new Institution(
                "Дитячий будинок «Надія»", InstitutionType.ORPHANAGE, "м. Харків, вул. Сумська, 25", "Завідувач Олена"
        ));
        Order finishedOrder = new Order(testUser, OrderType.GIFT, PaymentMethod.TRANSFER, BigDecimal.valueOf(900), BigDecimal.ZERO, 0, BigDecimal.valueOf(900));
        finishedOrder.setInstitution(institutionB);
        finishedOrder.setInstitutionName(institutionB.getName());
        finishedOrder.setInstitutionContactPerson(institutionB.getContactPerson());
        finishedOrder.setStatus(OrderStatus.DELIVERED);
        OrderItem item = new OrderItem(finishedOrder, testBook, testBook.getTitle(), BigDecimal.valueOf(450), 2);
        finishedOrder.addItem(item);
        finishedOrder = orderRepository.save(finishedOrder);

        // Запис у gift_log
        GiftLog giftLog = new GiftLog(finishedOrder, institutionB, institutionB.getName(), testBook.getTitle(), 2, LocalDate.now());
        giftLogRepository.save(giftLog);

        Long instBId = institutionB.getId();
        Long finishedOrderId = finishedOrder.getId();

        // Видалення закладу Б -> 200 OK
        DeleteInstitutionsRequest deleteReqB = new DeleteInstitutionsRequest(List.of(instBId));
        mockMvc.perform(delete("/api/manager/institutions")
                        .session(managerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReqB)))
                .andExpect(status().isOk());

        // Заклад Б та його завершене замовлення видалені
        assertThat(institutionRepository.findById(instBId)).isEmpty();
        assertThat(orderRepository.findById(finishedOrderId)).isEmpty();

        // Запис у gift_log зберігся
        mockMvc.perform(get("/api/manager/gift-log")
                        .session(managerSession)
                        .param("institutionName", "Надія"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDonatedBooks").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].institutionName").value("Дитячий будинок «Надія»"))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Володар Перснів"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("Сценарій 4: Перевірка точності аналітики demand (ігнорування CANCELLED), stock та ratings (середнє арифметичне)")
    void testAnalyticsCalculationsAccuracy() throws Exception {
        MockHttpSession managerSession = loginAs("slice5.manager@example.com", "ManagerPass123!");

        // Створюємо другу книгу того ж жанру
        Book secondBook = bookRepository.save(new Book(
                "Гобіт", "978-617-12-9999-2", testGenre, BigDecimal.valueOf(300), 15, "А-БА-БА-ГА-ЛА-МА-ГА", 2022
        ));

        // 1. Demand: створюємо виконане замовлення (2 книги Володар Перснів + 3 книги Гобіт)
        Order paidOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, BigDecimal.valueOf(1800), BigDecimal.ZERO, 0, BigDecimal.valueOf(1800));
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.addItem(new OrderItem(paidOrder, testBook, testBook.getTitle(), BigDecimal.valueOf(450), 2));
        paidOrder.addItem(new OrderItem(paidOrder, secondBook, secondBook.getTitle(), BigDecimal.valueOf(300), 3));
        orderRepository.save(paidOrder);

        // Створюємо скасоване замовлення (10 книг Володар Перснів) - повинно бути проігнороване
        Order cancelledOrder = new Order(testUser, OrderType.REGULAR, PaymentMethod.TRANSFER, BigDecimal.valueOf(4500), BigDecimal.ZERO, 0, BigDecimal.valueOf(4500));
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.addItem(new OrderItem(cancelledOrder, testBook, testBook.getTitle(), BigDecimal.valueOf(450), 10));
        orderRepository.save(cancelledOrder);

        // Перевірка Demand
        mockMvc.perform(get("/api/manager/analytics/demand")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byGenres[?(@.name == 'Фентезі Слайс')].orderedQuantity").value(5))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].orderedQuantity").value(2))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Гобіт')].orderedQuantity").value(3));

        // 2. Stock: testBook має 20, secondBook має 15 -> Жанр має 35
        mockMvc.perform(get("/api/manager/analytics/stock")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byGenres[?(@.name == 'Фентезі Слайс')].stock").value(35))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].stock").value(20))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Гобіт')].stock").value(15));

        // 3. Ratings: Book 1 має оцінки 5 та 4 (середнє 4.5); Book 2 має оцінку 3 (середнє 3.0)
        // Середнє для жанру: (5 + 4 + 3) / 3 = 4.0; кількість оцінок = 3
        ratingRepository.save(new Rating(testUser, testBook, paidOrder, (short) 5));
        User secondUser = userRepository.save(new User("slice5.user2@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Другий Читач"));
        ratingRepository.save(new Rating(secondUser, testBook, paidOrder, (short) 4));
        ratingRepository.save(new Rating(testUser, secondBook, paidOrder, (short) 3));

        mockMvc.perform(get("/api/manager/analytics/ratings")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].averageRating").value(4.5))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Володар Перснів')].ratingsCount").value(2))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Гобіт')].averageRating").value(3.0))
                .andExpect(jsonPath("$.byBooks[?(@.name == 'Гобіт')].ratingsCount").value(1))
                .andExpect(jsonPath("$.byGenres[?(@.name == 'Фентезі Слайс')].averageRating").value(4.0))
                .andExpect(jsonPath("$.byGenres[?(@.name == 'Фентезі Слайс')].ratingsCount").value(3));
    }
}
