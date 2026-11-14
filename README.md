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
│   │   ├── auth.http                  # Запити домену автентифікації, профілю та сесій
│   │   ├── catalog.http               # Запити каталогу книг, фільтрації, авторів та жанрів
│   │   └── orders.http                # Запити замовлень, розрахунків, масових дій та переходів
│   └── src/
│       ├── main/
│       │   ├── java/ua/readmania/
│       │   │   ├── ReadmaniaBackendApplication.java  # Головна точка входу Spring Boot застосунку (@EnableAsync, @EnableScheduling)
│       │   │   ├── ManagerBootstrapInitializer.java  # Автоматичне створення першого менеджера при старті на продакшні
│       │   │   ├── common/            # Загальносистемні компоненти та обробка винятків
│       │   │   │   ├── GlobalExceptionHandler.java   # Централізований обробник помилок (Problem Details RFC 9457)
│       │   │   │   ├── NotFoundException.java        # Доменний виняток для ресурсів, що не знайдено (404)
│       │   │   │   ├── ConflictException.java        # Доменний виняток для конфліктів стану (409)
│       │   │   │   ├── OrderStatusConflictException.java # Доменний виняток для конфліктів статусів замовлень (409)
│       │   │   │   ├── AccountBlockedException.java  # Виняток для заблокованих облікових записів (403)
│       │   │   │   ├── BusinessRuleException.java    # Доменний виняток для порушення бізнес-правил (400/409)
│       │   │   │   └── ValidationError.java          # Модель деталізації помилок валідації (field, message)
│       │   │   ├── controller/        # REST API контролери
│       │   │   │   ├── AuthController.java           # Публічна автентифікація, сесії, апеляції розблокування
│       │   │   │   ├── ProfileController.java        # Профіль користувача, зміна email та пароля
│       │   │   │   ├── CatalogController.java        # Публічний каталог книг, жанрів, авторів, обкладинки
│       │   │   │   ├── ManagerCatalogController.java # Управління каталогом, жанрами, авторами та CSV-імпортом
│       │   │   │   ├── CartController.java           # Кошик покупця
│       │   │   │   ├── OrderController.java          # Оформлення замовлень, історія та скасування покупцем
│       │   │   │   ├── ManagerOrderController.java   # Управління замовленнями, FSM-переходи, масові операції
│       │   │   │   ├── LoyaltyController.java        # Бонусний баланс та журнал операцій
│       │   │   │   ├── ChallengeController.java      # Читацькі виклики
│       │   │   │   ├── InstitutionController.java    # Благодійні заклади
│       │   │   │   └── RatingController.java         # Оцінювання книг та публічні рейтинги
│       │   │   ├── service/           # Сервісний шар бізнес-логіки
│       │   │   │   ├── AuthService.java              # Логіка входу, захисту від brute-force, відновлення пароля
│       │   │   │   ├── UserService.java              # Керування профілем, зміна email/пароля
│       │   │   │   ├── CatalogService.java           # Пошук, фільтрація залишків, розрахунок canDelete
│       │   │   │   ├── ManagerCatalogService.java    # CRUD книг, жанрів, авторів
│       │   │   │   ├── CsvImportService.java         # Пакетний імпорт видань із валідацією полів
│       │   │   │   ├── CartService.java              # Логіка кошика
│       │   │   │   ├── order/                        # OrderService, ManagerOrderService (FSM 4 статусів)
│       │   │   │   ├── loyalty/                      # LoyaltyService
│       │   │   │   ├── challenge/                    # ChallengeService (прогрес та планувальник @Scheduled)
│       │   │   │   ├── institution/                  # InstitutionService
│       │   │   │   ├── rating/                       # RatingService (приватність користувачів)
│       │   │   │   └── notification/                 # NotificationService, LoggingNotificationService, SmtpNotificationService
│       │   │   ├── domain/            # Доменний шар (JPA-сутності та Spring Data репозиторії)
│       │   │   │   ├── user/          # User, Role, UserStatus, PasswordResetToken, UnblockRequest
│       │   │   │   ├── catalog/       # Book, Genre, Author, BookCover, BookSpecifications, BookRepository
│       │   │   │   ├── cart/          # CartItem, CartItemId, CartItemRepository
│       │   │   │   ├── institution/   # Institution, InstitutionType, InstitutionRepository
│       │   │   │   ├── order/         # Order, OrderItem, OrderStatusHistory, OrderRepository
│       │   │   │   ├── loyalty/       # BonusTransaction, Rating, BonusTransactionRepository, RatingRepository
│       │   │   │   ├── challenge/     # Challenge, ChallengeCompletion, ChallengeRepository
│       │   │   │   └── gift/          # GiftLog, GiftLogRepository
│       │   │   ├── dto/               # Об'єкти передачі даних (Request / Response DTOs)
│       │   │   └── security/          # Безпека та автентифікація
│       │   │       ├── SecurityConfig.java           # Конфігурація Spring Security (RBAC, default deny, BCrypt, CSRF)
│       │   │       ├── SpaCsrfTokenRequestHandler.java # Обробник CSRF-токенів для SPA (заголовок X-XSRF-TOKEN)
│       │   │       ├── CsrfCookieFilter.java         # Фільтр генерації XSRF-TOKEN cookie
│       │   │       ├── ProblemDetailsAuthenticationEntryPoint.java  # Обробник 401 Unauthorized
│       │   │       ├── ProblemDetailsAccessDeniedHandler.java       # Обробник 403 Forbidden з типізованими URI
│       │   │       ├── AccountStatusFilter.java      # Фільтр перевірки блокування акаунта під час активної сесії
│       │   │       └── CustomUserDetails.java        # Адаптер користувача для Spring Security
│       │   └── resources/
│       │       ├── application.yml         # Основна конфігурація застосунку (БД, JPA, Flyway, Actuator, сесії)
│       │       ├── application-prod.yml    # Профіль для продакшн-середовища (Secure cookie, SMTP)
│       │       ├── application-example.yml # Приклад конфігурації для локального налаштування
│       │       ├── db/migration/
│       │       │   ├── V1__init.sql        # Початкова схема бази даних
│       │       │   ├── V2__users.sql       # Користувачі, ролі, статус та бонуси
│       │       │   ├── V3__catalog.sql     # Жанри, автори, книги (3НФ), обкладинки
│       │       │   ├── V4__cart_institutions.sql # Кошик та благодійні заклади
│       │       │   ├── V5__orders.sql      # Замовлення, позиції, історія статусів
│       │       │   ├── V6__loyalty.sql     # Журнал бонусів та оцінки книг
│       │       │   ├── V7__challenges.sql  # Читацькі виклики
│       │       │   ├── V8__gift_log.sql    # Журнал благодійних дарунків
│       │       │   └── V9__schema_fixes.sql # Фікси SRS: 4 статуси, видавництво, рік, контакти закладу
│       │       └── db/seed/
│       │           └── R__dev_seed.sql     # Початкові дані для локальної розробки (profile local)
│       └── test/
│           ├── java/ua/readmania/
│           │   ├── BaseIntegrationTest.java          # Базовий клас інтеграційних тестів (@Transactional, Flyway, validate)
│           │   ├── TestcontainersConfiguration.java  # Singleton-контейнер PostgreSQL 17 (@ServiceConnection)
│           │   ├── CsrfTestHelper.java               # Хелпер налаштування CSRF-заголовків для MockMvc
│           │   ├── SchemaIT.java                     # Інтеграційні тести схеми БД, FK-індексів та бізнес-обмежень
│           │   ├── OpenApiExportIT.java              # Тест автоматичного експорту актуальної схеми в docs/openapi.json
│           │   ├── AuthIntegrationTest.java          # Інтеграційні тести автентифікації, rate-limiting, апеляцій
│           │   ├── CatalogIntegrationTest.java       # Інтеграційні тести каталогу, фільтрації залишків, CSV
│           │   ├── OrderIntegrationTest.java         # Інтеграційні тести життєвого циклу замовлень (FSM 4 статусів)
│           │   ├── Slice4GamificationAndGiftSetIT.java # Інтеграційні тести лояльності, викликів та дарунків
│           │   ├── ReadmaniaBackendApplicationTests.java # Тест завантаження контексту Spring Boot
│           │   ├── domain/
│           │   │   └── DomainRepositoryIT.java       # Інтеграційні тести JPA-сутностей, репозиторіїв та специфікацій G1
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
│   ├── openapi.json                   # Експортована схема OpenAPI 3.1 для кодогенерації клієнта
│   ├── CURRENT_STATE.md               # Поточний стан системи, матриця відповідності вимогам ТЗ/SRS
│   ├── DATABASE_DECISIONS.md          # Реєстр архітектурних рішень щодо БД (M1, 3НФ, M5, лояльність тощо)
│   ├── ERD.md                         # Діаграма сутностей і зв'язків (Mermaid ERD) та опис схеми
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

