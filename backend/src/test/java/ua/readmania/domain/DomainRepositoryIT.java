package ua.readmania.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ua.readmania.BaseIntegrationTest;
import ua.readmania.domain.cart.CartItem;
import ua.readmania.domain.cart.CartItemId;
import ua.readmania.domain.cart.CartItemRepository;
import ua.readmania.domain.catalog.Author;
import ua.readmania.domain.catalog.AuthorRepository;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.catalog.BookSpecifications;
import ua.readmania.domain.catalog.Genre;
import ua.readmania.domain.catalog.GenreRepository;
import ua.readmania.domain.challenge.Challenge;
import ua.readmania.domain.challenge.ChallengeCompletion;
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
import ua.readmania.domain.order.OrderStatusHistory;
import ua.readmania.domain.order.OrderStatusHistoryRepository;
import ua.readmania.domain.order.OrderType;
import ua.readmania.domain.order.PaymentMethod;
import ua.readmania.domain.user.PasswordResetToken;
import ua.readmania.domain.user.PasswordResetTokenRepository;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.UnblockRequest;
import ua.readmania.domain.user.UnblockRequestRepository;
import ua.readmania.domain.user.UnblockRequestStatus;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;

class DomainRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UnblockRequestRepository unblockRequestRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCoverRepository bookCoverRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

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
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeCompletionRepository challengeCompletionRepository;

    @Autowired
    private GiftLogRepository giftLogRepository;

    @Test
    @DisplayName("User та пов'язані сутності (PasswordResetToken, UnblockRequest) зберігаються та коректно вибираються")
    void shouldSaveAndQueryUserWithTokensAndUnblockRequests() {
        User user = new User("reader@readmania.ua", "$2a$10$hashedpassword", Role.USER, "Тарас Шевченко");
        user.setPhone("+380501234567");
        user.setBonusBalance(100);
        user = userRepository.saveAndFlush(user);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userRepository.findByEmailIgnoreCase("READER@READMANIA.UA")).isPresent();

        PasswordResetToken token = new PasswordResetToken(user, "reset-token-xyz", Instant.now().plus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.saveAndFlush(token);

        assertThat(passwordResetTokenRepository.findByToken("reset-token-xyz")).isPresent();

        UnblockRequest request = new UnblockRequest(user, "Прошу розблокувати обліковий запис");
        unblockRequestRepository.saveAndFlush(request);

        List<UnblockRequest> pending = unblockRequestRepository.findByStatusOrderByCreatedAtDesc(UnblockRequestStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getUser().getEmail()).isEqualTo("reader@readmania.ua");
    }

    @Test
    @DisplayName("Catalog: Book, Genre, Author (3НФ), BookCover та оптимістичне блокування версії (@Version)")
    void shouldManageCatalogEntitiesWithOptimisticLockingAndCovers() {
        Genre fantasy = genreRepository.saveAndFlush(new Genre("Фентезі"));
        Author author1 = authorRepository.saveAndFlush(new Author("Анджей Сапковський"));
        Author author2 = authorRepository.saveAndFlush(new Author("Співавтор Тестовий"));

        Book book = new Book("Останнє бажання", "978-617-12-0001-1", fantasy, new BigDecimal("450.00"), 10);
        book.setAuthors(Set.of(author1, author2));
        book = bookRepository.saveAndFlush(book);

        assertThat(book.getId()).isNotNull();
        assertThat(book.getVersion()).isZero();

        // Окрема таблиця обкладинки
        BookCover cover = new BookCover(book, "image/jpeg", new byte[]{1, 2, 3, 4, 5});
        bookCoverRepository.saveAndFlush(cover);

        entityManager.clear();

        Book loaded = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(loaded.getAuthors()).hasSize(2);
        assertThat(loaded.getGenre().getName()).isEqualTo("Фентезі");

        // Оновлення залишку збільшує версію
        loaded.setStock(loaded.getStock() - 1);
        bookRepository.saveAndFlush(loaded);
        assertThat(loaded.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Catalog: М'яке видалення (@SQLRestriction) та JpaSpecificationExecutor з динамічною фільтрацією G1")
    void shouldFilterBooksDynamicallyAndExcludeSoftDeleted() {
        Genre sciFi = genreRepository.saveAndFlush(new Genre("Наукова фантастика"));
        Genre history = genreRepository.saveAndFlush(new Genre("Історія"));

        Author lem = authorRepository.saveAndFlush(new Author("Станіслав Лем"));
        Author asimov = authorRepository.saveAndFlush(new Author("Айзек Азімов"));

        Book book1 = new Book("Соляріс", "978-617-00-1111-1", sciFi, new BigDecimal("350.00"), 5);
        book1.addAuthor(lem);

        Book book2 = new Book("Фундація", "978-617-00-2222-2", sciFi, new BigDecimal("500.00"), 0);
        book2.addAuthor(asimov);

        Book book3 = new Book("Історія цивілізацій", "978-617-00-3333-3", history, new BigDecimal("600.00"), 12);
        book3.addAuthor(asimov);

        List<Book> savedBooks = bookRepository.saveAllAndFlush(List.of(book1, book2, book3));
        book1 = savedBooks.get(0);
        book2 = savedBooks.get(1);
        book3 = savedBooks.get(2);

        // 1. Пошук за назвою або автором
        Specification<Book> searchSpec = BookSpecifications.hasSearchQuery("Лем");
        Page<Book> searchResult = bookRepository.findAll(searchSpec, PageRequest.of(0, 10));
        assertThat(searchResult.getContent()).extracting(Book::getTitle).containsExactly("Соляріс");

        // 2. Фільтр за жанром + наявністю на складі
        Specification<Book> filterSpec = BookSpecifications.filter(null, sciFi.getId(), null, null, null, true);
        Page<Book> inStockSciFi = bookRepository.findAll(filterSpec, PageRequest.of(0, 10, Sort.by("price").ascending()));
        assertThat(inStockSciFi.getContent()).extracting(Book::getTitle).containsExactly("Соляріс");

        // 3. Фільтр за діапазоном ціни
        Specification<Book> priceSpec = BookSpecifications.filter(null, null, null, new BigDecimal("400.00"), new BigDecimal("650.00"), null);
        Page<Book> priceResult = bookRepository.findAll(priceSpec, PageRequest.of(0, 10, Sort.by("price").descending()));
        assertThat(priceResult.getContent()).extracting(Book::getTitle).containsExactly("Історія цивілізацій", "Фундація");

        // 4. М'яке видалення: помічена як видалена книга не повертається стандартними запитами
        book1.setDeletedAt(Instant.now());
        bookRepository.saveAndFlush(book1);

        entityManager.clear();

        assertThat(bookRepository.findById(book1.getId())).isEmpty();
        assertThat(bookRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("CartItem: збереження з композитним ключем @EmbeddedId та каскадні зв'язки")
    void shouldSaveAndQueryCartItems() {
        User user = userRepository.saveAndFlush(new User("buyer@readmania.ua", "hash", Role.USER, "Покупець"));
        Genre genre = genreRepository.saveAndFlush(new Genre("Детектив"));
        Book book = bookRepository.saveAndFlush(new Book("Шерлок Холмс", "978-617-11-1111-1", genre, new BigDecimal("300.00"), 10));

        CartItem item = new CartItem(user, book, 2);
        cartItemRepository.saveAndFlush(item);

        entityManager.clear();

        List<CartItem> userCart = cartItemRepository.findByUserId(user.getId());
        assertThat(userCart).hasSize(1);
        assertThat(userCart.get(0).getBook().getTitle()).isEqualTo("Шерлок Холмс");
        assertThat(userCart.get(0).getQuantity()).isEqualTo(2);

        cartItemRepository.deleteByUserIdAndBookId(user.getId(), book.getId());
        assertThat(cartItemRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("Order: замовлення, знімки позицій (OrderItem) та аудит зміни статусів (OrderStatusHistory)")
    void shouldSaveOrderWithItemSnapshotsAndStatusHistory() {
        User user = userRepository.saveAndFlush(new User("order_user@readmania.ua", "hash", Role.USER, "Замовник"));
        Institution orphanage = institutionRepository.saveAndFlush(
                new Institution("Дитячий будинок №1", InstitutionType.ORPHANAGE, "вул. Франка, 10", "Марія Петрівна", "+380441112233")
        );

        Genre genre = genreRepository.saveAndFlush(new Genre("Казки"));
        Book book = bookRepository.saveAndFlush(new Book("Казки народів світу", "978-617-22-2222-2", genre, new BigDecimal("250.00"), 50));

        Order order = new Order(user, OrderType.GIFT, PaymentMethod.TRANSFER, new BigDecimal("500.00"), BigDecimal.ZERO, 0, new BigDecimal("500.00"));
        order.setInstitution(orphanage);
        order.setInstitutionName(orphanage.getName());
        order.setStatus(OrderStatus.AWAITING_PAYMENT);

        OrderItem item = new OrderItem(order, book, book.getTitle(), book.getPrice(), 2);
        order.addItem(item);

        order = orderRepository.saveAndFlush(order);

        OrderStatusHistory history = new OrderStatusHistory(order, null, OrderStatus.AWAITING_PAYMENT, user, "Створено благодійне замовлення");
        orderStatusHistoryRepository.saveAndFlush(history);

        entityManager.clear();

        Order loaded = orderRepository.findWithDetailsById(order.getId()).orElseThrow();
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().get(0).getTitleSnapshot()).isEqualTo("Казки народів світу");
        assertThat(loaded.getItems().get(0).getPriceSnapshot()).isEqualByComparingTo("250.00");
        assertThat(loaded.getInstitution().getType()).isEqualTo(InstitutionType.ORPHANAGE);

        List<OrderStatusHistory> historyList = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertThat(historyList).hasSize(1);
        assertThat(historyList.get(0).getToStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }

    @Test
    @DisplayName("Loyalty, Challenge, ChallengeCompletion та GiftLog: повноцінне збереження та навігація зв'язків")
    void shouldManageLoyaltyChallengesAndGiftLog() {
        User user = userRepository.saveAndFlush(new User("loyalty_user@readmania.ua", "hash", Role.USER, "Учасник"));
        Genre genre = genreRepository.saveAndFlush(new Genre("Класика"));
        Book book = bookRepository.saveAndFlush(new Book("Кобзар", "978-617-33-3333-3", genre, new BigDecimal("400.00"), 20));

        Institution library = institutionRepository.saveAndFlush(
                new Institution("Міська бібліотека", InstitutionType.LIBRARY, "вул. Соборна, 5", "Анна Іванівна", "+380442223344")
        );

        Order order = new Order(user, OrderType.REGULAR, PaymentMethod.ON_DELIVERY, new BigDecimal("400.00"), BigDecimal.ZERO, 0, new BigDecimal("400.00"));
        order = orderRepository.saveAndFlush(order);

        // Бонусна транзакція
        BonusTransaction bonusTx = new BonusTransaction(user, 40, BonusTransactionType.ORDER_REWARD);
        bonusTx.setOrder(order);
        bonusTransactionRepository.saveAndFlush(bonusTx);

        // Оцінка
        Rating rating = new Rating(user, book, order, (short) 5);
        ratingRepository.saveAndFlush(rating);

        // Читацький виклик
        Challenge challenge = new Challenge("Прочитай 5 книг класики", "Осінній виклик", 5, genre, Instant.now().plus(30, ChronoUnit.DAYS), 150);
        challenge = challengeRepository.saveAndFlush(challenge);

        // Завершення виклику
        ChallengeCompletion completion = new ChallengeCompletion(user, challenge);
        challengeCompletionRepository.saveAndFlush(completion);

        // Журнал дарунків
        GiftLog giftLog = new GiftLog(order, library, library.getName(), book.getTitle(), 3, LocalDate.now());
        giftLogRepository.saveAndFlush(giftLog);

        entityManager.clear();

        assertThat(bonusTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(1);
        assertThat(ratingRepository.findAverageRatingByBookId(book.getId())).isEqualTo(5.0);
        assertThat(challengeCompletionRepository.existsByUserIdAndChallengeId(user.getId(), challenge.getId())).isTrue();
        assertThat(giftLogRepository.findByInstitutionIdOrderByDeliveredDateDesc(library.getId())).hasSize(1);
    }
}
