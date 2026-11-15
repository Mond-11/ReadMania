-- R__dev_seed.sql
-- Початкове наповнення тестовими даними для локальної розробки та шоукейсу (профіль local/dev)

-- 1. Користувачі
-- Менеджер: manager@readmania.ua / Manager123!
-- Покупець: user@readmania.ua / User123!
-- Активний читач: reader@readmania.ua / User123!
-- Заблокований користувач: blocked@readmania.ua / User123!
INSERT INTO users (email, password_hash, full_name, role, status, bonus_balance, failed_login_attempts, blocked_at, block_reason)
SELECT 'manager@readmania.ua', '$2a$10$8bfpwagFWI9BqEtBHk2bMeAopSArM3YhEUCa46DJX5k8DKquyyPYm', 'Тестовий Менеджер', 'MANAGER', 'ACTIVE', 0, 0, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER('manager@readmania.ua'));

INSERT INTO users (email, password_hash, full_name, role, status, bonus_balance, failed_login_attempts, blocked_at, block_reason)
SELECT 'user@readmania.ua', '$2a$10$oFEL5AgyKCKHuAfkwdTN3ukXwBSeJKZTdHcqwuPakaC1duPxMMBV2', 'Тестовий Покупець', 'USER', 'ACTIVE', 150, 0, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER('user@readmania.ua'));

INSERT INTO users (email, password_hash, full_name, role, status, bonus_balance, failed_login_attempts, blocked_at, block_reason)
SELECT 'reader@readmania.ua', '$2a$10$oFEL5AgyKCKHuAfkwdTN3ukXwBSeJKZTdHcqwuPakaC1duPxMMBV2', 'Олена Петренко', 'USER', 'ACTIVE', 50, 0, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER('reader@readmania.ua'));

INSERT INTO users (email, password_hash, full_name, role, status, bonus_balance, failed_login_attempts, blocked_at, block_reason)
SELECT 'blocked@readmania.ua', '$2a$10$oFEL5AgyKCKHuAfkwdTN3ukXwBSeJKZTdHcqwuPakaC1duPxMMBV2', 'Заблокований Користувач', 'USER', 'BLOCKED', 0, 5, TIMESTAMPTZ '2026-11-01 10:00:00+02', 'Перевищено ліміт спроб входу'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER('blocked@readmania.ua'));

-- 2. Запити на розблокування
INSERT INTO unblock_requests (user_id, reason, status)
SELECT u.id, 'Прошу розблокувати мій акаунт, пароль відновлено.', 'PENDING'
FROM users u
WHERE u.email = 'blocked@readmania.ua'
  AND NOT EXISTS (
      SELECT 1 FROM unblock_requests ur WHERE ur.user_id = u.id AND ur.status = 'PENDING'
  );

-- 3. Токени скидання пароля
INSERT INTO password_reset_tokens (user_id, token, expires_at)
SELECT u.id, 'dev-reset-token-reader-12345', TIMESTAMPTZ '2026-12-31 23:59:59+02'
FROM users u
WHERE u.email = 'reader@readmania.ua'
  AND NOT EXISTS (
      SELECT 1 FROM password_reset_tokens prt WHERE prt.token = 'dev-reset-token-reader-12345'
  );

-- 4. Жанри
INSERT INTO genres (name) VALUES
    ('Класика'),
    ('Поезія'),
    ('Драматургія'),
    ('Фантастика'),
    ('Детектив'),
    ('Дитяча література'),
    ('Нон-фікшн')
ON CONFLICT DO NOTHING;

-- 5. Автори
INSERT INTO authors (name) VALUES
    ('Тарас Шевченко'),
    ('Леся Українка'),
    ('Іван Франко'),
    ('Джордж Орвелл'),
    ('Агата Крісті'),
    ('Станіслав Лем'),
    ('Артур Конан Дойл'),
    ('Всеволод Нестайко'),
    ('Валер''ян Підмогильний'),
    ('Іван Нечуй-Левицький'),
    ('Рей Бредбері')
ON CONFLICT DO NOTHING;

