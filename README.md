# ReadMania

Вебзастосунок книжкового інтернет-магазину «ReadMania». Монорепозиторій: бекенд (Spring Boot) і фронтенд (React + Vite) в єдиній кодовій базі.

---

## Технологічний стек

- **Бекенд:** Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA / Hibernate, Flyway, PostgreSQL 17, Maven.
- **Тестування бекенду:** JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL 17), MockMvc.
- **Фронтенд:** React 19, Vite, Oxlint.
- **Інфраструктура та CI/CD:** Docker / Docker Compose, GitHub Actions.

---

## Структура репозиторію та функціонал файлів

```
ReadMania/
├── .github/
│   └── workflows/
│       └── backend-ci.yml             # Пайплайн GitHub Actions: збірка та тестування бекенду (JDK 21, Testcontainers)
├── backend/                           # Серверна частина (Java 21, Spring Boot 4.1, Maven)
│   ├── .mvn/wrapper/                  # Налаштування Maven Wrapper
│   ├── mvnw, mvnw.cmd                 # Скрипти запуску Maven Wrapper
│   ├── pom.xml                        # Опис залежностей та плагінів збірки Maven
│   ├── http/                          # Запити HTTP Client в IntelliJ IDEA
│   │   ├── http-client.env.json       # Змінні середовища (local, prod)
│   │   ├── http-client.private.env.json.example # Шаблон для приватних секретів (паролів)
│   │   ├── auth.http                  # Запити домену автентифікації та сесій
│   │   ├── catalog.http               # Запити домену каталогу книг і жанрів
│   │   └── orders.http                # Запити домену замовлень і розрахунків
│   └── src/
│       ├── main/
│       │   ├── java/ua/readmania/
│       │   │   ├── ReadmaniaBackendApplication.java  # Головна точка входу Spring Boot застосунку
│       │   │   ├── common/            # Загальносистемні компоненти та обробка винятків
│       │   │   │   ├── GlobalExceptionHandler.java   # Централізований обробник помилок (Problem Details RFC 9457)
│       │   │   │   ├── NotFoundException.java        # Доменний виняток для ресурсів, що не знайдено (404)
│       │   │   │   ├── ConflictException.java        # Доменний виняток для конфліктів стану (409)
│       │   │   │   ├── BusinessRuleException.java    # Доменний виняток для порушення бізнес-правил (400/409)
│       │   │   │   └── ValidationError.java          # Модель деталізації помилок валідації (field, message)
│       │   │   └── security/          # Безпека та автентифікація
│       │   │       ├── SecurityConfig.java           # Конфігурація Spring Security (RBAC, default deny, BCrypt)
│       │   │       ├── ProblemDetailsAuthenticationEntryPoint.java  # Обробник 401 Unauthorized у форматі Problem Details
│       │   │       ├── ProblemDetailsAccessDeniedHandler.java       # Обробник 403 Forbidden у форматі Problem Details
│       │   │       └── AccountStatusFilter.java      # Фільтр перевірки блокування акаунта під час активної сесії
│       │   └── resources/
│       │       ├── application.yml         # Основна конфігурація застосунку (БД, JPA, Flyway, Actuator, сесії)
│       │       ├── application-prod.yml    # Профіль для продакшн-середовища (Secure cookie)
│       │       ├── application-example.yml # Приклад конфігурації для локального налаштування
│       │       ├── db/migration/
│       │       │   ├── V1__init.sql        # Початкова схема бази даних
│       │       │   ├── V2__users.sql       # Користувачі, ролі, статус та бонуси
│       │       │   ├── V3__catalog.sql     # Жанри, автори, книги (3НФ), обкладинки
│       │       │   ├── V4__cart_institutions.sql # Кошик та благодійні заклади
│       │       │   ├── V5__orders.sql      # Замовлення, позиції, історія статусів
│       │       │   ├── V6__loyalty.sql     # Журнал бонусів та оцінки книг
│       │       │   ├── V7__challenges.sql  # Читацькі виклики
│       │       │   └── V8__gift_log.sql    # Журнал благодійних дарунків
│       │       └── db/seed/
│       │           └── R__dev_seed.sql     # Початкові дані для локальної розробки (profile local)
│       └── test/
│           ├── java/ua/readmania/
│           │   ├── BaseIntegrationTest.java          # Базовий клас інтеграційних тестів (@Transactional, Flyway, validate)
│           │   ├── TestcontainersConfiguration.java  # Singleton-контейнер PostgreSQL 17 (@ServiceConnection)
│           │   ├── SchemaIT.java                     # Інтеграційні тести схеми БД, FK-індексів та бізнес-обмежень
│           │   ├── ReadmaniaBackendApplicationTests.java # Тест завантаження контексту Spring Boot
│           │   ├── common/
│           │   │   └── GlobalExceptionHandlerTest.java   # Тести типізованих відповідей та статусів помилок
│           │   └── security/
│           │       ├── SecurityConfigTest.java       # Тести базових правил доступу, паролів та статусів
│           │       └── LocalProfileSecurityConfigTest.java # Тест доступності Swagger у профілі "local"
│           └── resources/
│               ├── application.yml         # Базова конфігурація для тестового контексту
│               └── application-test.yml    # Профіль для ізольованого тестування
├── frontend/                          # Клієнтська частина (React 19, Vite)
│   ├── public/                        # Статичні ресурси (favicon.svg, icons.svg)
│   ├── src/
│   │   ├── assets/                    # Локальні медіа-ресурси
│   │   ├── App.jsx, App.css           # Головний компонент та його стилі
│   │   ├── main.jsx                   # Точка монтування React-застосунку
│   │   └── index.css                  # Глобальні стилі
│   ├── index.html                     # HTML-шаблон сторінки
│   ├── package.json                   # Залежності та npm-скрипти
│   ├── vite.config.js                 # Конфігурація збирача Vite
│   ├── .oxlintrc.json                 # Налаштування лінтера Oxlint
│   └── README.md                      # Документація клієнтської частини
├── docs/                              # Проєктна документація
│   ├── API.md                         # Специфікація REST API ендпоінтів та контрактів
│   ├── DATABASE_DECISIONS.md          # Реєстр архітектурних рішень щодо БД (M1, 3НФ, M5, лояльність тощо)
│   ├── ERD.md                         # Діаграма сутностей і зв'язків (Mermaid ERD) та опис схеми (O2)
│   ├── Технічне завдання розробки Вебзастосунку.pdf # ТЗ проєкту
│   └── Cпецифікація вимог до Вебзастосунку.pdf       # SRS проєкту
├── docker-compose.yml                 # Локальне середовище бази даних PostgreSQL 17
├── .editorconfig                      # Стандарти форматування коду
├── .gitattributes                     # Нормалізація закінчень рядків у Git
├── .gitignore                         # Виключення тимчасових та згенерованих файлів
└── README.md                          # Головна документація проєкту
```

