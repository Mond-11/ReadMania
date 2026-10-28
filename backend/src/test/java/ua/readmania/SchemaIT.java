package ua.readmania;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SchemaIT - Інтеграційні тести валідації схеми та обмежень БД на Testcontainers")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaIT extends BaseIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("""
            TRUNCATE TABLE 
                gift_log,
                challenge_completions,
                bonus_transactions,
                challenges,
                ratings,
                order_status_history,
                order_items,
                orders,
                cart_items,
                institutions,
                book_covers,
                book_authors,
                books,
                authors,
                genres,
                users
            CASCADE
        """).update();
    }

    @Test
    @DisplayName("D7/V2: Відхилення дублікатів email без урахування регістру")
    void shouldRejectDuplicateEmailCaseInsensitive() {
        jdbcClient.sql("INSERT INTO users (email, password_hash, role) VALUES (:email, :hash, 'USER')")
                .param("email", "reader@example.com")
                .param("hash", "$2a$10$hashedpassword")
                .update();

        assertThatThrownBy(() ->
                jdbcClient.sql("INSERT INTO users (email, password_hash, role) VALUES (:email, :hash, 'USER')")
                        .param("email", "READER@EXAMPLE.COM")
                        .param("hash", "$2a$10$hashedpassword2")
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("M1/V3: Частковий індекс ISBN — заборона дублікатів серед активних та дозвіл після soft-delete")
    void shouldRejectDuplicateIsbnForActiveBooksAndAllowAfterSoftDelete() {
        Long genreId = insertGenre("Класика");

        // Створення першої активної книги
        Long book1Id = insertBook("Кобзар", "978-966-03-7000-1", genreId, new BigDecimal("350.00"), 10);
        assertThat(book1Id).isNotNull();

        // Спроба додати другу активну книгу з тим самим ISBN повинна викликати помилку
        assertThatThrownBy(() ->
                insertBook("Кобзар Дублікат", "978-966-03-7000-1", genreId, new BigDecimal("400.00"), 5)
        ).isInstanceOf(DataIntegrityViolationException.class);

        // М'яке видалення першої книги
        jdbcClient.sql("UPDATE books SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("id", book1Id)
                .update();

        // Тепер створення нової активної книги з таким же ISBN дозволено
        Long book2Id = insertBook("Кобзар Нове видання", "978-966-03-7000-1", genreId, new BigDecimal("450.00"), 15);
        assertThat(book2Id).isNotNull();
    }

    @Test
    @DisplayName("V6: Обмеження оцінки книги значеннями від 1 до 5 (відхилення 0 та 6)")
    void shouldRejectInvalidRatingValues() {
        Long genreId = insertGenre("Фантастика");
        Long bookId = insertBook("1984", "978-966-03-7000-4", genreId, new BigDecimal("300.00"), 10);
        Long userId = insertUser("rater@example.com");

        // Відхилення оцінки 0
        assertThatThrownBy(() ->
                jdbcClient.sql("INSERT INTO ratings (user_id, book_id, value) VALUES (:userId, :bookId, 0)")
                        .param("userId", userId)
                        .param("bookId", bookId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Відхилення оцінки 6
        assertThatThrownBy(() ->
                jdbcClient.sql("INSERT INTO ratings (user_id, book_id, value) VALUES (:userId, :bookId, 6)")
                        .param("userId", userId)
                        .param("bookId", bookId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Валідна оцінка 5 успішно додається
        int rows = jdbcClient.sql("INSERT INTO ratings (user_id, book_id, value) VALUES (:userId, :bookId, 5)")
                .param("userId", userId)
                .param("bookId", bookId)
                .update();
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("V3: Відхилення від'ємного залишку на складі (stock >= 0)")
    void shouldRejectNegativeStock() {
        Long genreId = insertGenre("Детектив");

        assertThatThrownBy(() ->
                jdbcClient.sql("INSERT INTO books (title, isbn, genre_id, price, stock) VALUES ('Шерлок', '978-0-12345-0', :genreId, 200.00, -1)")
                        .param("genreId", genreId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V5: Відхилення знижки понад 30% від вартості товарів замовлення")
    void shouldRejectDiscountGreaterThan30Percent() {
        Long userId = insertUser("discount_user@example.com");

        // items_total = 100.00, points_used = 4 -> discount_amount = 40.00 (40 > 30% від 100.00)
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'PROCESSING', 'ON_DELIVERY', 100.00, 40.00, 4, 60.00)
                """)
                        .param("userId", userId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // items_total = 100.00, points_used = 3 -> discount_amount = 30.00 (рівно 30%) -> успіх
        Long orderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'PROCESSING', 'ON_DELIVERY', 100.00, 30.00, 3, 70.00)
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();

        assertThat(orderId).isNotNull();
    }

    @Test
    @DisplayName("M5/V5: Видалення користувача встановлює user_id = NULL в його замовленнях")
    void shouldSetOrderUserIdToNullWhenUserDeleted() {
        Long userId = insertUser("to_delete@example.com");

        Long orderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'PROCESSING', 'ON_DELIVERY', 200.00, 0.00, 0, 200.00)
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();

        // Видалення користувача
        jdbcClient.sql("DELETE FROM users WHERE id = :id").param("id", userId).update();

        // Перевірка, що замовлення збереглося, а user_id став NULL
        Map<String, Object> order = jdbcClient.sql("SELECT id, user_id FROM orders WHERE id = :id")
                .param("id", orderId)
                .query()
                .singleRow();

        assertThat(order.get("id")).isEqualTo(orderId);
        assertThat(order.get("user_id")).isNull();
    }

    @Test
    @DisplayName("V5: Видалення книги залишає позиції замовлення order_items зі знімком назви та ціни")
    void shouldSetOrderItemBookIdToNullWhenBookDeleted() {
        Long genreId = insertGenre("Драма");
        Long bookId = insertBook("Лісова пісня", "978-966-03-7000-2", genreId, new BigDecimal("250.00"), 10);
        Long userId = insertUser("buyer@example.com");

        Long orderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'PROCESSING', 'ON_DELIVERY', 250.00, 0.00, 0, 250.00)
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();

        Long itemId = jdbcClient.sql("""
                    INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
                    VALUES (:orderId, :bookId, 'Лісова пісня (Знімок)', 250.00, 1)
                    RETURNING id
                """)
                .param("orderId", orderId)
                .param("bookId", bookId)
                .query(Long.class)
                .single();

        // Фізичне видалення книги
        jdbcClient.sql("DELETE FROM books WHERE id = :id").param("id", bookId).update();

        // Перевірка: book_id став NULL, але знімок назви і ціни зберігся
        Map<String, Object> item = jdbcClient.sql("SELECT id, book_id, book_title, book_price FROM order_items WHERE id = :id")
                .param("id", itemId)
                .query()
                .singleRow();

        assertThat(item.get("book_id")).isNull();
        assertThat(item.get("book_title")).isEqualTo("Лісова пісня (Знімок)");
        assertThat(new BigDecimal(item.get("book_price").toString())).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("M1/V3: Відхилення видалення жанру, до якого належать книги (ON DELETE RESTRICT)")
    void shouldRejectGenreDeletionWhenBooksExist() {
        Long genreId = insertGenre("Нон-фікшн");
        insertBook("Sapiens", "978-966-03-7000-9", genreId, new BigDecimal("300.00"), 10);

        assertThatThrownBy(() ->
                jdbcClient.sql("DELETE FROM genres WHERE id = :id").param("id", genreId).update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V6: Заборона повторного нарахування бонусів типу ORDER_REWARD на одне й те саме замовлення")
    void shouldRejectDuplicateOrderRewardBonus() {
        Long userId = insertUser("bonus_recipient@example.com");
        Long orderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'DELIVERED', 'ON_DELIVERY', 500.00, 0.00, 0, 500.00)
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();

        // Перше нарахування балів за замовлення
        jdbcClient.sql("""
                    INSERT INTO bonus_transactions (user_id, points, type, order_id)
                    VALUES (:userId, 50, 'ORDER_REWARD', :orderId)
                """)
                .param("userId", userId)
                .param("orderId", orderId)
                .update();

        // Спроба повторного нарахування за те саме замовлення відхиляється унікальним індексом
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO bonus_transactions (user_id, points, type, order_id)
                    VALUES (:userId, 50, 'ORDER_REWARD', :orderId)
                """)
                        .param("userId", userId)
                        .param("orderId", orderId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V5: Для подарункового замовлення (type = 'GIFT') обов'язкова назва закладу institution_name")
    void shouldRequireInstitutionNameForGiftOrders() {
        Long userId = insertUser("gift_giver@example.com");

        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay, institution_name)
                    VALUES (:userId, 'GIFT', 'PROCESSING', 'TRANSFER', 300.00, 0.00, 0, 300.00, NULL)
                """)
                        .param("userId", userId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        Long giftOrderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay, institution_name)
                    VALUES (:userId, 'GIFT', 'PROCESSING', 'TRANSFER', 300.00, 0.00, 0, 300.00, 'Дитяча лікарня №1')
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();

        assertThat(giftOrderId).isNotNull();
    }

    // Допоміжні методи для підготовки тестових даних

    private Long insertUser(String email) {
        return jdbcClient.sql("INSERT INTO users (email, password_hash, role) VALUES (:email, '$2a$10$hashed', 'USER') RETURNING id")
                .param("email", email)
                .query(Long.class)
                .single();
    }

    private Long insertGenre(String name) {
        return jdbcClient.sql("INSERT INTO genres (name) VALUES (:name) RETURNING id")
                .param("name", name)
                .query(Long.class)
                .single();
    }

    private Long insertBook(String title, String isbn, Long genreId, BigDecimal price, int stock) {
        return jdbcClient.sql("""
                    INSERT INTO books (title, isbn, genre_id, price, stock)
                    VALUES (:title, :isbn, :genreId, :price, :stock)
                    RETURNING id
                """)
                .param("title", title)
                .param("isbn", isbn)
                .param("genreId", genreId)
                .param("price", price)
                .param("stock", stock)
                .query(Long.class)
                .single();
    }
}
