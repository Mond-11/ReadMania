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
                    VALUES (:userId, 'REGULAR', 'PAID', 'ON_DELIVERY', 100.00, 40.00, 4, 60.00)
                """)
                        .param("userId", userId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // items_total = 100.00, points_used = 3 -> discount_amount = 30.00 (рівно 30%) -> успіх
        Long orderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'PAID', 'ON_DELIVERY', 100.00, 30.00, 3, 70.00)
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
                    VALUES (:userId, 'REGULAR', 'PAID', 'ON_DELIVERY', 200.00, 0.00, 0, 200.00)
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
                    VALUES (:userId, 'REGULAR', 'PAID', 'ON_DELIVERY', 250.00, 0.00, 0, 250.00)
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
    @DisplayName("V5/V9: Для подарункового замовлення (type = 'GIFT') обов'язкові institution_name та institution_contact_person")
    void shouldRequireInstitutionNameForGiftOrders() {
        Long userId = insertUser("gift_giver@example.com");

        // Відхилення без назви закладу
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay, institution_name, institution_contact_person)
                    VALUES (:userId, 'GIFT', 'PAID', 'TRANSFER', 300.00, 0.00, 0, 300.00, NULL, 'Олена Ковальчук')
                """)
                        .param("userId", userId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Відхилення без контактної особи
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay, institution_name, institution_contact_person)
                    VALUES (:userId, 'GIFT', 'PAID', 'TRANSFER', 300.00, 0.00, 0, 300.00, 'Дитяча лікарня №1', NULL)
                """)
                        .param("userId", userId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        Long giftOrderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay, institution_name, institution_contact_person)
                    VALUES (:userId, 'GIFT', 'PAID', 'TRANSFER', 300.00, 0.00, 0, 300.00, 'Дитяча лікарня №1', 'Олена Ковальчук')
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();

        assertThat(giftOrderId).isNotNull();
    }

    @Test
    @DisplayName("G3/V9: Захист від brute-force — збереження кількості невдалих спроб входу та часу блокування")
    void shouldSupportBruteForceTrackingFields() {
        Long userId = insertUser("bruteforce_test@example.com");

        int updated = jdbcClient.sql("""
                    UPDATE users 
                    SET failed_login_attempts = 5, locked_until = CURRENT_TIMESTAMP + INTERVAL '15 minutes'
                    WHERE id = :id
                """)
                .param("id", userId)
                .update();
        assertThat(updated).isEqualTo(1);

        var userRow = jdbcClient.sql("SELECT failed_login_attempts, locked_until FROM users WHERE id = :id")
                .param("id", userId)
                .query()
                .singleRow();

        assertThat(userRow.get("failed_login_attempts")).isEqualTo(5);
        assertThat(userRow.get("locked_until")).isNotNull();
    }

    @Test
    @DisplayName("G3/V9: Одноразові токени скидання пароля — унікальність, термін дії та каскадне видалення")
    void shouldHandlePasswordResetTokensWithCascadeAndUniqueness() {
        Long userId = insertUser("reset_test@example.com");

        Long tokenId = jdbcClient.sql("""
                    INSERT INTO password_reset_tokens (user_id, token, expires_at)
                    VALUES (:userId, 'unique-token-12345', CURRENT_TIMESTAMP + INTERVAL '1 hour')
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();
        assertThat(tokenId).isNotNull();

        // Повторне використання того самого токену відхиляється унікальним індексом
        Long user2Id = insertUser("reset_test2@example.com");
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO password_reset_tokens (user_id, token, expires_at)
                    VALUES (:userId, 'unique-token-12345', CURRENT_TIMESTAMP + INTERVAL '1 hour')
                """)
                        .param("userId", user2Id)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Каскадне видалення токенів при видаленні користувача
        jdbcClient.sql("DELETE FROM users WHERE id = :id").param("id", userId).update();
        int tokenCount = jdbcClient.sql("SELECT count(*) FROM password_reset_tokens WHERE id = :id")
                .param("id", tokenId)
                .query(Integer.class)
                .single();
        assertThat(tokenCount).isZero();
    }

    @Test
    @DisplayName("M7/V9: Запити на розблокування — статуси PENDING/APPROVED/REJECTED, менеджер та каскад")
    void shouldHandleUnblockRequestsAndPreserveOnManagerDelete() {
        Long userId = insertUser("blocked_user@example.com");
        Long managerId = insertUser("manager_admin@example.com");

        Long requestId = jdbcClient.sql("""
                    INSERT INTO unblock_requests (user_id, reason, status)
                    VALUES (:userId, 'Помилкове блокування, прошу розблокувати', 'PENDING')
                    RETURNING id
                """)
                .param("userId", userId)
                .query(Long.class)
                .single();
        assertThat(requestId).isNotNull();

        // Спроба встановити невалідний статус відхиляється
        assertThatThrownBy(() ->
                jdbcClient.sql("UPDATE unblock_requests SET status = 'INVALID_STATUS' WHERE id = :id")
                        .param("id", requestId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Менеджер погоджує запит
        jdbcClient.sql("""
                    UPDATE unblock_requests 
                    SET status = 'APPROVED', resolved_by_user_id = :managerId, resolved_at = CURRENT_TIMESTAMP, manager_comment = 'Розблоковано'
                    WHERE id = :id
                """)
                .param("managerId", managerId)
                .param("id", requestId)
                .update();

        // При видаленні менеджера запит зберігається (resolved_by_user_id стає NULL завдяки ON DELETE SET NULL)
        jdbcClient.sql("DELETE FROM users WHERE id = :id").param("id", managerId).update();
        var req = jdbcClient.sql("SELECT status, resolved_by_user_id FROM unblock_requests WHERE id = :id")
                .param("id", requestId)
                .query()
                .singleRow();
        assertThat(req.get("status")).isEqualTo("APPROVED");
        assertThat(req.get("resolved_by_user_id")).isNull();
    }

    @Test
    @DisplayName("M8/M12/V9: Типи благодійних закладів — валідні значення ORPHANAGE, BOARDING_SCHOOL, LIBRARY")
    void shouldEnforceInstitutionTypesAndAllowValidValues() {
        // Успішне додавання з кожним валідним типом
        for (String type : new String[]{"ORPHANAGE", "BOARDING_SCHOOL", "LIBRARY"}) {
            Long instId = jdbcClient.sql("""
                        INSERT INTO institutions (name, type, address, contact_person)
                        VALUES (:name, :type, 'вул. Тестова, 1', 'Контактна особа')
                        RETURNING id
                    """)
                    .param("name", "Заклад " + type)
                    .param("type", type)
                    .query(Long.class)
                    .single();
            assertThat(instId).isNotNull();
        }

        // Відхилення невалідного типу
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO institutions (name, type, address, contact_person)
                    VALUES ('Невалідний заклад', 'HOSPITAL', 'вул. Лікарняна, 5', 'Лікар')
                """).update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V10: Відхилення дублікатів назв закладів без урахування регістру")
    void shouldRejectDuplicateInstitutionNameCaseInsensitive() {
        jdbcClient.sql("""
                    INSERT INTO institutions (name, type, address, contact_person)
                    VALUES ('Дитячий будинок "Сонечко"', 'ORPHANAGE', 'вул. Лісова, 10', 'Директор')
                """)
                .update();

        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO institutions (name, type, address, contact_person)
                    VALUES ('ДИТЯЧИЙ БУДИНОК "СОНЕЧКО"', 'LIBRARY', 'вул. Інша, 20', 'Інший директор')
                """).update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("M3/7.2.2.7/V9: Збереження записів gift_log при повному видаленні архівного замовлення")
    void shouldPreserveGiftLogWhenOrderIsDeleted() {
        Long userId = insertUser("donor@example.com");
        Long instId = jdbcClient.sql("""
                    INSERT INTO institutions (name, type, address, contact_person)
                    VALUES ('Дитячий будинок №5', 'ORPHANAGE', 'вул. Дитяча, 10', 'Директор')
                    RETURNING id
                """)
                .query(Long.class)
                .single();

        Long orderId = jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay, institution_id, institution_name, institution_contact_person)
                    VALUES (:userId, 'GIFT', 'DELIVERED', 'TRANSFER', 500.00, 0.00, 0, 500.00, :instId, 'Дитячий будинок №5', 'Директор')
                    RETURNING id
                """)
                .param("userId", userId)
                .param("instId", instId)
                .query(Long.class)
                .single();

        Long logId = jdbcClient.sql("""
                    INSERT INTO gift_log (order_id, institution_id, institution_name, book_title, quantity, delivered_date)
                    VALUES (:orderId, :instId, 'Дитячий будинок №5', 'Кобзар', 5, CURRENT_DATE)
                    RETURNING id
                """)
                .param("orderId", orderId)
                .param("instId", instId)
                .query(Long.class)
                .single();
        assertThat(logId).isNotNull();

        // Повне видалення архівного замовлення (M3 / 7.2.2.7)
        jdbcClient.sql("DELETE FROM orders WHERE id = :orderId").param("orderId", orderId).update();

        // Запис у gift_log залишається цілісним, order_id оновлюється на NULL (ON DELETE SET NULL)
        var logRow = jdbcClient.sql("SELECT id, order_id, institution_name, book_title FROM gift_log WHERE id = :logId")
                .param("logId", logId)
                .query()
                .singleRow();
        assertThat(logRow.get("id")).isEqualTo(logId);
        assertThat(logRow.get("order_id")).isNull();
        assertThat(logRow.get("institution_name")).isEqualTo("Дитячий будинок №5");
        assertThat(logRow.get("book_title")).isEqualTo("Кобзар");
    }

    @Test
    @DisplayName("D7: На кожній FK-колонці обов'язково присутній індекс (pg_constraint / pg_index)")
    void shouldHaveIndexesOnAllForeignKeyColumns() {
        var unindexedForeignKeys = jdbcClient.sql("""
            SELECT
                c.conname AS constraint_name,
                t.relname AS table_name,
                a.attname AS column_name
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_namespace n ON t.relnamespace = n.oid
            JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
            WHERE c.contype = 'f'
              AND n.nspname = 'public'
              AND NOT EXISTS (
                  SELECT 1
                  FROM pg_index i
                  WHERE i.indrelid = c.conrelid
                    AND i.indkey[0] = a.attnum
              )
        """).query().listOfRows();

        assertThat(unindexedForeignKeys)
                .as("Усі зовнішні ключі (FK) повинні мати відповідний індекс згідно з вимогою D7")
                .isEmpty();
    }

    @Test
    @DisplayName("A.1/V9: Обмеження статусів замовлень та історії рівно 4 значеннями: AWAITING_PAYMENT, PAID, DELIVERED, CANCELLED")
    void shouldEnforceFourOrderStatuses() {
        Long userId = insertUser("status_test@example.com");

        for (String status : new String[]{"AWAITING_PAYMENT", "PAID", "DELIVERED", "CANCELLED"}) {
            Long orderId = jdbcClient.sql("""
                        INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                        VALUES (:userId, 'REGULAR', :status, 'ON_DELIVERY', 100.00, 0.00, 0, 100.00)
                        RETURNING id
                    """)
                    .param("userId", userId)
                    .param("status", status)
                    .query(Long.class)
                    .single();
            assertThat(orderId).isNotNull();

            // Перевірка order_status_history
            Long historyId = jdbcClient.sql("""
                        INSERT INTO order_status_history (order_id, from_status, to_status, comment)
                        VALUES (:orderId, NULL, :status, 'Початковий статус')
                        RETURNING id
                    """)
                    .param("orderId", orderId)
                    .param("status", status)
                    .query(Long.class)
                    .single();
            assertThat(historyId).isNotNull();
        }

        // Відхилення невалідного статусу (наприклад застарілого PROCESSING)
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO orders (user_id, type, status, payment_method, items_total, discount_amount, points_used, total_to_pay)
                    VALUES (:userId, 'REGULAR', 'PROCESSING', 'ON_DELIVERY', 100.00, 0.00, 0, 100.00)
                """)
                        .param("userId", userId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("A.4/V9: Видавництво та рік видання — значення за замовчуванням та обмеження 1450..2100")
    void shouldEnforcePublisherAndPublicationYearConstraints() {
        Long genreId = insertGenre("Наукова фантастика");

        // Успішне додавання з дефолтними значеннями
        Long book1Id = jdbcClient.sql("""
                    INSERT INTO books (title, isbn, genre_id, price, stock)
                    VALUES ('Тестова книга 1', '978-966-03-7099-1', :genreId, 250.00, 5)
                    RETURNING id
                """)
                .param("genreId", genreId)
                .query(Long.class)
                .single();

        var book1Row = jdbcClient.sql("SELECT publisher, publication_year FROM books WHERE id = :id")
                .param("id", book1Id)
                .query()
                .singleRow();
        assertThat(book1Row.get("publisher")).isEqualTo("Невідоме видавництво");
        assertThat(((Number) book1Row.get("publication_year")).intValue()).isEqualTo(2024);

        // Успішне додавання з вказаним видавництвом та валідним роком
        Long book2Id = jdbcClient.sql("""
                    INSERT INTO books (title, isbn, genre_id, price, stock, publisher, publication_year)
                    VALUES ('Тестова книга 2', '978-966-03-7099-2', :genreId, 300.00, 10, 'А-ба-ба-га-ла-ма-га', 2023)
                    RETURNING id
                """)
                .param("genreId", genreId)
                .query(Long.class)
                .single();

        var book2Row = jdbcClient.sql("SELECT publisher, publication_year FROM books WHERE id = :id")
                .param("id", book2Id)
                .query()
                .singleRow();
        assertThat(book2Row.get("publisher")).isEqualTo("А-ба-ба-га-ла-ма-га");
        assertThat(((Number) book2Row.get("publication_year")).intValue()).isEqualTo(2023);

        // Відхилення року раніше 1450
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO books (title, isbn, genre_id, price, stock, publication_year)
                    VALUES ('Стародавня книга', '978-966-03-7099-3', :genreId, 500.00, 1, 1400)
                """)
                        .param("genreId", genreId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Відхилення року пізніше 2100
        assertThatThrownBy(() ->
                jdbcClient.sql("""
                    INSERT INTO books (title, isbn, genre_id, price, stock, publication_year)
                    VALUES ('Книга з майбутнього', '978-966-03-7099-4', :genreId, 500.00, 1, 2150)
                """)
                        .param("genreId", genreId)
                        .update()
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V10: Дедуплікація закладів із переприв'язкою замовлень та збереженням найменшого id")
    void shouldDeduplicateInstitutionsRetainingMinIdAndRelinkingOrders() {
        // Тимчасово видаляємо індекс для емуляції стану старої бази з дублікатами
        jdbcClient.sql("DROP INDEX IF EXISTS uk_institutions_lower_name").update();

        Long id1 = jdbcClient.sql("""
            INSERT INTO institutions (name, address, contact_person)
            VALUES ('Лікарня "Надія"', 'Адреса 1', 'Контакт 1')
            RETURNING id
        """).query(Long.class).single();

        Long id2 = jdbcClient.sql("""
            INSERT INTO institutions (name, address, contact_person)
            VALUES ('лікарня "надія"', 'Адреса 2', 'Контакт 2')
            RETURNING id
        """).query(Long.class).single();

        Long userId = insertUser("giftuser@example.com");

        // Замовлення, прив'язане до дубліката id2
        Long orderId = jdbcClient.sql("""
            INSERT INTO orders (user_id, type, status, payment_method, institution_id, institution_name, institution_contact_person, items_total, discount_amount, points_used, total_to_pay)
            VALUES (:userId, 'GIFT', 'AWAITING_PAYMENT', 'ON_DELIVERY', :instId, 'Лікарня "Надія"', 'Контакт 2', 500.00, 0.00, 0, 500.00)
            RETURNING id
        """)
                .param("userId", userId)
                .param("instId", id2)
                .query(Long.class)
                .single();

        // Виконання логіки міграції V10
        jdbcClient.sql("""
            UPDATE orders o
            SET institution_id = canonical.min_id
            FROM (
                SELECT id, MIN(id) OVER (PARTITION BY LOWER(name)) AS min_id
                FROM institutions
            ) canonical
            WHERE o.institution_id = canonical.id AND canonical.id != canonical.min_id
        """).update();

        jdbcClient.sql("""
            DELETE FROM institutions
            WHERE id NOT IN (
                SELECT MIN(id)
                FROM institutions
                GROUP BY LOWER(name)
            )
        """).update();

        jdbcClient.sql("CREATE UNIQUE INDEX IF NOT EXISTS uk_institutions_lower_name ON institutions (LOWER(name))").update();

        // Перевірка: залишився тільки запис з id1 (найменший)
        var remainingIds = jdbcClient.sql("SELECT id FROM institutions WHERE LOWER(name) = 'лікарня \"надія\"'")
                .query(Long.class)
                .list();
        assertThat(remainingIds).containsExactly(id1);

        // Перевірка: замовлення переприв'язалося на id1
        Long updatedOrderInstId = jdbcClient.sql("SELECT institution_id FROM orders WHERE id = :orderId")
                .param("orderId", orderId)
                .query(Long.class)
                .single();
        assertThat(updatedOrderInstId).isEqualTo(id1);
    }

    @Test
    @DisplayName("R__dev_seed: Ідемпотентне наповнення тестовими даними для всіх сутностей")
    void shouldExecuteDevSeedIdempotentlyAndPopulateAllEntities() throws Exception {
        org.springframework.core.io.Resource seedResource = new org.springframework.core.io.ClassPathResource("db/seed/R__dev_seed.sql");
        String seedSql = new String(seedResource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        // Виконуємо seed перший раз
        for (String statement : splitSqlStatements(seedSql)) {
            if (!statement.isBlank()) {
                jdbcClient.sql(statement).update();
            }
        }

        // Перевірка наявності даних для всіх сутностей
        assertThat(jdbcClient.sql("SELECT count(*) FROM users").query(Long.class).single()).isGreaterThanOrEqualTo(4);
        assertThat(jdbcClient.sql("SELECT count(*) FROM unblock_requests").query(Long.class).single()).isGreaterThanOrEqualTo(1);
        assertThat(jdbcClient.sql("SELECT count(*) FROM password_reset_tokens").query(Long.class).single()).isGreaterThanOrEqualTo(1);
        assertThat(jdbcClient.sql("SELECT count(*) FROM genres").query(Long.class).single()).isGreaterThanOrEqualTo(7);
        assertThat(jdbcClient.sql("SELECT count(*) FROM authors").query(Long.class).single()).isGreaterThanOrEqualTo(11);
        assertThat(jdbcClient.sql("SELECT count(*) FROM institutions").query(Long.class).single()).isEqualTo(3);
        assertThat(jdbcClient.sql("SELECT count(*) FROM books").query(Long.class).single()).isGreaterThanOrEqualTo(13);
        assertThat(jdbcClient.sql("SELECT count(*) FROM book_authors").query(Long.class).single()).isGreaterThanOrEqualTo(13);
        assertThat(jdbcClient.sql("SELECT count(*) FROM cart_items").query(Long.class).single()).isGreaterThanOrEqualTo(2);
        assertThat(jdbcClient.sql("SELECT count(*) FROM challenges").query(Long.class).single()).isGreaterThanOrEqualTo(2);
        assertThat(jdbcClient.sql("SELECT count(*) FROM challenge_completions").query(Long.class).single()).isGreaterThanOrEqualTo(1);
        assertThat(jdbcClient.sql("SELECT count(*) FROM orders").query(Long.class).single()).isGreaterThanOrEqualTo(5);
        assertThat(jdbcClient.sql("SELECT count(*) FROM order_items").query(Long.class).single()).isGreaterThanOrEqualTo(6);
        assertThat(jdbcClient.sql("SELECT count(*) FROM order_status_history").query(Long.class).single()).isGreaterThanOrEqualTo(8);
        assertThat(jdbcClient.sql("SELECT count(*) FROM gift_log").query(Long.class).single()).isGreaterThanOrEqualTo(1);
        assertThat(jdbcClient.sql("SELECT count(*) FROM ratings").query(Long.class).single()).isGreaterThanOrEqualTo(2);
        assertThat(jdbcClient.sql("SELECT count(*) FROM bonus_transactions").query(Long.class).single()).isGreaterThanOrEqualTo(4);

        // Виконуємо seed другий раз для перевірки ідемпотентності (не повинно бути помилок або дублювання)
        for (String statement : splitSqlStatements(seedSql)) {
            if (!statement.isBlank()) {
                jdbcClient.sql(statement).update();
            }
        }

        assertThat(jdbcClient.sql("SELECT count(*) FROM institutions").query(Long.class).single()).isEqualTo(3);
        assertThat(jdbcClient.sql("SELECT count(*) FROM challenges").query(Long.class).single()).isEqualTo(2);
        assertThat(jdbcClient.sql("SELECT count(*) FROM orders").query(Long.class).single()).isEqualTo(5);
    }

    private java.util.List<String> splitSqlStatements(String sql) {
        // Простий поділ за крапкою з комою
        String cleaned = sql.replaceAll("--.*", "");
        return java.util.Arrays.stream(cleaned.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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