### 1. Безпека та контроль доступу (Spring Security)
- **Розмежування доступу:** реалізовано правило *Default Deny* для всіх шляхів `/api/**`. Публічно відкриті лише `/api/auth/register`, `/api/auth/login`, `/api/auth/password-reset/**`, `/api/auth/unblock-request`, `/api/catalog/**` та `/actuator/health`. Ендпоінти `/api/manager/**` суворо обмежені роллю `MANAGER`.
- **Шифрування паролів:** `BCryptPasswordEncoder` (відкриті паролі ніде не зберігаються та не логуються).
- **Сесії та Cookie:** `HttpOnly`, `SameSite=Lax`, автоматичний прапорець `Secure` у профілі `prod`.
- **CSRF-захист для SPA:** налаштовано `CookieCsrfTokenRepository.withHttpOnlyFalse()` та `SpaCsrfTokenRequestHandler`. Клієнтський застосунок отримує токен у cookie `XSRF-TOKEN` та передає його у заголовку `X-XSRF-TOKEN` під час запитів зі зміною стану (`POST`, `PUT`, `PATCH`, `DELETE`).
- **Контроль активних сесій:** наявний `AccountStatusFilter` для миттєвої перевірки статусу блокування облікового запису на рівні кожного запиту у відкритій сесії.
- **Захист від Brute-Force:** після 5 поспіль невдалих спроб входу обліковий запис тимчасово блокується на 15 хвилин з типізованою відповіддю `urn:readmania:error:login-rate-limited` та полем `retryAfter`.
- **Ініціалізація Менеджера (Bootstrap):** компонент `ManagerBootstrapInitializer` (`ApplicationRunner`) створює першого менеджера на продакшні за наявності змінних середовища `APP_INITIAL_MANAGER_EMAIL` та `APP_INITIAL_MANAGER_PASSWORD`.