---

## Ключові архітектурні рішення

### 1. Безпека (Spring Security)
- **Розмежування доступу:** реалізовано правило *Default Deny* для всіх шляхів `/api/**`. Публічно відкриті лише `/api/auth/register`, `/api/auth/login`, `/api/catalog/**` та `/actuator/health`. Ендпоінти `/api/manager/**` суворо обмежені роллю `MANAGER`.
- **Шифрування паролів:** `BCryptPasswordEncoder` (відкриті паролі ніде не зберігаються та не логуються).
- **Сесії та Cookie:** `HttpOnly`, `SameSite=Lax`, автоматичний прапорець `Secure` у профілі `prod`.
- **Активні сесії:** наявний `AccountStatusFilter` для перевірки блокування облікового запису на рівні кожного запиту у відкритій сесії.
- **Документація:** Swagger/OpenAPI доступний виключно у профілі `local` та блокується на стадії production.

### 2. Уніфікована обробка помилок (Problem Details)
- Усі відповіді про помилки формуються за стандартом **RFC 9457 Problem Details** із MIME-типом `application/problem+json`.
- Коди відповідей:
  - `400 Bad Request` — помилки валідації (з масивом `errors: [{ field, message }]`), невалідні параметри;
  - `401 Unauthorized` — спроба доступу без автентифікації;
  - `403 Forbidden` — недостатньо прав або заблокований акаунт;
  - `404 Not Found` — ресурс не знайдено;
  - `409 Conflict` — порушення оптимістичного блокування або цілісності даних;
  - `500 Internal Server Error` — непередбачені помилки з генерацією унікального `errorId` для безпечного аудиту без витоку стектрейсів.