-- 6. Благодійні заклади (без дублювання, з WHERE NOT EXISTS)
INSERT INTO institutions (name, type, address, contact_person, phone)
SELECT 'Київська міська дитяча клінічна лікарня №1', 'ORPHANAGE', 'м. Київ, вул. Богатирська, 30', 'Олена Ковальчук', '+380442010101'
WHERE NOT EXISTS (SELECT 1 FROM institutions WHERE LOWER(name) = LOWER('Київська міська дитяча клінічна лікарня №1'));

INSERT INTO institutions (name, type, address, contact_person, phone)
SELECT 'Львівський обласний дитячий притулок "Сонечко"', 'BOARDING_SCHOOL', 'м. Львів, вул. Личаківська, 150', 'Михайло Гриньків', '+380322550202'
WHERE NOT EXISTS (SELECT 1 FROM institutions WHERE LOWER(name) = LOWER('Львівський обласний дитячий притулок "Сонечко"'));

INSERT INTO institutions (name, type, address, contact_person, phone)
SELECT 'Харківська обласна бібліотека для юнацтва', 'LIBRARY', 'м. Харків, пр. Науки, 25', 'Тетяна Сидоренко', '+380577000303'
WHERE NOT EXISTS (SELECT 1 FROM institutions WHERE LOWER(name) = LOWER('Харківська обласна бібліотека для юнацтва'));

-- 7. Книги
INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Кобзар', 'Збірка поетичних творів українського генія', '978-966-03-7000-1', g.id, 350.00, 25, 'Фоліо', 2021
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Лісова пісня', 'Драма-феєрія у трьох діях', '978-966-03-7000-2', g.id, 220.00, 15, 'А-ба-ба-га-ла-ма-га', 2022
FROM genres g WHERE g.name = 'Драматургія'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Захар Беркут', 'Історична повість про боротьбу карпатських русинів проти монгольської навали', '978-966-03-7000-3', g.id, 280.00, 18, 'Видавництво Старого Лева', 2020
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT '1984', 'Культовий роман-антиутопія про тоталітаризм', '978-966-03-7000-4', g.id, 320.00, 30, 'Видавництво Жупанського', 2021
FROM genres g WHERE g.name = 'Фантастика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Вбивство у Східному експресі', 'Класичний детективний роман про розслідування Еркюля Пуаро', '978-966-03-7000-5', g.id, 300.00, 12, 'Клуб Сімейного Дозвілля', 2023
FROM genres g WHERE g.name = 'Детектив'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Кайдашева сім''я', 'Реалістична соціально-побутова повість', '978-966-03-7000-6', g.id, 220.00, 20, 'Фоліо', 2021
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Місто', 'Урбаністичний роман про підкорення столиці', '978-966-03-7000-7', g.id, 340.00, 15, 'Видавництво Старого Лева', 2022
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Соляріс', 'Філософсько-фантастичний роман про контакт із позаземним розумом', '978-966-03-7000-8', g.id, 310.00, 18, 'Видавництво Жупанського', 2021
FROM genres g WHERE g.name = 'Фантастика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT '451 градус за Фаренгейтом', 'Роман-антиутопія про суспільство споживання', '978-966-03-7000-9', g.id, 290.00, 22, 'Навчальна книга - Богдан', 2022
FROM genres g WHERE g.name = 'Фантастика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Пригоди Шерлока Холмса', 'Збірка детективних оповідань про знаменитого детектива', '978-966-03-7001-0', g.id, 380.00, 25, 'А-ба-ба-га-ла-ма-га', 2023
FROM genres g WHERE g.name = 'Детектив'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Десять негренят', 'Культовий детективний роман Агати Крісті', '978-966-03-7001-1', g.id, 270.00, 14, 'Клуб Сімейного Дозвілля', 2022
FROM genres g WHERE g.name = 'Детектив'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Тореадори з Васюківки', 'Пригодницька гумористична повість для дітей', '978-966-03-7001-2', g.id, 450.00, 30, 'А-ба-ба-га-ла-ма-га', 2021
FROM genres g WHERE g.name = 'Дитяча література'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock, publisher, publication_year)
SELECT 'Зів''яле листя', 'Збірка ліричних поезій', '978-966-03-7001-3', g.id, 210.00, 12, 'Фоліо', 2020
FROM genres g WHERE g.name = 'Поезія'
ON CONFLICT DO NOTHING;