### 2. Сповіщення та фонові задачі
- **Поштовий сервіс (`NotificationService`):** асинхронна відправка листів (`@Async`, `@EnableAsync`) про замовлення, зміну статусів, блокування/розблокування та скидання пароля.
  - `@Profile("prod")` — `SmtpNotificationService` на базі JavaMailSender;
  - `@Profile({"local", "test", "default"})` — `LoggingNotificationService` із логуванням тексту повідомлень та збереженням у пам'яті для інтеграційних тестів.
- **Планувальник читацьких викликів (`@EnableScheduling`):** фонова задача періодично перевіряє активні виклики та автоматично завершує виклики з простроченим дедлайном (`deadline < now()`).

### 3. Уніфікована обробка помилок (Problem Details RFC 9457)
- Усі помилки формуються за стандартом **RFC 9457** із MIME-типом `application/problem+json`.
- Коди відповідей:
  - `400 Bad Request` — помилки валідації (з масивом `errors: [{ field, message }]`), невалідні параметри (наприклад, `stockMin > stockMax`);
  - `401 Unauthorized` — спроба доступу без автентифікації;
  - `403 Forbidden` — недостатньо прав або заблокований акаунт (`urn:readmania:error:access-denied`, `urn:readmania:error:account-blocked`, `urn:readmania:error:login-rate-limited`);
  - `404 Not Found` — ресурс не знайдено;
  - `409 Conflict` — конфлікти статусів замовлень (`urn:readmania:error:order-status-conflict`), дублікати ISBN/авторів або спрацювання оптимістичного блокування (`@Version`);
  - `500 Internal Server Error` — непередбачені помилки з генерацією унікального `errorId` для безпечного аудиту без витоку стектрейсів.
- Забезпечено сумісність із SPA: маршрутизація 404 у JSON діє лише для `/api/**`, тоді як невідомі статичні шляхи направляються на `index.html`.

### 4. База даних, міграції та схема
- Використовується **PostgreSQL 17**.
- Керування схемою здійснюється через **Flyway** (`db/migration/V1__init.sql` ... `V9__schema_fixes.sql`).
- Hibernate налаштовано у режимі суворої валідації схеми (`ddl-auto: validate`).
- Машина станів замовлення (FSM) зведена до **4 актуальних статусів** згідно з SRS: `AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED`.
- Реалізовано 100% покриття зовнішніх ключів (FK) B-Tree індексами для високої швидкодії `JOIN` та каскадних операцій.

### 5. Контракти API та OpenAPI 3.1
- Інтеграційний тест `OpenApiExportIT` автоматично генерує та зберігає актуальну схему у `docs/openapi.json` під час збірки.
- Swagger UI доступний за адресою `http://localhost:8080/swagger-ui.html` у профілях `local`/`test` та безпечно вимкнений у `prod`.

---

## Швидкий старт та локальний запуск

### Попередні вимоги
- **Java:** JDK 21 (рекомендовано Eclipse Temurin 21)
- **Node.js:** v20+ та npm
- **Docker:** встановлений та запущений Docker Daemon

### 1. Запуск через Docker (БД + Бекенд одним скриптом)
```bash
# Запуск усього стеку (PostgreSQL 17 + Spring Boot Backend):
./run-docker.sh up

# Або напряму через Docker Compose:
docker compose up -d --build
```
- **Backend API:** `http://localhost:8080`
- **PostgreSQL 17:** `localhost:5432` (`readmania` / `readmania`)
- **Статус / Логи / Зупинка:**
  ```bash
  ./run-docker.sh status   # перевірка стану контейнерів
  ./run-docker.sh logs     # перегляд логів у реальному часі
  ./run-docker.sh down     # зупинка контейнерів
  ```

### 2. Запуск бекенду локально (без Docker для бекенду)
```bash
# Підняття лише бази даних у Docker:
docker compose up -d db

cd backend

# Стандартний запуск:
./mvnw spring-boot:run

# Запуск із профілем local (активує тестові дані seed та Swagger/OpenAPI):
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