- Забезпечено сумісність із SPA: маршрутизація 404 у JSON діє лише для `/api/**`, тоді як невідомі статичні шляхи направляються на `index.html`.

### 3. База даних та міграції
- Використовується **PostgreSQL 17**.
- Керування схемою здійснюється через **Flyway** (`db/migration/V1__init.sql`).
- Hibernate налаштовано у режимі валідації схеми (`ddl-auto: validate`).

---

## Швидкий старт та локальний запуск

### Попередні вимоги
- **Java:** JDK 21 (рекомендовано Eclipse Temurin 21)
- **Node.js:** v20+ та npm
- **Docker:** встановлений та запущений Docker Daemon

### 1. Запуск бази даних
```bash
docker compose up -d
```
Створюється контейнер PostgreSQL 17 (`readmania-db`) на порту `5432`.

### 2. Запуск бекенду
```bash
cd backend

# Стандартний запуск:
./mvnw spring-boot:run

# Запуск із профілем local (активує Swagger/OpenAPI):
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Сервер буде доступний за адресою `http://localhost:8080`.

### 3. Запуск фронтенду
```bash
cd frontend
npm install
npm run dev
```
Клієнтський застосунок буде доступний за адресою `http://localhost:5173`.

---

## Тестування та перевірка коду

### Бекенд
Усі тести виконуються ізольовано та без зовнішніх залежностей завдяки **Testcontainers**, який автоматично підіймає PostgreSQL 17 у Docker:
```bash
cd backend

# Запуск усіх тестів:
./mvnw test

# Повна перевірка збірки та тестів:
./mvnw verify
```

### Фронтенд
```bash
cd frontend

# Перевірка коду лінтером Oxlint:
npm run lint

# Тестова збірка:
npm run build
```

---

## Виклик та ручне тестування API (HTTP Client в IntelliJ IDEA)

Для швидкого виклику та тестування ендпоінтів бекенду без участі фронтенду в проєкті налаштовано набір `.http`-файлів у каталозі `backend/http/`:

- `auth.http` — реєстрація, вхід (USER/MANAGER), отримання поточного профілю (`/api/auth/me`), завершення сеансу та сценарії помилок (400, 401, 403, 409).
- `catalog.http` — публічний перегляд книг із пагінацією та фільтрами, деталі книги, жанри, операції менеджера з додавання/редагування видань та обробка помилок.
- `orders.http` — попередній розрахунок вартості, створення звичайних та подарункових замовлень, перегляд історії, скасування замовлення, зміна статусу менеджером та валідація конфліктів.

### Налаштування середовища (Environments)
1. Публічні змінні середовищ (хости, тестові email) зберігаються у `backend/http/http-client.env.json`.
2. Реальні паролі та секрети зберігаються локально у `backend/http/http-client.private.env.json` (шаблон наведено в `.example`, файл автоматично додано до `.gitignore`).
3. Під час запуску запитів у редакторі IntelliJ IDEA оберіть потрібне цільове середовище (наприклад, `local`).
4. IntelliJ HTTP Client автоматично зберігає сесійну cookie `JSESSIONID` між послідовними викликами (після виконання запиту входу `login` наступні запити автоматично авторизовані).

---

## CI / CD
У репозиторії налаштовано безперервну інтеграцію через GitHub Actions (`.github/workflows/backend-ci.yml`):
- Тригери: `push` та `pull_request` у гілку `main` (за умови змін у `backend/**`).
- Середовище: Ubuntu Latest, JDK 21 Temurin, кеш Maven.
- Команда: `./mvnw -B verify` (збірка, виконання міграцій Flyway та перевірка відповідності сутностей схемі БД через Testcontainers).