-- 8. Зв'язки книг з авторами
INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-1' AND a.name = 'Тарас Шевченко'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-2' AND a.name = 'Леся Українка'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-3' AND a.name = 'Іван Франко'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-4' AND a.name = 'Джордж Орвелл'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-5' AND a.name = 'Агата Крісті'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-6' AND a.name = 'Іван Нечуй-Левицький'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-7' AND a.name = 'Валер''ян Підмогильний'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-8' AND a.name = 'Станіслав Лем'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7000-9' AND a.name = 'Рей Бредбері'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7001-0' AND a.name = 'Артур Конан Дойл'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7001-1' AND a.name = 'Агата Крісті'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7001-2' AND a.name = 'Всеволод Нестайко'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b, authors a
WHERE b.isbn = '978-966-03-7001-3' AND a.name = 'Іван Франко'
ON CONFLICT DO NOTHING;

-- 9. Кошик покупця
INSERT INTO cart_items (user_id, book_id, quantity)
SELECT u.id, b.id, 1
FROM users u, books b
WHERE u.email = 'user@readmania.ua' AND b.isbn = '978-966-03-7000-1'
ON CONFLICT (user_id, book_id) DO NOTHING;

INSERT INTO cart_items (user_id, book_id, quantity)
SELECT u.id, b.id, 2
FROM users u, books b
WHERE u.email = 'user@readmania.ua' AND b.isbn = '978-966-03-7000-4'
ON CONFLICT (user_id, book_id) DO NOTHING;

-- 10. Читацькі виклики
-- Активний виклик
INSERT INTO challenges (title, description, required_books, genre_id, deadline, reward_points, status)
SELECT 'Осінній марафон класики', 'Прочитайте 3 книги класичної літератури до кінця року та отримайте бонуси', 3, g.id, TIMESTAMPTZ '2026-12-31 23:59:59+02', 150, 'ACTIVE'
FROM genres g
WHERE g.name = 'Класика'
  AND NOT EXISTS (SELECT 1 FROM challenges WHERE title = 'Осінній марафон класики');

-- Завершений виклик для історії та аналітики
INSERT INTO challenges (title, description, required_books, genre_id, deadline, reward_points, status, finished_at)
SELECT 'Майстри фантастики', 'Прочитайте 2 науково-фантастичні книги та отримайте додаткові бонуси', 2, g.id, TIMESTAMPTZ '2026-11-01 23:59:59+02', 100, 'COMPLETED', TIMESTAMPTZ '2026-11-01 23:59:59+02'
FROM genres g
WHERE g.name = 'Фантастика'
  AND NOT EXISTS (SELECT 1 FROM challenges WHERE title = 'Майстри фантастики');

-- 11. Виконання викликів користувачами
INSERT INTO challenge_completions (user_id, challenge_id, completed_at)
SELECT u.id, c.id, TIMESTAMPTZ '2026-10-25 18:00:00+02'
FROM users u, challenges c
WHERE u.email = 'user@readmania.ua' AND c.title = 'Майстри фантастики'
ON CONFLICT (user_id, challenge_id) DO NOTHING;

-- 12. Замовлення (з різними статусами та типами)

-- Замовлення 1: Доставлене звичайне замовлення з використанням бонусних балів
INSERT INTO orders (user_id, type, status, delivery_method, payment_method, recipient_name, recipient_phone, delivery_address, items_total, discount_amount, points_used, total_to_pay, created_at, status_changed_at, delivered_at)
SELECT u.id, 'REGULAR', 'DELIVERED', 'COURIER', 'TRANSFER', 'Тестовий Покупець', '+380501112233', 'м. Київ, вул. Хрещатик, 1, кв. 10', 570.00, 100.00, 10, 470.00, TIMESTAMPTZ '2026-11-08 10:00:00+02', TIMESTAMPTZ '2026-11-10 14:30:00+02', TIMESTAMPTZ '2026-11-10 14:30:00+02'
FROM users u
WHERE u.email = 'user@readmania.ua'
  AND NOT EXISTS (
      SELECT 1 FROM orders WHERE user_id = u.id AND created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02'
  );

-- Позиції для Замовлення 1
INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
SELECT o.id, b.id, b.title, 350.00, 1
FROM orders o, books b, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02' AND b.isbn = '978-966-03-7000-1'
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id AND book_title = 'Кобзар');

INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
SELECT o.id, b.id, b.title, 220.00, 1
FROM orders o, books b, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02' AND b.isbn = '978-966-03-7000-2'
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id AND book_title = 'Лісова пісня');

-- Історія статусів для Замовлення 1
INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, NULL, 'AWAITING_PAYMENT', TIMESTAMPTZ '2026-11-08 10:00:00+02', u.id, 'Створення замовлення'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'AWAITING_PAYMENT');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, 'AWAITING_PAYMENT', 'PAID', TIMESTAMPTZ '2026-11-08 10:05:00+02', m.id, 'Оплата карткою підтверджена'
FROM orders o, users u, users m
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02' AND m.email = 'manager@readmania.ua'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'PAID');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, 'PAID', 'DELIVERED', TIMESTAMPTZ '2026-11-10 14:30:00+02', m.id, 'Замовлення доставлено кур''єром'
FROM orders o, users u, users m
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02' AND m.email = 'manager@readmania.ua'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'DELIVERED');

-- Замовлення 2: Оплачене звичайне замовлення (очікує відправки)
INSERT INTO orders (user_id, type, status, delivery_method, payment_method, recipient_name, recipient_phone, delivery_address, items_total, discount_amount, points_used, total_to_pay, created_at, status_changed_at)
SELECT u.id, 'REGULAR', 'PAID', 'POST_OFFICE', 'TRANSFER', 'Тестовий Покупець', '+380501112233', 'м. Київ, Відділення Нової Пошти №15', 320.00, 0.00, 0, 320.00, TIMESTAMPTZ '2026-11-14 12:00:00+02', TIMESTAMPTZ '2026-11-14 12:05:00+02'
FROM users u
WHERE u.email = 'user@readmania.ua'
  AND NOT EXISTS (
      SELECT 1 FROM orders WHERE user_id = u.id AND created_at = TIMESTAMPTZ '2026-11-14 12:00:00+02'
  );

INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
SELECT o.id, b.id, b.title, 320.00, 1
FROM orders o, books b, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-14 12:00:00+02' AND b.isbn = '978-966-03-7000-4'
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id AND book_title = '1984');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, NULL, 'AWAITING_PAYMENT', TIMESTAMPTZ '2026-11-14 12:00:00+02', u.id, 'Створення замовлення'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-14 12:00:00+02'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'AWAITING_PAYMENT');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, 'AWAITING_PAYMENT', 'PAID', TIMESTAMPTZ '2026-11-14 12:05:00+02', m.id, 'Оплата підтверджена'
FROM orders o, users u, users m
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-14 12:00:00+02' AND m.email = 'manager@readmania.ua'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'PAID');

-- Замовлення 3: Замовлення, що очікує оплати
INSERT INTO orders (user_id, type, status, delivery_method, payment_method, recipient_name, recipient_phone, delivery_address, items_total, discount_amount, points_used, total_to_pay, created_at, status_changed_at)
SELECT u.id, 'REGULAR', 'AWAITING_PAYMENT', 'POST_OFFICE', 'ON_DELIVERY', 'Олена Петренко', '+380671234567', 'м. Львів, Відділення Нової Пошти №1', 450.00, 0.00, 0, 450.00, TIMESTAMPTZ '2026-11-15 18:00:00+02', TIMESTAMPTZ '2026-11-15 18:00:00+02'
FROM users u
WHERE u.email = 'reader@readmania.ua'
  AND NOT EXISTS (
      SELECT 1 FROM orders WHERE user_id = u.id AND created_at = TIMESTAMPTZ '2026-11-15 18:00:00+02'
  );

INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
SELECT o.id, b.id, b.title, 450.00, 1
FROM orders o, books b, users u
WHERE o.user_id = u.id AND u.email = 'reader@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-15 18:00:00+02' AND b.isbn = '978-966-03-7001-2'
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id AND book_title = 'Тореадори з Васюківки');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, NULL, 'AWAITING_PAYMENT', TIMESTAMPTZ '2026-11-15 18:00:00+02', u.id, 'Оформлення замовлення з післяплатою'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'reader@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-15 18:00:00+02'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'AWAITING_PAYMENT');

