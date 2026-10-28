-- R__dev_seed.sql
-- Початкове наповнення тестовими даними для локальної розробки (профіль local)

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
    ('Агата Крісті')
ON CONFLICT DO NOTHING;

INSERT INTO institutions (name, address, contact_person, phone) VALUES
    ('Київська міська дитяча клінічна лікарня №1', 'м. Київ, вул. Богатирська, 30', 'Олена Ковальчук', '+380442010101'),
    ('Львівський обласний дитячий притулок "Сонечко"', 'м. Львів, вул. Личаківська, 150', 'Михайло Гриньків', '+380322550202'),
    ('Харківська обласна бібліотека для юнацтва', 'м. Харків, пр. Науки, 25', 'Тетяна Сидоренко', '+380577000303')
ON CONFLICT DO NOTHING;

-- Додавання прикладів книг
INSERT INTO books (title, description, isbn, genre_id, price, stock)
SELECT 'Кобзар', 'Збірка поетичних творів українського генія', '978-966-03-7000-1', g.id, 350.00, 25
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock)
SELECT 'Лісова пісня', 'Драма-феєрія у трьох діях', '978-966-03-7000-2', g.id, 220.00, 15
FROM genres g WHERE g.name = 'Драматургія'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock)
SELECT 'Захар Беркут', 'Історична повість про боротьбу карпатських русинів проти монгольської навали', '978-966-03-7000-3', g.id, 280.00, 18
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock)
SELECT '1984', 'Культовий роман-антиутопія про тоталітаризм', '978-966-03-7000-4', g.id, 320.00, 30
FROM genres g WHERE g.name = 'Фантастика'
ON CONFLICT DO NOTHING;

INSERT INTO books (title, description, isbn, genre_id, price, stock)
SELECT 'Вбивство у Східному експресі', 'Класичний детективний роман про розслідування Еркюля Пуаро', '978-966-03-7000-5', g.id, 300.00, 12
FROM genres g WHERE g.name = 'Детектив'
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

-- Приклад активного читацького виклику
INSERT INTO challenges (title, description, required_books, genre_id, deadline, reward_points, status)
SELECT 'Осінній марафон класики', 'Прочитайте 3 книги класичної літератури до кінця року та отримайте бонуси', 3, g.id, TIMESTAMPTZ '2026-12-31 23:59:59+02', 150, 'ACTIVE'
FROM genres g WHERE g.name = 'Класика'
ON CONFLICT DO NOTHING;
