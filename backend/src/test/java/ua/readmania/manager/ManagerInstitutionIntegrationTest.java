package ua.readmania.manager;

import java.math.BigDecimal;
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
import ua.readmania.domain.institution.Institution;
import ua.readmania.domain.institution.InstitutionRepository;
import ua.readmania.domain.institution.InstitutionType;
import ua.readmania.domain.order.Order;
import ua.readmania.domain.order.OrderItem;
import ua.readmania.domain.order.OrderRepository;
import ua.readmania.domain.order.OrderStatus;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.dto.auth.LoginRequest;
import ua.readmania.dto.institution.DeleteInstitutionsRequest;
import ua.readmania.dto.institution.InstitutionUpsertRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ManagerInstitutionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User managerUser;
    private User regularUser;
    private Institution institution1;
    private Institution institution2;
    private Institution institution3;
    private Book testBook;

    @BeforeEach
    void setUp() {
        Genre genre = genreRepository.findByNameIgnoreCase("Дитяча література")
                .orElseGet(() -> genreRepository.save(new Genre("Дитяча література")));
        testBook = bookRepository.findByIsbn("978-0-123456-48-9")
                .orElseGet(() -> bookRepository.save(new Book("Казки", "978-0-123456-48-9", genre, BigDecimal.valueOf(200), 20, "А-ба-ба-га-ла-ма-га", 2022)));

        managerUser = userRepository.findByEmailIgnoreCase("manager.inst.test@example.com")
                .orElseGet(() -> userRepository.save(new User("manager.inst.test@example.com", passwordEncoder.encode("ManagerPass123!"), Role.MANAGER, "Менеджер Заклади")));

        regularUser = userRepository.findByEmailIgnoreCase("user.inst.test@example.com")
                .orElseGet(() -> userRepository.save(new User("user.inst.test@example.com", passwordEncoder.encode("UserPass123!"), Role.USER, "Користувач Дар")));

        institution1 = institutionRepository.save(new Institution("Дитячий будинок 'Сонечко'", InstitutionType.ORPHANAGE, "вул. Лісова, 10, Київ", "Олена Петрівна"));
        institution2 = institutionRepository.save(new Institution("Інтернат №5", InstitutionType.BOARDING_SCHOOL, "вул. Шевченка, 25, Львів", "Іван Васильович"));
        institution3 = institutionRepository.save(new Institution("Міська бібліотека для юнацтва", InstitutionType.LIBRARY, "просп. Миру, 12, Одеса", "Марія Сергіївна"));
    }

    private MockHttpSession loginAsManager() throws Exception {
        LoginRequest loginRequest = new LoginRequest("manager.inst.test@example.com", "ManagerPass123!");
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("GET /api/institutions - публічний список закладів для благодійних замовлень")
    void testPublicGetInstitutions() throws Exception {
        mockMvc.perform(get("/api/institutions")
                        .param("type", "ORPHANAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Дитячий будинок \\'Сонечко\\'')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Дитячий будинок \\'Сонечко\\'')].type").value("ORPHANAGE"));
    }

    @Test
    @DisplayName("GET /api/manager/institutions - фільтрація за запитом та типом")
    void testManagerGetInstitutionsWithFilters() throws Exception {
        MockHttpSession session = loginAsManager();

        mockMvc.perform(get("/api/manager/institutions")
                        .session(session)
                        .param("query", "Шевченка, 25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Інтернат №5")))
                .andExpect(jsonPath("$[0].canDelete", is(true)))
                .andExpect(jsonPath("$[0].giftOrdersCount", is(0)));
    }

    @Test
    @DisplayName("GET /api/manager/institutions/{id} - повертає деталі та прапорець canDelete=false при наявності активних замовлень")
    void testManagerGetInstitutionDetails() throws Exception {
        MockHttpSession session = loginAsManager();

        // Створюємо активне замовлення в дар
        Order giftOrder = new Order(regularUser, OrderType.GIFT, PaymentMethod.TRANSFER, BigDecimal.valueOf(200), BigDecimal.ZERO, 0, BigDecimal.valueOf(200));
        giftOrder.setInstitution(institution1);
        giftOrder.setInstitutionName(institution1.getName());
        giftOrder.setInstitutionContactPerson(institution1.getContactPerson());
        giftOrder.setDeliveryAddress(institution1.getAddress());
        giftOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        OrderItem item = new OrderItem(giftOrder, testBook, testBook.getTitle(), BigDecimal.valueOf(200), 1);
        giftOrder.addItem(item);
        orderRepository.save(giftOrder);

        mockMvc.perform(get("/api/manager/institutions/" + institution1.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(institution1.getId().intValue())))
                .andExpect(jsonPath("$.giftOrdersCount", is(1)))
                .andExpect(jsonPath("$.canDelete", is(false)));
    }

    @Test
    @DisplayName("POST /api/manager/institutions та PUT /api/manager/institutions/{id} - створення та редагування закладу")
    void testCreateAndUpdateInstitution() throws Exception {
        MockHttpSession session = loginAsManager();

        InstitutionUpsertRequest createRequest = new InstitutionUpsertRequest(
                "Центральна дитяча бібліотека",
                InstitutionType.LIBRARY,
                "вул. Володимирська, 50, Київ",
                "Тетяна Іванівна"
        );

        String createResponse = mockMvc.perform(post("/api/manager/institutions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Центральна дитяча бібліотека")))
                .andExpect(jsonPath("$.type", is("LIBRARY")))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        InstitutionUpsertRequest updateRequest = new InstitutionUpsertRequest(
                "Центральна дитяча бібліотека (Оновлено)",
                InstitutionType.LIBRARY,
                "вул. Володимирська, 52, Київ",
                "Тетяна Миколаївна"
        );

        mockMvc.perform(put("/api/manager/institutions/" + createdId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Центральна дитяча бібліотека (Оновлено)")))
                .andExpect(jsonPath("$.contactPerson", is("Тетяна Миколаївна")));
    }

    @Test
    @DisplayName("DELETE /api/manager/institutions - відхилення 409 Conflict при наявності активних замовлень AWAITING_PAYMENT/PAID")
    void testDeleteInstitutions_ConflictWhenActiveOrdersExist() throws Exception {
        MockHttpSession session = loginAsManager();

        Order activeGiftOrder = new Order(regularUser, OrderType.GIFT, PaymentMethod.TRANSFER, BigDecimal.valueOf(200), BigDecimal.ZERO, 0, BigDecimal.valueOf(200));
        activeGiftOrder.setInstitution(institution2);
        activeGiftOrder.setInstitutionName(institution2.getName());
        activeGiftOrder.setInstitutionContactPerson(institution2.getContactPerson());
        activeGiftOrder.setStatus(OrderStatus.PAID);
        OrderItem item = new OrderItem(activeGiftOrder, testBook, testBook.getTitle(), BigDecimal.valueOf(200), 1);
        activeGiftOrder.addItem(item);
        orderRepository.save(activeGiftOrder);

        DeleteInstitutionsRequest request = new DeleteInstitutionsRequest(List.of(institution2.getId(), institution3.getId()));

        mockMvc.perform(delete("/api/manager/institutions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertThat(institutionRepository.existsById(institution2.getId())).isTrue();
        assertThat(institutionRepository.existsById(institution3.getId())).isTrue();
    }

    @Test
    @DisplayName("DELETE /api/manager/institutions - успішне видалення закладів без активних замовлень та очищення завершених замовлень")
    void testDeleteInstitutions_Success() throws Exception {
        MockHttpSession session = loginAsManager();

        Order deliveredGiftOrder = new Order(regularUser, OrderType.GIFT, PaymentMethod.TRANSFER, BigDecimal.valueOf(200), BigDecimal.ZERO, 0, BigDecimal.valueOf(200));
        deliveredGiftOrder.setInstitution(institution3);
        deliveredGiftOrder.setInstitutionName(institution3.getName());
        deliveredGiftOrder.setInstitutionContactPerson(institution3.getContactPerson());
        deliveredGiftOrder.setStatus(OrderStatus.DELIVERED);
        OrderItem item = new OrderItem(deliveredGiftOrder, testBook, testBook.getTitle(), BigDecimal.valueOf(200), 1);
        deliveredGiftOrder.addItem(item);
        deliveredGiftOrder = orderRepository.save(deliveredGiftOrder);

        Long orderId = deliveredGiftOrder.getId();
        Long instId = institution3.getId();

        DeleteInstitutionsRequest request = new DeleteInstitutionsRequest(List.of(instId));

        mockMvc.perform(delete("/api/manager/institutions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Заклади успішно видалено"));

        assertThat(institutionRepository.findById(instId)).isEmpty();
        assertThat(orderRepository.findById(orderId)).isEmpty();
    }
}