-- Замовлення 4: Благодійний подарунок закладу (доставлено)
INSERT INTO orders (user_id, type, status, delivery_method, payment_method, recipient_name, recipient_phone, delivery_address, institution_id, institution_name, institution_contact_person, items_total, discount_amount, points_used, total_to_pay, created_at, status_changed_at, delivered_at)
SELECT u.id, 'GIFT', 'DELIVERED', 'COURIER', 'TRANSFER', 'Олена Ковальчук', '+380442010101', 'м. Київ, вул. Богатирська, 30', inst.id, inst.name, inst.contact_person, 900.00, 0.00, 0, 900.00, TIMESTAMPTZ '2026-11-11 09:00:00+02', TIMESTAMPTZ '2026-11-12 11:00:00+02', TIMESTAMPTZ '2026-11-12 11:00:00+02'
FROM users u, institutions inst
WHERE u.email = 'user@readmania.ua' AND inst.name = 'Київська міська дитяча клінічна лікарня №1'
  AND NOT EXISTS (
      SELECT 1 FROM orders WHERE user_id = u.id AND created_at = TIMESTAMPTZ '2026-11-11 09:00:00+02'
  );

INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
SELECT o.id, b.id, b.title, 450.00, 2
FROM orders o, books b, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-11 09:00:00+02' AND b.isbn = '978-966-03-7001-2'
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id AND book_title = 'Тореадори з Васюківки');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, NULL, 'AWAITING_PAYMENT', TIMESTAMPTZ '2026-11-11 09:00:00+02', u.id, 'Створення благодійного подарунка'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-11 09:00:00+02'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'AWAITING_PAYMENT');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, 'AWAITING_PAYMENT', 'PAID', TIMESTAMPTZ '2026-11-11 09:05:00+02', m.id, 'Оплата подарунку підтверджена'
FROM orders o, users u, users m
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-11 09:00:00+02' AND m.email = 'manager@readmania.ua'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'PAID');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, 'PAID', 'DELIVERED', TIMESTAMPTZ '2026-11-12 11:00:00+02', m.id, 'Книги передано дитячій лікарні'
FROM orders o, users u, users m
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-11 09:00:00+02' AND m.email = 'manager@readmania.ua'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'DELIVERED');

-- Замовлення 5: Скасоване замовлення
INSERT INTO orders (user_id, type, status, delivery_method, payment_method, recipient_name, recipient_phone, delivery_address, items_total, discount_amount, points_used, total_to_pay, created_at, status_changed_at)
SELECT u.id, 'REGULAR', 'CANCELLED', 'COURIER', 'ON_DELIVERY', 'Олена Петренко', '+380671234567', 'м. Львів, вул. Франка, 20', 300.00, 0.00, 0, 300.00, TIMESTAMPTZ '2026-11-05 14:00:00+02', TIMESTAMPTZ '2026-11-05 15:30:00+02'
FROM users u
WHERE u.email = 'reader@readmania.ua'
  AND NOT EXISTS (
      SELECT 1 FROM orders WHERE user_id = u.id AND created_at = TIMESTAMPTZ '2026-11-05 14:00:00+02'
  );

INSERT INTO order_items (order_id, book_id, book_title, book_price, quantity)
SELECT o.id, b.id, b.title, 300.00, 1
FROM orders o, books b, users u
WHERE o.user_id = u.id AND u.email = 'reader@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-05 14:00:00+02' AND b.isbn = '978-966-03-7000-5'
  AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id AND book_title = 'Вбивство у Східному експресі');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, NULL, 'AWAITING_PAYMENT', TIMESTAMPTZ '2026-11-05 14:00:00+02', u.id, 'Створення замовлення'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'reader@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-05 14:00:00+02'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'AWAITING_PAYMENT');

INSERT INTO order_status_history (order_id, from_status, to_status, changed_at, changed_by_user_id, comment)
SELECT o.id, 'AWAITING_PAYMENT', 'CANCELLED', TIMESTAMPTZ '2026-11-05 15:30:00+02', u.id, 'Скасовано покупцем'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'reader@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-05 14:00:00+02'
  AND NOT EXISTS (SELECT 1 FROM order_status_history WHERE order_id = o.id AND to_status = 'CANCELLED');

