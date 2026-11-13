package ua.readmania.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.BaseIntegrationTest;
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.loyalty.Rating;
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
import ua.readmania.dto.catalog.CreateBookRequest;
import ua.readmania.dto.catalog.GenreRequest;
import ua.readmania.dto.catalog.UpdateBookRequest;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CatalogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookCoverRepository bookCoverRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Genre testGenre;
    private Author testAuthor;
    private Book testBook;
    private User testUser;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        ratingRepository.deleteAll();
        bookCoverRepository.deleteAll();
        bookRepository.deleteAll();
        genreRepository.deleteAll();
        authorRepository.deleteAll();
        userRepository.deleteAll();

        testGenre = genreRepository.save(new Genre("Фантастика"));
        testAuthor = authorRepository.save(new Author("Станіслав Лем"));
        testBook = bookRepository.save(new Book("Соляріс", "978-966-03-0001-1", new BigDecimal("320.00"), 10, testGenre, Set.of(testAuthor)));
        testUser = userRepository.save(new User("reader@example.com", "hash", Role.USER, "Тестовий Читач"));
    }

    // =========================================================================
    // 1. Публічний каталог (G1)
    // =========================================================================

    @Test
    @DisplayName("G1: Отримання списку книг з пагінацією та фільтрацією")
    void shouldGetBooksWithPaginationAndFilters() throws Exception {
        Author secondAuthor = authorRepository.save(new Author("Джордж Орвелл"));
        Genre classicGenre = genreRepository.save(new Genre("Класика"));
        bookRepository.save(new Book("1984", "978-966-03-0002-2", new BigDecimal("250.00"), 5, classicGenre, Set.of(secondAuthor)));

        // Filter by genre
        mockMvc.perform(get("/api/catalog/books")
                        .param("genre", "Фантастика")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Соляріс"))
                .andExpect(jsonPath("$.content[0].genre.name").value("Фантастика"));

        // Filter by price range
        mockMvc.perform(get("/api/catalog/books")
                        .param("priceMin", "200.00")
                        .param("priceMax", "300.00")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("1984"));

        // Search query
        mockMvc.perform(get("/api/catalog/books")
                        .param("query", "Лем"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Соляріс"));
    }

    @Test
    @DisplayName("G1: Отримання деталей книги з агрегованою оцінкою та статусом обкладинки")
    void shouldGetBookDetailsWithRatings() throws Exception {
        ratingRepository.save(new Rating(testUser, testBook, null, (short) 5));

        mockMvc.perform(get("/api/catalog/books/" + testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBook.getId()))
                .andExpect(jsonPath("$.title").value("Соляріс"))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.ratingsCount").value(1))
                .andExpect(jsonPath("$.hasCover").value(false));
    }

    @Test
    @DisplayName("G1: Запит неіснуючої книги повертає 404 Problem Details")
    void shouldReturn404WhenBookNotFound() throws Exception {
        mockMvc.perform(get("/api/catalog/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Ресурс не знайдено"));
    }

    @Test
    @DisplayName("G1: Отримання та віддача обкладинки з правильним Content-Type та кешуванням")
    void shouldGetBookCoverWithCacheHeaders() throws Exception {
        byte[] dummyImage = new byte[]{1, 2, 3, 4, 5};
        bookCoverRepository.save(new BookCover(testBook, "image/png", dummyImage));

        mockMvc.perform(get("/api/catalog/books/" + testBook.getId() + "/cover"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(content().bytes(dummyImage));
    }

    @Test
    @DisplayName("G1: Некоректні параметри пагінації чи ціни повертають 400 Problem Details")
    void shouldReturn400ForInvalidQueryParams() throws Exception {
        mockMvc.perform(get("/api/catalog/books")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/catalog/books")
                        .param("priceMin", "500.00")
                        .param("priceMax", "100.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("Мінімальна ціна не може бути більшою")));
    }

    @Test
    @DisplayName("G1: Отримання списку жанрів та авторів")
    void shouldGetGenresAndAuthors() throws Exception {
        mockMvc.perform(get("/api/catalog/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Фантастика"));

        mockMvc.perform(get("/api/catalog/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Станіслав Лем"));
    }

    // =========================================================================
    // 2. Керування каталогом для Менеджера (M1.1, M1.2, M1.3)
    // =========================================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1.1: Менеджер успішно додає нову книгу (201 Created)")
    void shouldCreateBookAsManager() throws Exception {
        CreateBookRequest request = new CreateBookRequest(
                "Кібернетика",
                "978-966-03-5555-5",
                new BigDecimal("450.00"),
                15,
                "Опис книги",
                testGenre.getId(),
                List.of("Норберт Вінер")
        );

        mockMvc.perform(post("/api/manager/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Кібернетика"))
                .andExpect(jsonPath("$.isbn").value("978-966-03-5555-5"))
                .andExpect(jsonPath("$.price").value(450.00))
                .andExpect(jsonPath("$.authors[0].name").value("Норберт Вінер"));

        assertTrue(bookRepository.existsActiveByIsbn("978-966-03-5555-5"));
    }

    @Test
    @DisplayName("M1: Анонім отримує 401, а USER отримує 403 на /api/manager/**")
    void shouldEnforceSecurityOnManagerEndpoints() throws Exception {
        CreateBookRequest request = new CreateBookRequest(
                "Тест", "978-966-03-9999-9", new BigDecimal("100.00"), 5, null, testGenre.getId(), List.of("Автор")
        );

        // Anonymous -> 401 Problem Details
        mockMvc.perform(post("/api/manager/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")));

        // USER -> 403 Problem Details
        mockMvc.perform(post("/api/manager/books")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1.1: Спроба створення книги з дублюванням активного ISBN повертає 409 Conflict")
    void shouldReturn409OnDuplicateIsbn() throws Exception {
        CreateBookRequest request = new CreateBookRequest(
                "Дублікат",
                "978-966-03-0001-1", // Same as testBook
                new BigDecimal("200.00"),
                5,
                null,
                testGenre.getId(),
                List.of("Автор")
        );

        mockMvc.perform(post("/api/manager/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail", containsString("вже існує")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1.2: Менеджер оновлює дані книги (200 OK)")
    void shouldUpdateBookAsManager() throws Exception {
        UpdateBookRequest request = new UpdateBookRequest(
                "Соляріс (Оновлене видання)",
                "978-966-03-0001-1",
                new BigDecimal("399.00"),
                20,
                "Повне зібрання творів",
                testGenre.getId(),
                List.of("Станіслав Лем")
        );

        mockMvc.perform(put("/api/manager/books/" + testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Соляріс (Оновлене видання)"))
                .andExpect(jsonPath("$.price").value(399.00))
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1.3: М'яке видалення відхиляється, якщо stock > 0 (400 Bad Request)")
    void shouldRejectDeleteIfStockGreaterThanZero() throws Exception {
        // testBook has stock = 10
        mockMvc.perform(delete("/api/manager/books/" + testBook.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("ненульовим залишком")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1.3: М'яке видалення відхиляється, якщо книга фігурує в активному замовленні (409 Conflict)")
    void shouldRejectDeleteIfBookInActiveOrder() throws Exception {
        testBook.setStock(0);
        bookRepository.save(testBook);

        Order order = new Order(
                testUser, OrderType.REGULAR, PaymentMethod.TRANSFER,
                new BigDecimal("320.00"), BigDecimal.ZERO, 0, new BigDecimal("320.00")
        );
        order.setDeliveryMethod(DeliveryMethod.COURIER);
        order.setRecipientName("Отримувач");
        order.setRecipientPhone("+380501112233");
        order.setDeliveryAddress("Київ");
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setItemsTotal(new BigDecimal("320.00"));
        order.setTotalToPay(new BigDecimal("320.00"));
        order = orderRepository.save(order);

        OrderItem orderItem = new OrderItem(order, testBook, "Соляріс", new BigDecimal("320.00"), 1);
        orderItemRepository.save(orderItem);

        mockMvc.perform(delete("/api/manager/books/" + testBook.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail", containsString("фігурує в активних замовленнях")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1.3: Успішне м'яке видалення книги при stock = 0 та відсутності активних замовлень (204 No Content)")
    void shouldSoftDeleteBookSuccessfully() throws Exception {
        testBook.setStock(0);
        bookRepository.save(testBook);

        mockMvc.perform(delete("/api/manager/books/" + testBook.getId()))
                .andExpect(status().isNoContent());

        // Book is not visible in public search
        mockMvc.perform(get("/api/catalog/books/" + testBook.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1: Створення, оновлення та спроба видалення жанру з прив'язаними книгами (409 Conflict)")
    void shouldManageGenresAndEnforceRestrictOnDelete() throws Exception {
        // Create genre
        mockMvc.perform(post("/api/manager/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenreRequest("Детектив"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Детектив"));

        // Delete testGenre which has testBook -> 409 Conflict (RESTRICT)
        mockMvc.perform(delete("/api/manager/genres/" + testGenre.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail", containsString("прив'язані книги")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M1: Завантаження обкладинки для книги (200 OK)")
    void shouldUploadBookCover() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", new byte[]{10, 20, 30, 40}
        );

        mockMvc.perform(multipart("/api/manager/books/" + testBook.getId() + "/cover")
                        .file(file))
                .andExpect(status().isOk());

        assertTrue(bookCoverRepository.existsById(testBook.getId()));
    }

    // =========================================================================
    // 3. CSV Імпорт (M2)
    // =========================================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("M2: Імпорт книг із CSV з покроковою валідацією, створенням авторів/жанрів та звітом")
    void shouldImportBooksFromCsvWithValidationReport() throws Exception {
        String csvContent = """
                title,isbn,price,stock,description,genre,authors
                Мистецтво війни,978-966-03-7777-1,280.00,10,Трактат про військову стратегію,Філософія,Сунь-цзи
                Дублікат у файлі,978-966-03-7777-1,150.00,5,Тест дубліката,Філософія,Сунь-цзи
                Існуючий у БД,978-966-03-0001-1,300.00,5,Тест дубля БД,Фантастика,Станіслав Лем
                Битий рядок з від'ємною ціною,978-966-03-8888-2,-50.00,2,Опис,Філософія,Автор
                """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "books.csv", "text/csv", csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/manager/books/import-csv")
                        .file(csvFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.addedCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(3))
                .andExpect(jsonPath("$.rejectedRows", hasSize(3)))
                .andExpect(jsonPath("$.rejectedRows[0].reason", containsString("Дублікат ISBN у файлі")))
                .andExpect(jsonPath("$.rejectedRows[1].reason", containsString("вже існує в базі даних")))
                .andExpect(jsonPath("$.rejectedRows[2].reason", containsString("Ціна не може бути від'ємною")));

        assertTrue(bookRepository.existsActiveByIsbn("978-966-03-7777-1"));
        assertTrue(genreRepository.existsByNameIgnoreCase("Філософія"));
        assertTrue(authorRepository.existsByNameIgnoreCase("Сунь-цзи"));
    }
}
