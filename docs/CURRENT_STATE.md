# Поточний стан системи ReadMania та звіт про працюючий функціонал

Документ містить систематизований опис усіх реалізованих, налаштованих та верифікованих компонентів проєкту **ReadMania** станом на поточний момент. Звіт призначений для аналізу архітектурної готовності, оцінки наявних можливостей та прийняття рішень щодо пріоритетів подальшої розробки для досягнення максимальної корисності та надійності системи.

---

## 1. Загальний огляд реалізованого функціоналу

Наразі повністю завершено етап створення базового інженерного фундаменту (Core Architecture & Infrastructure) та реалізовано перші три критичні бізнес-зрізи: автентифікацію, публічний та менеджерський каталог, а також підсистему кошика, розрахунку бонусів, оформлення та життєвого циклу замовлень.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ГОТОВНІСТЬ КОМПОНЕНТІВ                          │
├───────────────────────────────┬──────────────┬─────────────────────────┤
│ Інфраструктура та Docker      │ ГОТОВО       │ 100% (DB + App + CI)    │
│ Схема БД (Flyway V1–V9 + Seed)│ ГОТОВО       │ 100% (Schema + Indexes) │
│ Безпека та Пошта (Security)   │ ГОТОВО       │ 100% (RBAC, Mail, 401/403)│
│ CSRF-захист для SPA (Security)│ ГОТОВО       │ 100% (Cookie + Header)  │
│ Планувальник (Scheduling/Cron)│ ГОТОВО       │ 100% (@Scheduled, Auto) │
│ Ініціалізація Менеджера       │ ГОТОВО       │ 100% (Bootstrap Runner) │
│ Обробка помилок (RFC 9457)    │ ГОТОВО       │ 100% (Problem Details)  │
│ Контракти API (.http + Doc)   │ ГОТОВО       │ 100% (Auth/Cat/Orders)  │
│ OpenAPI Схема (docs/openapi)  │ ГОТОВО       │ 100% (OpenAPI 3.1 JSON) │
│ Документація (ERD/Decisions)  │ ГОТОВО       │ 100% (ERD + Architectural)│
│ JPA Сутності та Репозиторії   │ ГОТОВО       │ 100% (Entities + Spec)  │
│ Автентифікація, Сесії, Профіль│ ГОТОВО       │ 100% (Auth, M6, G2, G3) │
│ Каталог, Обкладинки, CSV (M1) │ ГОТОВО       │ 100% (G1, M1, M2, A.4)  │
│ Кошик та Розрахунки (C1, C8)  │ ГОТОВО       │ 100% (Cart/Preview)     │
│ Оформлення Замовлень (C2, C10)│ ГОТОВО       │ 100% (Regular/Gift/A.10)│
│ Життєвий цикл та Менеджер (M3)│ ГОТОВО       │ 100% (4 Statuses FSM/A.1)│
│ Лояльність та Виклики (C7, C8)│ ГОТОВО       │ 100% (Bonus/Challenges) │
│ Оцінки, Виклики М4, Подарунки │ ГОТОВО       │ 100% (C6, M4, C9)       │
│ Фронтенд (UI компоненти)      │ В ОЧІКУВАННІ │ Базова структура Vite   │
└───────────────────────────────┴──────────────┴─────────────────────────┘
```

---

## 2. Працюючий функціонал за підсистемами

### 2.1. Інфраструктура, розгортання та CI/CD
1. **Multi-stage Dockerfile (`backend/Dockerfile`):**
   - Побудований на базі Eclipse Temurin JDK 21 (builder) та JRE 21 (runtime).
   - Запуск застосунку виконується від непривілейованого системного користувача `spring:spring` для захисту хоста.
   - Вбудована перевірка здоров'я контейнера (`HEALTHCHECK`).
2. **Оркестрація Docker Compose (`docker-compose.yml`):**
   - Контейнер `db` (PostgreSQL 17) з перевіркою `pg_isready` та збереженням даних у volume `pgdata`.
   - Контейнер `backend` із залежністю від здоров'я БД (`condition: service_healthy`) та конфігурацією профілю `local`.
3. **Автоматизований скрипт керування (`run-docker.sh`):**
   - Підтримує команди `up`, `down`, `restart`, `logs`, `status`, `clean`.
4. **Конфігураційні профілі Spring Boot:**
   - `default` (`application.yml`): базові налаштування, валідація Hibernate (`ddl-auto: validate`), захист сесійних cookie (`HttpOnly`, `SameSite=Lax`).
   - `local` (`application-local.yml`): автоматичне підтягування міграцій `R__dev_seed.sql` та увімкнення документації Swagger.
   - `prod` (`application-prod.yml`): примусовий `Secure` прапорець для cookie, підтримка проксі (`server.forward-headers-strategy=framework`), мінімальний Actuator без деталей, вимкнений Swagger.
   - `test` (`application-test.yml`): оптимізований профіль для тестів із Flyway та вимкненими сторонніми сервісами.
5. **Неперервна інтеграція (GitHub Actions CI):**
   - Робочий процес `.github/workflows/backend-ci.yml` запускається на `push` та `pull_request` у `main` при зміні файлів `backend/**`.
   - Використовує Temurin JDK 21, кеш Maven, виконуваний біт `mvnw` та виконує `./mvnw -B verify` із запуском Testcontainers (PostgreSQL 17).

---

### 2.2. База даних та міграції (PostgreSQL 17 + Flyway)
Реалізовано 9 послідовних версій міграцій та 1 скрипт локального наповнення:

1. **`V1__init.sql`** — Базова ініціалізація схеми.
2. **`V2__users.sql` (Користувачі та безпека):**
   - Таблиця `users`: поля `email`, `password_hash`, `role` (`USER`, `MANAGER`), `status` (`ACTIVE`, `BLOCKED`), `bonus_balance` (`CHECK >= 0`), `last_login_at`.
   - Унікальний регістронезалежний індекс `idx_users_email_lower` на `lower(email)`.
   - Частковий індекс `idx_users_active_last_login` для швидкого пошуку неактивних акаунтів (M5).
3. **`V3__catalog.sql` (Каталог, автори, обкладинки):**
   - `genres`: довідник жанрів (`ON DELETE RESTRICT` у книгах).
   - `authors` та проміжна таблиця `book_authors`: нормалізація у 3-тю нормальну форму (3НФ) для підтримки співіснування кількох авторів.
   - `books`: м'яке видалення `deleted_at`, оптимістичне блокування `version`, частковий унікальний індекс ISBN `idx_books_isbn_active` лише для не видалених книг (`WHERE deleted_at IS NULL`).
   - `book_covers`: виділена таблиця з типом `bytea` для збереження картинок у БД без уповільнення вибірки списків книг та забезпечення цілісності `pg_dump`.
4. **`V4__cart_institutions.sql` (Кошик та благодійні заклади):**
   - `cart_items`: складений первинний ключ `(user_id, book_id)`, обмеження кількості `quantity > 0`, каскадне очищення при видаленні користувача.
   - `institutions`: довідник перевірених закладів для благодійних подарунків (C10).
5. **`V5__orders.sql` (Замовлення, позиції та аудит):**
   - `orders`: підтримка особистих і благодійних (`GIFT`) замовлень; поля `type`, `status`, `delivery_method`, `payment_method`, знімок контактів одержувача.
   - Суворі розрахункові обмеження на рівні БД:
     - `chk_orders_discount_calc`: `discount_amount = points_used * 10`
     - `chk_orders_total_calc`: `total_to_pay = items_total - discount_amount`
     - `chk_orders_discount_limit`: `discount_amount * 10 <= items_total * 3` (знижка не більше 30%)
     - `chk_orders_gift_institution`: для `GIFT` обов'язкова наявність `institution_name`
   - `order_items`: знімки ціни `price_snapshot` та назви `title_snapshot`, що гарантує збереження фінансової історії навіть при зміні або видаленні книги/користувача.
   - `order_status_history`: хронологія зміни статусів замовлень для аудиту та налагодження.
6. **`V6__loyalty.sql` (Програма лояльності та оцінки):**
   - `bonus_transactions`: журнал операцій з балами (позитивні/негативні суми), типи операцій (`ORDER_REWARD`, `RATING_REWARD`, `CHALLENGE_REWARD`, `SPENT`, `REFUND`).
   - Часткові унікальні індекси від повторних нарахувань (`idx_bonus_unique_order_reward`, `idx_bonus_unique_challenge_reward`).
   - `ratings`: оцінки 1–5, унікальність пари `(user_id, book_id)`.
7. **`V7__challenges.sql` (Читацькі виклики):**
   - `challenges`: визначення викликів, дедлайнів, вимог до кількості та жанру.
   - `challenge_completions`: фіксація завершення викликів користувачами. Прогрес розраховується динамічним SQL-запитом без збереження проміжних лічильників.
8. **`V8__gift_log.sql` (Журнал благодійності):**
   - Плаский журнал `gift_log` з унікальністю `(order_id, book_title)` для звітності перед закладами.
9. **`V9__schema_fixes.sql` / `V9__srs_alignment.sql` (Узгодження зі специфікацією SRS/UX, Патчі 1 та 2):**
   - Зведення статусів замовлень та історії до 4 значень: `AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED` (А.1). Проміжні статуси `PROCESSING`, `ASSEMBLED`, `DELIVERING` мігровано у `PAID`.
   - Видавництво та рік видання (А.4): додано колонки `publisher VARCHAR(255) NOT NULL DEFAULT 'Невідоме видавництво'` та `publication_year INT NOT NULL DEFAULT 2024 CHECK (publication_year BETWEEN 1450 AND 2100)` з індексом `idx_books_publication_year`.
   - Контактна особа закладу (А.10): додано `orders.institution_contact_person VARCHAR(255)` та оновлено `chk_orders_gift_institution` (для `GIFT` обов'язкова наявність і назви, і контактної особи).
   - Захист від brute-force: `users.failed_login_attempts` та `users.locked_until` на 15 хв після 5 спроб (G3).
   - Відновлення пароля: таблиця `password_reset_tokens` з одноразовими токенами на 1 годину (G3).
   - Розблокування облікових записів: таблиця `unblock_requests` з обґрунтуванням та модерацією менеджера (M6, M7).
   - Типи закладів: `institutions.type` (`ORPHANAGE`, `BOARDING_SCHOOL`, `LIBRARY`) (M8, M12).
   - Менеджерські вибірки та архівування: часткові індекси неоплачених (`idx_orders_unpaid_awaiting`) та архівних (`idx_orders_archived`) замовлень; зміна зв'язку `gift_log.order_id` на `ON DELETE SET NULL` для збереження журналу після повного видалення старих замовлень (M3, 7.2.2.6, 7.2.2.7).
10. **`R__dev_seed.sql` (Тестові дані для `local`):**
    - Заповнює тестові жанри, авторів, книги з реальними видавництвами, роками видання та залишками на складі, типізовані заклади, замовлення з 4 актуальними статусами та активні виклики.
11. **Індексація зовнішніх ключів (FK):**
    - 100% колонок зовнішніх ключів покриті явними індексами (вимога D7), що підтверджено тестом системного каталогу `pg_indexes`.

---

### 2.3. Безпека, CSRF та Поштовий сервіс (Spring Security + Mail)
1. **Архітектура Default Deny:**
   - Будь-який ендпоінт під префіксом `/api/**` за замовчуванням вимагає авторизації, якщо він не відкритий явно.
2. **Публічні ендпоінти:**
   - Дозволено без авторизації: `/api/auth/register`, `/api/auth/login`, `/api/auth/password-reset/**`, `/api/auth/unblock-request`, `/api/catalog/**`, `/api/institutions/**`, `/api/gift-sets/**`, `/api/challenges/**`, `/actuator/health`.
3. **Серверне розмежування ролей (RBAC):**
   - Усі маршрути `/api/manager/**` строго обмежені роллю `MANAGER`. Звичайні користувачі (`USER`) отримують відмову в доступі (`403 Forbidden`).
4. **CSRF-захист через Cookie для SPA:**
   - Налаштовано `CookieCsrfTokenRepository.withHttpOnlyFalse()` та `SpaCsrfTokenRequestHandler` разом із `CsrfCookieFilter`.
   - Захист вимагає передачу заголовка `X-XSRF-TOKEN` у запитах, що змінюють стан (`POST`, `PUT`, `PATCH`, `DELETE`).
5. **Типізовані відповіді Problem Details (RFC 9457):**
   - `ProblemDetailsAuthenticationEntryPoint`: перехоплює неавторизований доступ і повертає `401 Unauthorized` з коректним JSON-тілом.
   - `ProblemDetailsAccessDeniedHandler`: перехоплює брак прав і повертає `403 Forbidden` з `type = urn:readmania:error:access-denied`.
   - `AccountStatusFilter` та `AuthService.login`: перевіряють статус `BLOCKED` і повертають `403 Forbidden` з `type = urn:readmania:error:account-blocked` та полями `blockReason` і `blockedAt`.
   - Захист від brute-force у `AuthService.login`: блокування на 15 хв після 5 спроб повертає `403 Forbidden` з `type = urn:readmania:error:login-rate-limited` та полем `retryAfter`.
   - Конфлікт станів замовлення: `OrderStatusConflictException` повертає `409 Conflict` з `type = urn:readmania:error:order-status-conflict` та полем `currentStatus`.
6. **Шифрування паролів та безпека профілю:**
   - Налаштовано бін `BCryptPasswordEncoder` для безпечного хешування паролів.
   - Ендпоінти оновлення даних користувача (`/api/account`, `/api/profile`) вимагають обов'язкової верифікації `currentPassword`, перевіряють унікальність `lower(newEmail)` (`409 Conflict`) та оновлюють сесію.
7. **Поштовий сервіс (`NotificationService`):**
   - Підключено `spring-boot-starter-mail` та увімкнено `@EnableAsync`.
   - `SmtpNotificationService` (`@Profile("prod")`) на базі `JavaMailSender` для реальної відправки листів.
   - `LoggingNotificationService` (`@Profile({"local", "test", "default"})`) для логування та збереження повідомлень у пам'яті для верифікації в тестах.
   - Асинхронні сповіщення: нові замовлення менеджерам, зміна статусів замовлень, дострокове завершення викликів, одноразові посилання відновлення пароля (1 година), блокування/розблокування акаунтів.
8. **Ініціалізація першого менеджера при старті (`ManagerBootstrapInitializer`):**
   - Компонент `ApplicationRunner`, що зчитує змінні `APP_INITIAL_MANAGER_EMAIL` та `APP_INITIAL_MANAGER_PASSWORD` (властивості `app.initial-manager.email`, `app.initial-manager.password`) та створює менеджера у разі його відсутності в БД.
9. **Планувальник автоматичного завершення викликів (@EnableScheduling):**
   - Щодванадцять секунд / щохвилини перевіряє активні читацькі виклики з простроченим дедлайном і переводить їх у стан `COMPLETED`.
10. **Апеляція на розблокування (`POST /api/auth/unblock-request`):**
    - Публічний ендпоінт, що перевіряє пароль заблокованого користувача і створює/оновлює запис у `unblock_requests`.
11. **Сесійні cookie та мережевий захист:**
    - `HttpOnly = true`, `SameSite = Lax`, `Secure = true` (у prod).
    - Захист від SQL-ін'єкцій на рівні JPA та відсутність динамічної конкатенації рядків.

---

### 2.4. Глобальна обробка помилок (`GlobalExceptionHandler`)
Уніфіковано всі відповіді про помилки під стандарт RFC 9457 (Problem Details) із єдиною структурою (`status`, `title`, `detail`, `instance`, `errorId`, `errors`):

1. **Доменна ієрархія винятків (`ua.readmania.common`):**
   - `NotFoundException` → статус `404 Not Found`.
   - `ConflictException` → статус `409 Conflict`.
   - `BusinessRuleException` → статус `400 Bad Request` або `409 Conflict`.
2. **Конфлікти даних та паралелізму:**
   - Оптимістичне блокування (`ObjectOptimisticLockingFailureException`) → `409 Conflict`.
   - Порушення унікальності або цілісності (`DataIntegrityViolationException`) → `409 Conflict`.
3. **Помилки валідації:**
   - Порушення DTO `@Valid` (`MethodArgumentNotValidException`) → `400 Bad Request` з масивом `errors: [{field, message}]`.
   - Порушення параметрів запитів (`ConstraintViolationException`, `MethodArgumentTypeMismatchException`) → `400 Bad Request`.
4. **Стандартні винятки Spring MVC:**
   - Непідтримуваний HTTP метод (`HttpRequestMethodNotSupportedException`) → `405 Method Not Allowed`.
   - Непідтримуваний тип контенту (`HttpMediaTypeNotSupportedException`) → `415 Unsupported Media Type`.
   - Відсутній обов'язковий параметр (`MissingServletRequestParameterException`) → `400 Bad Request`.
5. **Захисний екран (Safety Net для невідомих винятків):**
   - Будь-який непередбачений виняток перетворюється на статус `500 Internal Server Error`.
   - Генерується унікальний ідентифікатор `errorId` (UUID), який повертається клієнту та записується в логи.
   - Запобігання витоку внутрішніх деталей (стектрейси, SQL-запити, назви таблиць ніколи не віддаються назовні).
   - Винятки безпеки (`AccessDeniedException`, `AuthenticationException`) явно прокидаються далі, щоб уникнути помилкового перетворення 401/403 на 500.
6. **Сумісність із Single Page Application (SPA):**
   - Статус `404 Not Found` у форматі JSON повертається лише для запитів `/api/**`. Невідомі не-API маршрути скеровуються на `index.html` для підтримки клієнтського роутингу.

---

### 2.5. Контракти API та специфікація HTTP-клієнта
Створено виконувані `.http`-специфікації у каталозі `backend/http/`:
- `auth.http`: реєстрація, логін, отримання поточного профілю, логаут, обробка помилок валідації (400) та конфліктів email (409).
- `catalog.http`: список книг, фільтрація за жанром, пошук, пагінація, деталі книги, отримання обкладинки, CRUD операції менеджера (401/403 перевірки).
- `orders.http`: розрахунок вартості замовлення зі знижкою балами (до 30%), створення замовлення, оформлення благодійного замовлення (`GIFT`), перегляд історії та зміна статусів менеджером.
- `http-client.env.json` та `http-client.private.env.json.example`: середовища для запуску запитів в IntelliJ IDEA із збереженням сесійних cookie.

---

### 2.6. Доменна модель (JPA Entities) та Репозиторії
Повністю реалізовано 100% сумісність Java-класів зі схемою PostgreSQL 17 при активному `ddl-auto: validate`:
1. **Користувачі та безпека (`ua.readmania.domain.user`):**
   - `User`: статус (`ACTIVE`/`BLOCKED`), ролі (`USER`/`MANAGER`), захист від brute-force (`failedLoginAttempts`, `lockedUntil`), причини блокування (`blockedAt`, `blockReason`), баланс бонусів, дати реєстрації та останнього входу.
   - `PasswordResetToken`: одноразові токени з терміном дії 1 година.
   - `UnblockRequest`: запити на розблокування зі статусами `PENDING`/`APPROVED`/`REJECTED`, обґрунтуванням та резолюцією менеджера.
2. **Каталог (`ua.readmania.domain.catalog`):**
   - `Genre`, `Author` та `Book`: нормалізація у 3НФ (`@ManyToMany Set<Author>`), поля `publisher` та `publicationYear` (A.4), оптимістичне блокування залишків на складі (`@Version`), автоматичне виключення видалених записів (`@SQLRestriction("deleted_at IS NULL")`).
   - `BookCover`: виділена сутність для збереження бінарних даних обкладинки (`byte[]`) з `@MapsId` для запобігання зайвому навантаженню під час вибірки каталогу.
   - `BookRepository` та `BookSpecifications`: динамічна фільтрація каталогу G1 (пошук за назвою чи автором, жанром, автором, діапазоном ціни, наявністю `stock > 0`) через `JpaSpecificationExecutor` та оптимізоване завантаження через `@EntityGraph(attributePaths = {"genre", "authors"})` без проблеми N+1.
3. **Кошик та заклади (`ua.readmania.domain.cart`, `ua.readmania.domain.institution`):**
   - `CartItem`: композитний первинний ключ `@EmbeddedId CartItemId` з прив'язкою до `User` та `Book`.
   - `Institution`: верифіковані благодійні заклади з типізацією `InstitutionType` (`ORPHANAGE`, `BOARDING_SCHOOL`, `LIBRARY`).
4. **Замовлення (`ua.readmania.domain.order`):**
   - `Order`: розрахунки сум, знижок та бонусів, типи `REGULAR`/`GIFT`, знімок контакту закладу `institutionContactPerson` (A.10), статуси `OrderStatus` (4 значення: `AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED` згідно з A.1), способи оплати та доставки, версіонування `@Version`.
   - `OrderItem`: незмінні знімки вартості та назви (`titleSnapshot`, `priceSnapshot`).
   - `OrderStatusHistory`: аудит хронології зміни статусів замовлень.
5. **Лояльність та благодійність (`ua.readmania.domain.loyalty`, `ua.readmania.domain.challenge`, `ua.readmania.domain.gift`):**
   - `BonusTransaction`: типи операцій `ORDER_REWARD`, `RATING_REWARD`, `CHALLENGE_REWARD`, `SPENT`, `REFUND`.
   - `Rating`: оцінки 1–5 зі зв'язками на книгу та замовлення.
   - `Challenge`, `ChallengeCompletion`: читацькі виклики та фіксація їх виконання.
   - `GiftLog`: журнал подарованих книг для звітності.

---

### 2.7. Автоматизована верифікація та тести
У проєкті налаштовано та успішно виконується повний набір із **112 автоматизованих тестів**:
1. **`AuthIntegrationTest` (17 тестів на Testcontainers PostgreSQL 17):**
   - Реєстрація, автентифікація, керування сесіями, захист від brute-force з `login-rate-limited`, миттєве блокування активних сесій (M6) з `account-blocked`.
   - Запит на відновлення пароля без витоку токена з відправкою листа через `NotificationService`.
   - Публічне подання апеляції на розблокування (`POST /api/auth/unblock-request`) з перевіркою пароля та збереженням у `unblock_requests`.
   - Редагування профілю (`PUT /api/account`, `PUT /api/profile`): обов'язкова перевірка `currentPassword`, зміна email із контролем унікальності (`409 Conflict`) та оновленням `SecurityContext`.
2. **`OrderIntegrationTest` (11 тестів на Testcontainers PostgreSQL 17):**
   - Керування кошиком (C1.1–C1.3): додавання, зміна кількості в межах залишку, контроль лімітів та прапорець `available`.
   - Попередній розрахунок замовлення (C8): перевірка `itemsTotal`, максимальної знижки бонусами до 30% та `totalToPay`.
   - Оформлення замовлення (C2, A.10): списання залишків на складі, списання бонусів (`SPENT`), фіксація фінансових знімків у `order_items`, очищення кошика, аудит статусів та збереження `institutionContactPerson`.
   - Благодійне замовлення (`GIFT` C10): валідація закладу й контактної особи, примусовий `TRANSFER` та відсутність способу доставки.
   - Скасування покупцем (C3): відновлення залишків книг на складі та повернення бонусів (`REFUND`) для неоплачених замовлень; 409 Conflict при неприпустимих статусах.
   - Машина станів менеджера (M3, Таблиця 3 SRS / A.1): валідація 4 статусів (`AWAITING_PAYMENT` -> `PAID`/`DELIVERED`/`CANCELLED`, `PAID` -> `DELIVERED`/`CANCELLED`) та захист від конкурентних змін через `expectedStatus` (409 Conflict).
   - Побічні дії при доставці (`DELIVERED`): нарахування бонусів (C8), динамічне зарахування читацьких викликів (C7) та записи у журнал благодійності `gift_log` (M9).
   - Спеціальні масові дії менеджера за списком ID: скасування неоплачених замовлень > 7 днів та очищення архіву > 3 років із валідацією списку та 409 Conflict при невідповідності критеріїв.
   - Збагачення `OrderItemResponse` полем `userRating`.
   - Багатопотокова конкуренція: одночасне замовлення останнього примірника двома користувачами (Optimistic Locking -> 409 Conflict).
3. **`CatalogIntegrationTest` (18 тестів на Testcontainers PostgreSQL 17):**
   - Публічний каталог (G1, A.4): пагінація, фільтри за жанром, автором, ціною, залишками (`stockMin`, `stockMax`), наявністю, сортування за роком видання `publicationYear`, пошук, кешування обкладинок.
   - Агрегація середніх оцінок та кількості відгуків у реальному часі.
   - Керування виданнями (M1.1, M1.2, M1.3, A.4): створення й редагування з полями `publisher` та `publicationYear`, прапорець `canDelete`, захист ролями (401/403), перевірка унікальності активного ISBN (409), оновлення та м'яке видалення.
   - Створення авторів `POST /api/manager/authors` та запобігання дублікатам (409 Conflict).
   - Керування жанрами: створення, оновлення, лічильник книг `booksCount`, блокування видалення при наявності прив'язаних книг (RESTRICT 409).
   - Завантаження бінарних обкладинок через multipart/form-data.
   - CSV-імпорт (M2): покрокова валідація рядків (включаючи рік і видавництво), дублікати ISBN у файлі та базі, автоматичне створення сутностей та звіт.
4. **`Slice4GamificationAndGiftSetIT` (12 тестів на Testcontainers PostgreSQL 17):**
   - Оцінювання книг (C6), захист приватності публічних оцінок (`PublicRatingResponse` без `userId`), читацькі виклики менеджера (M4.1–M4.3) з достроковим завершенням та поштовими сповіщеннями, генератор подарункового набору (C9).
5. **`DomainRepositoryIT` (6 тестів на Testcontainers PostgreSQL 17):**
   - Збереження та вибірка користувачів, токенів та запитів розблокування.
   - Робота каталогу, зв'язок 3НФ, `publisher` та `publicationYear`, оптимістичне блокування `@Version` та ізольовані обкладинки `BookCover`.
   - Динамічна фільтрація `BookSpecifications`, композитний ключ кошика `CartItem`.
   - Замовлення з `institutionContactPerson`, знімки позицій та аудит статусів.
   - Програма лояльності, читацькі виклики та звітність `gift_log`.
6. **`SchemaIT` (18 тестів на Testcontainers PostgreSQL 17):**
   - Перевірка валідності всіх міграцій Flyway (V1–V9) на чистій базі.
   - Валідація CHECK-обмеження 4 статусів замовлень (A.1).
   - Валідація діапазону років видання (1450–2100) та наявності індексу `idx_books_publication_year` (A.4).
   - Валідація обов'язковості `institution_contact_person` для замовлень `GIFT` (A.10).
   - Регістронезалежність email (`test@example.com` vs `TEST@EXAMPLE.COM`).
   - Часткова унікальність ISBN для активних книг та дозвіл дублів для видалених.
   - Валідація діапазону оцінок (1–5) та залишків (`stock >= 0`).
   - Контроль обмеження знижки (не більше 30%).
   - Поведінка зв'язків: `RESTRICT` для жанрів з книгами, `SET NULL` для замовлень при видаленні користувача або книги.
   - Захист від повторного нарахування бонусів.
   - Захист від brute-force, одноразові токени скидання пароля, запити розблокування, типи закладів, збереження `gift_log`.
   - Автоматична перевірка наявності індексів на всіх FK-колонках схеми (D7).
7. **`GlobalExceptionHandlerTest` (18 тестів):**
   - Перевірка коректного мапінгу статусів 400, 401, 403, 404, 405, 409, 415, 500, типізованих URN (`access-denied`, `account-blocked`, `login-rate-limited`, `order-status-conflict`), `errorId` та масиву `errors`.
8. **`SecurityConfigTest` та `LocalProfileSecurityConfigTest` (11 тестів):**
   - Перевірка Default Deny, RBAC, відкритих шляхів, BCrypt шифрування та профілів.
9. **`OpenApiExportIT` (1 тест):**
   - Перевірка генерації OpenAPI 3.1 специфікації за адресою `/v3/api-docs` та збереження відформатованого файлу у `docs/openapi.json`.

---

## 3. Матриця готовності за вимогами ТЗ та SRS

| Вимога | Суть вимоги | Стан реалізації | Де реалізовано / підтверджено |
|---|---|---|---|
| **D2** | Захист від несанкціонованого доступу, HTTPS за проксі, захист від SQLi | **Повністю закрито** | `SecurityConfig`, `application-prod.yml`, JPA |
| **D3** | Паролі лише через BCrypt, без збереження у відкритому вигляді | **Повністю закрито** | `SecurityConfig.passwordEncoder()`, `V2__users.sql` |
| **D7** | Обов'язкова індексація всіх колонок зовнішніх ключів (FK) | **Повністю закрито** | Міграції `V2–V9`, тест `SchemaIT.shouldHaveIndexesOnAllForeignKeyColumns` |
| **G3** | Захист від brute-force (15 хв після 5 спроб, `login-rate-limited`), скидання пароля через email без витоку токена | **Повністю закрито** | `V9`, `AuthService`, `NotificationService`, `AuthIntegrationTest` |
| **U1** | Серверне розмежування доступу за ролями (/api/manager/**) | **Повністю закрито** | `SecurityConfig.filterChain()`, `SecurityConfigTest`, `ProblemDetailsAccessDeniedHandler` |
| **U2** | Серверна валідація DTO (@Valid) та єдина структура помилок | **Повністю закрито** | `GlobalExceptionHandler`, модель `ValidationError` |
| **U3** | Формат помилок валідації `errors[{field, message}]` | **Повністю закрито** | `GlobalExceptionHandler.handleMethodArgumentNotValid` |
| **R3** | Інформативні типізовані відповіді Problem Details (RFC 9457) | **Повністю закрито** | `GlobalExceptionHandler`, `access-denied`, `account-blocked`, `login-rate-limited`, `order-status-conflict` |
| **M1** | М'яке видалення книг (збереження історії замовлень і аналітики), автори, жанри | **Повністю закрито** | `V3__catalog.sql` (`deleted_at`), `V5__orders.sql`, `ManagerCatalogService`, `POST /api/manager/authors` |
| **M3** | Життєвий цикл замовлень (Таблиця 3 SRS, 4 статуси), масові дії за списком ID | **Повністю закрито** | `ManagerOrderService`, `V9`, `OrderIntegrationTest` |
| **M5** | Анонімізація неактивних акаунтів та замовлень | **Схема + Репозиторій** | `V2__users.sql` (частковий індекс), `V5__orders.sql` (SET NULL) |
| **M6/M7**| Блокування акаунта (`account-blocked`) та розгляд апеляцій на розблокування (`POST /api/auth/unblock-request`) | **Повністю закрито** | `AccountStatusFilter`, `AuthService`, `unblock_requests`, `AuthIntegrationTest` |
| **M8/M12**| Типізовані благодійні заклади (дитячий будинок, інтернат, бібліотека) | **Повністю закрито** | `V9`, `InstitutionService`, `R__dev_seed.sql` |
| **C10**| Замовлення в дар для благодійних закладів (`GIFT`) з контактною особою | **Повністю закрито** | `V4`, `V5`, `V8`, `V9`, `OrderService`, `OrderIntegrationTest` |
| **A.1** | Зведення статусів замовлень до 4 значень (`AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED`) | **Повністю закрито** | `V9__schema_fixes.sql`, `OrderStatus`, `OrderService`, `ManagerOrderService` |
| **A.4** | Видавництво та рік видання книги з індексом сортування `idx_books_publication_year` | **Повністю закрито** | `V9__schema_fixes.sql`, `Book`, `CatalogService`, `CatalogIntegrationTest` |
| **A.10**| Знімок контактної особи закладу (`institutionContactPerson`) у замовленні | **Повністю закрито** | `V9__schema_fixes.sql`, `Order`, `OrderService`, `OrderIntegrationTest` |
| **CSRF**| CSRF-захист cookie/header для SPA (`CookieCsrfTokenRepository` + `SpaCsrfTokenRequestHandler`) | **Повністю закрито** | `SecurityConfig`, `CsrfCookieFilter`, `SpaCsrfTokenRequestHandler` |
| **Cron**| Автоматичне завершення прострочених читацьких викликів | **Повністю закрито** | `ReadmaniaBackendApplication` (@EnableScheduling), `ChallengeService` (@Scheduled) |
| **Boot**| Ініціалізація початкового менеджера через змінні середовища | **Повністю закрито** | `ManagerBootstrapInitializer` (`app.initial-manager.*`) |
| **OpenAPI**| Експорт актуальної схеми OpenAPI у `docs/openapi.json` | **Повністю закрито** | `springdoc-openapi`, `OpenApiExportIT`, `docs/openapi.json` |
| **Mail**| Поштовий сервіс сповіщень (`NotificationService`: SMTP / Logging) | **Повністю закрито** | `SmtpNotificationService`, `LoggingNotificationService`, `@EnableAsync` |
| **O2** | Діаграма сутностей і зв'язків (ERD) | **Повністю закрито** | `docs/ERD.md`, `docs/DATABASE_DECISIONS.md` |

---

## 4. Аналіз та рекомендації для подальшої розробки

З точки зору максимізації віддачі та забезпечення швидкого переходу до робочого бізнес-функціоналу, поточний стан розробки та наступні пріоритети структуровано за зрізами:

### Зрізи 1–4: Повний бекенд-функціонал — ЗАВЕРШЕНО (100%)
1. **Зріз 1 (Автентифікація, безпека, профіль, пошта):**
   - Повністю реалізовано: `User`, `PasswordResetToken`, `UnblockRequest`, `AuthService`, `UserService`, `NotificationService`, `AuthController`, `ProfileController`.
   - Захист від brute-force, типізовані Problem Details, безпечне відновлення пароля через email, подання апеляцій на розблокування, перевірка поточного пароля при зміні даних.
2. **Зріз 2 (Каталог, жанри, автори, обкладинки, CSV-імпорт):**
   - Повністю реалізовано: `Book`, `Genre`, `Author`, `BookCover`, `CatalogService`, `ManagerCatalogService`, `CsvImportService`, `CatalogController`, `ManagerCatalogController`.
   - Поля `publisher` та `publicationYear`, сортування, фільтрація за залишками `stockMin`/`stockMax`, динамічний прапорець `canDelete`, створення авторів, лічильник книг у жанрах `booksCount`, парсер CSV, бінарні обкладинки.
3. **Зріз 3 (Кошик, розрахунки, оформлення, життєвий цикл замовлень):**
   - Повністю реалізовано: `CartItem`, `Order`, `OrderItem`, `OrderStatusHistory`, `Institution`, `OrderService`, `ManagerOrderService`, `OrderController`, `ManagerOrderController`.
   - Знижки до 30%, FSM з 4 статусами (`AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED`), конфлікти статусів (409 Conflict з `currentStatus`), замовлення `GIFT` з `institutionContactPerson`, масові операції за списком ID (`BatchOrderActionRequest`), оцінки в позиціях замовлень `userRating`.
4. **Зріз 4 (Гейміфікація, лояльність, виклики, набори):**
   - Повністю реалізовано: `BonusTransaction`, `Rating`, `Challenge`, `ChallengeCompletion`, `GiftLog`, `LoyaltyService`, `ChallengeService`, `RatingService`, `GiftSetService`, `ChallengeController`, `ManagerChallengeController`, `RatingController`, `GiftSetController`, `InstitutionController`.
   - Нарахування балів, приватність оцінок (`PublicRatingResponse`), автоматичне завершення викликів планувальником (`@Scheduled`), генератор наборів.

### Зріз 5: Адміністрування M5–M12 та розширена аналітика — ЗАПЛАНОВАНО (Roadmap)
- `M5`: Періодична анонімізація неактивних акаунтів (понад 2 роки) із збереженням знеособленої історії замовлень.
- `M6/M7`: Панель менеджера для перегляду та модерації поданих запитів розблокування (`unblock_requests`) та ручного блокування.
- `M8`: CRUD-керування закладами-одержувачами для благодійних замовлень менеджером.
- `M9`: Звітність та журнал благодійних передач (`gift_log`).
- `M10`: Аналітичні звіти затребуваності книг/жанрів, залишків та рейтингів.

---

## 5. Корисні команди для контролю стану

```bash
# Перевірка локального стеку Docker:
./run-docker.sh up
./run-docker.sh status

# Запуск усіх автоматизованих тестів:
cd backend && ./mvnw test

# Повна верифікація збірки та валідація схеми Testcontainers:
cd backend && ./mvnw -B verify
```