-- 13. Журнал подарунків (Gift Log)
INSERT INTO gift_log (order_id, institution_id, institution_name, book_title, quantity, delivered_date, created_at)
SELECT o.id, o.institution_id, o.institution_name, 'Тореадори з Васюківки', 2, DATE '2026-11-12', TIMESTAMPTZ '2026-11-12 11:00:00+02'
FROM orders o, users u
WHERE o.user_id = u.id AND u.email = 'user@readmania.ua' AND o.created_at = TIMESTAMPTZ '2026-11-11 09:00:00+02'
ON CONFLICT (order_id, book_title) DO NOTHING;

-- 14. Оцінки книг (Ratings)
INSERT INTO ratings (user_id, book_id, order_id, value, created_at, updated_at)
SELECT u.id, b.id, o.id, 5, TIMESTAMPTZ '2026-11-10 16:00:00+02', TIMESTAMPTZ '2026-11-10 16:00:00+02'
FROM users u, books b, orders o
WHERE u.email = 'user@readmania.ua' AND b.isbn = '978-966-03-7000-1' AND o.user_id = u.id AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02'
  AND NOT EXISTS (
      SELECT 1 FROM ratings r WHERE r.user_id = u.id AND r.book_id = b.id
  );

INSERT INTO ratings (user_id, book_id, order_id, value, created_at, updated_at)
SELECT u.id, b.id, o.id, 4, TIMESTAMPTZ '2026-11-10 16:05:00+02', TIMESTAMPTZ '2026-11-10 16:05:00+02'
FROM users u, books b, orders o
WHERE u.email = 'user@readmania.ua' AND b.isbn = '978-966-03-7000-2' AND o.user_id = u.id AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02'
  AND NOT EXISTS (
      SELECT 1 FROM ratings r WHERE r.user_id = u.id AND r.book_id = b.id
  );

-- 15. Транзакції бонусних балів (Bonus Transactions)
-- Списання балів у замовленні
INSERT INTO bonus_transactions (user_id, points, type, order_id, created_at)
SELECT u.id, -10, 'SPENT', o.id, TIMESTAMPTZ '2026-11-08 10:00:00+02'
FROM users u, orders o
WHERE u.email = 'user@readmania.ua' AND o.user_id = u.id AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02'
  AND NOT EXISTS (
      SELECT 1 FROM bonus_transactions WHERE user_id = u.id AND order_id = o.id AND type = 'SPENT'
  );

-- Нарахування балів за замовлення
INSERT INTO bonus_transactions (user_id, points, type, order_id, created_at)
SELECT u.id, 10, 'ORDER_REWARD', o.id, TIMESTAMPTZ '2026-11-10 14:30:00+02'
FROM users u, orders o
WHERE u.email = 'user@readmania.ua' AND o.user_id = u.id AND o.created_at = TIMESTAMPTZ '2026-11-08 10:00:00+02'
  AND NOT EXISTS (
      SELECT 1 FROM bonus_transactions WHERE order_id = o.id AND type = 'ORDER_REWARD'
  );

-- Бонус за виконання виклику
INSERT INTO bonus_transactions (user_id, points, type, challenge_id, created_at)
SELECT u.id, 100, 'CHALLENGE_REWARD', c.id, TIMESTAMPTZ '2026-10-25 18:00:00+02'
FROM users u, challenges c
WHERE u.email = 'user@readmania.ua' AND c.title = 'Майстри фантастики'
  AND NOT EXISTS (
      SELECT 1 FROM bonus_transactions WHERE user_id = u.id AND challenge_id = c.id AND type = 'CHALLENGE_REWARD'
  );

-- Бонус за оцінку книги
INSERT INTO bonus_transactions (user_id, points, type, book_id, created_at)
SELECT u.id, 50, 'RATING_REWARD', b.id, TIMESTAMPTZ '2026-11-10 16:00:00+02'
FROM users u, books b
WHERE u.email = 'user@readmania.ua' AND b.isbn = '978-966-03-7000-1'
  AND NOT EXISTS (
      SELECT 1 FROM bonus_transactions WHERE user_id = u.id AND book_id = b.id AND type = 'RATING_REWARD'
  );
