-- R__dev_seed.sql
-- Початкове наповнення тестовими даними для локальної розробки (профіль local)

-- Тестові користувачі для локального середовища
-- Менеджер: manager@readmania.ua / Manager123!
-- Покупець: user@readmania.ua / User123!
INSERT INTO users (email, password_hash, full_name, role, status, bonus_balance) VALUES
    ('manager@readmania.ua', '$2a$10$8bfpwagFWI9BqEtBHk2bMeAopSArM3YhEUCa46DJX5k8DKquyyPYm', 'Тестовий Менеджер', 'MANAGER', 'ACTIVE', 0),
    ('user@readmania.ua', '$2a$10$oFEL5AgyKCKHuAfkwdTN3ukXwBSeJKZTdHcqwuPakaC1duPxMMBV2', 'Тестовий Покупець', 'USER', 'ACTIVE', 100)
ON CONFLICT DO NOTHING;

INSERT INTO genres (name) VALUES
    ('Класика'),
    ('Поезія'),
    ('Драматургія'),
    ('Фантастика'),
    ('Детектив'),
    ('Дитяча література'),
    ('Нон-фікшн')
ON CONFLICT DO NOTHING;

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

INSERT INTO institutions (name, type, address, contact_person, phone) VALUES
    ('Київська міська дитяча клінічна лікарня №1', 'ORPHANAGE', 'м. Київ, вул. Богатирська, 30', 'Олена Ковальчук', '+380442010101'),
    ('Львівський обласний дитячий притулок "Сонечко"', 'BOARDING_SCHOOL', 'м. Львів, вул. Личаківська, 150', 'Михайло Гриньків', '+380322550202'),
    ('Харківська обласна бібліотека для юнацтва', 'LIBRARY', 'м. Харків, пр. Науки, 25', 'Тетяна Сидоренко', '+380577000303')
ON CONFLICT DO NOTHING;

-- Додавання прикладів книг
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

-- Зв'язки книг з авторами
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

-- Приклад активного читацького виклику
INSERT INTO challenges (title, description, required_books, genre_id, deadline, reward_points, status)
SELECT 'Осінній марафон класики', 'Прочитайте 3 книги класичної літератури до кінця року та отримайте бонуси', 3, g.id, TIMESTAMPTZ '2026-12-31 23:59:59+02', 150, 'ACTIVE'
FROM genres g
WHERE g.name = 'Класика'
  AND NOT EXISTS (SELECT 1 FROM challenges WHERE title = 'Осінній марафон класики');
