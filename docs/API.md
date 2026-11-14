# Специфікація REST API ReadMania

Цей документ є єдиним джерелом правди (Single Source of Truth) щодо контракту API між бекендом та фронтендом/зовнішніми клієнтами. Усі шляхи, структури DTO, параметри та коди відповідей відповідають поточній реалізації у Spring Boot бекенді.

---

## 1. Загальні домовленості та конвенції

- **Базовий шлях:** `/api`
- **Формат обміну даними:** `application/json; charset=UTF-8` (крім файлових операцій multipart та віддачі бінарних зображень).
- **Автентифікація, сесії та CSRF:**
  - Сесійна cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`, `Secure` у профілі `prod`).
  - CSRF-захист через cookie для SPA: `CookieCsrfTokenRepository` (`XSRF-TOKEN`, `HttpOnly=false`) та `SpaCsrfTokenRequestHandler`. Клієнт зобов'язаний передавати заголовок `X-XSRF-TOKEN` у запитах, що змінюють стан (`POST`, `PUT`, `PATCH`, `DELETE`).
  - Фронтенд-клієнт зобов'язаний надсилати всі запити з налаштуванням `credentials: 'include'`.
- **Ролі користувачів (RBAC):**
  - `GUEST` — неавторизований користувач (доступ до публічного каталогу, реєстрації, входу, списку закладів та перегляду викликів).
  - `USER` (`ROLE_USER`) — автентифікований покупець (керування кошиком, профілем, оформлення та перегляд власних замовлень, бонусна програма).
  - `MANAGER` (`ROLE_MANAGER`) — співробітник/адміністратор із доступом до маршрутів `/api/manager/**` (керування каталогом, зміна статусів замовлень, пакетний імпорт тощо).
- **Формат помилок (RFC 9457 Problem Details):**
  Усі помилки повертаються з заголовком `Content-Type: application/problem+json` та єдиною уніфікованою структурою:
  ```json
  {
    "type": "about:blank",
    "title": "Bad Request",
    "status": 400,
    "detail": "Помилка валідації вхідних даних",
    "instance": "/api/orders",
    "errors": [
      {
        "field": "deliveryMethod",
        "message": "Спосіб доставки є обов'язковим. Допустимі значення: COURIER, POST_OFFICE"
      }
    ]
  }
  ```
  Спеціалізовані типи помилок безпеки та бізнес-правил (`type` URI):
  - `urn:readmania:error:access-denied` — спроба доступу до ресурсу без відповідної ролі (`403 Forbidden`).
  - `urn:readmania:error:account-blocked` — доступ або спроба входу заблокованого користувача (`403 Forbidden`); додатково містить поля `blockReason` та `blockedAt`.
  - `urn:readmania:error:login-rate-limited` — блокування входу на 15 хв після 5 невдалих спроб (`403 Forbidden`); містить поле `retryAfter`.
  - `urn:readmania:error:order-status-conflict` — конфлікт зміни статусу замовлення або невідповідність `expectedStatus` / оптимістичного блокування (`409 Conflict`); містить поле `currentStatus`.
  
  Для внутрішніх помилок сервера (`500 Internal Server Error`) додатково повертається безпечний `errorId` для пошуку інциденту в логах:
  ```json
  {
    "type": "about:blank",
    "title": "Internal Server Error",
    "status": 500,
    "detail": "Сталася внутрішня помилка сервера. Зверніться до підтримки з ідентифікатором помилки.",
    "instance": "/api/catalog/books",
    "errorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }
  ```
- **Статуси HTTP:**
  - `200 OK` — успішний запит із поверненням даних.
  - `201 Created` — ресурс успішно створено.
  - `204 No Content` — успішна дія без повернення тіла (наприклад, видалення або очищення).
  - `400 Bad Request` — помилка валідації вхідних параметрів або порушення бізнес-правила.
  - `401 Unauthorized` — запит потребує автентифікації.
  - `403 Forbidden` — недостатньо прав або обліковий запис заблоковано (вимога M6).
  - `404 Not Found` — запитуваний ресурс не знайдено.
  - `409 Conflict` — конфлікт даних (дублікат ISBN/Email, конкурентна зміна статусу `expectedStatus`, оверселінг або зміна стану замовлення).
  - `500 Internal Server Error` — непередбачена помилка сервера.
- **Пагінація списків (Spring Data Page):**
  - Параметри запиту: `?page=0&size=10&sort=field,direction` (наприклад, `sort=price,desc` або `sort=createdAt,desc`).
  - Формат відповіді:
    ```json
    {
      "content": [ ... ],
      "page": {
        "size": 10,
        "number": 0,
        "totalElements": 42,
        "totalPages": 5
      }
    }
    ```
- **Формати та типи значень:**
  - Суми: число `NUMERIC(12,2)` з двома знаками після коми (наприклад `250.00`).
  - Час: рядок у форматі ISO-8601 UTC (наприклад `2026-11-13T18:30:00Z`).
  - Переліки (Enums):
    - Статуси замовлення: `AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED` (Таблиця 3 SRS / A.1)
    - Типи замовлення: `REGULAR`, `GIFT`
    - Спосіб доставки: `COURIER`, `POST_OFFICE`
    - Спосіб оплати: `ON_DELIVERY`, `TRANSFER`
    - Статус акаунта: `ACTIVE`, `BLOCKED`
    - Роль: `USER`, `MANAGER`
    - Тип благодійного закладу: `ORPHANAGE`, `BOARDING_SCHOOL`, `LIBRARY`
    - Тип бонусних транзакцій: `ORDER_REWARD`, `RATING_REWARD`, `CHALLENGE_REWARD`, `SPENT`, `REFUND`

---

## 2. Реалізовані ендпоінти API

### 2.1. Автентифікація та сесії (`/api/auth`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | `GUEST` | **Реєстрація нового користувача (G2)**<br>Тіло: `{ email, password, confirmPassword, fullName, phone, deliveryAddress }` | `201 Created` (`UserResponse`), `400`, `409 Conflict` (дублікат email) |
| `POST` | `/api/auth/login` | `GUEST` | **Вхід у систему (G3)**<br>Тіло: `{ email, password }`<br>Встановлює сесійну cookie `JSESSIONID`. Захист від brute-force (5 спроб → 15 хв блок, `login-rate-limited`). При блокуванні акаунта повертає `403` з `account-blocked`. | `200 OK` (`UserResponse`), `400`, `401`, `403 Forbidden` |
| `POST` | `/api/auth/logout` | `USER`, `MANAGER` | **Вихід із системи (C10)**<br>Інвалідує поточну сесію та очищає контекст безпеки. | `200 OK` `{ message: "..." }` |
| `GET` | `/api/auth/me` | `USER`, `MANAGER` | **Отримання профілю поточного автентифікованого користувача** | `200 OK` (`UserResponse`), `401` |
| `POST` | `/api/auth/password-reset/request` | `GUEST` | **Запит на скидання пароля (G3)**<br>Тіло: `{ email }`<br>Генерує одноразовий токен на 1 годину і надсилає посилання через `NotificationService`. Завжди повертає стандартне повідомлення без токена у відповіді. | `200 OK` `{ message: "Якщо обліковий запис існує, лист надіслано" }`, `400` |
| `POST` | `/api/auth/password-reset/confirm` | `GUEST` | **Підтвердження зміни пароля за токеном (G3)**<br>Тіло: `{ token, newPassword, confirmPassword }` | `200 OK` `{ message }`, `400`, `404` |
| `POST` | `/api/auth/unblock-request` | `GUEST` | **Подання апеляції на розблокування акаунта (M6, M7)**<br>Тіло: `{ email, password, reason }`<br>Перевіряє пароль заблокованого користувача, створює або оновлює запис у `unblock_requests` зі статусом `PENDING`. | `200 OK` `{ message, id, status, createdAt }`, `400`, `401`, `404` |

#### Модель `UserResponse`:
```json
{
  "id": 1,
  "email": "user@example.com",
  "fullName": "Тарас Шевченко",
  "phone": "+380501234567",
  "deliveryAddress": "м. Київ, вул. Хрещатик, 1",
  "role": "USER",
  "status": "ACTIVE",
  "blockReason": null,
  "blockedAt": null,
  "bonusBalance": 150,
  "createdAt": "2026-11-01T10:00:00Z",
  "lastLoginAt": "2026-11-13T12:00:00Z"
}
```

---

### 2.2. Профіль користувача (`/api/account`, `/api/profile`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/account` | `USER`, `MANAGER` | **Отримання контактних даних та інформації профілю (C5)** | `200 OK` (`UserResponse`), `401` |
| `PUT` | `/api/account`, `/api/profile` | `USER`, `MANAGER` | **Оновлення даних профілю, контактів, email або пароля (C5)**<br>Тіло: `{ currentPassword, email?, fullName?, phone?, deliveryAddress?, newPassword?, confirmPassword? }`<br>Поле `currentPassword` є обов'язковим для будь-якого редагування. При зміні email перевіряється унікальність `lower(email)` (`409 Conflict`) та оновлюється сесія. | `200 OK` (`UserResponse`), `400`, `401`, `409 Conflict` |
| `PUT` | `/api/account/password` | `USER`, `MANAGER` | **Зміна пароля (C5)**<br>Тіло: `{ currentPassword, newPassword, confirmPassword }`<br>Перевіряє коректність поточного пароля. | `200 OK` (`UserResponse`), `400`, `401` |

---

### 2.3. Публічний каталог (`/api/catalog`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/catalog/books` | Публічний | **Пошук та фільтрація книг (G1, A.4)**<br>Параметри запиту:<br>- `query` або `q`: пошуковий рядок за назвою/автором<br>- `genre` / `genreId`: назва або ID жанру<br>- `author` / `authorId`: ім'я або ID автора<br>- `priceMin`, `priceMax`: діапазон цін<br>- `stockMin`, `stockMax`: діапазон кількості на складі (`400 Bad Request`, якщо `stockMin > stockMax`)<br>- `inStock`: тільки наявні (`true`/`false`)<br>- `page`, `size`, `sort` (`publicationYear,desc`, `publicationYear,asc`, `price,asc`, `price,desc`, `title,asc`, `stock,desc`, `createdAt,desc`) | `200 OK` (`Page<BookSummaryResponse>`), `400` |
| `GET` | `/api/catalog/books/{id}` | Публічний | **Детальна сторінка книги (G1, A.4)**<br>Включає видавництво, рік видання, анотацію, авторів, жанр, залишок, динамічний прапорець `canDelete`, середню оцінку та кількість відгуків. | `200 OK` (`BookDetailsResponse`), `404` |
| `GET` | `/api/catalog/books/{id}/cover` | Публічний | **Отримання бінарного зображення обкладинки**<br>Повертає `image/jpeg` або `image/png` із заголовком кешування `Cache-Control: max-age=86400, public`. | `200 OK` (binary), `404` |
| `GET` | `/api/catalog/genres` | Публічний | **Список усіх доступних жанрів**<br>Включає кількість активних книг `booksCount` для блокування видалення непорожнього жанру на фронтенді. | `200 OK` (`List<GenreResponse>`) |
| `GET` | `/api/catalog/authors` | Публічний | **Список усіх авторів каталогу** | `200 OK` (`List<AuthorResponse>`) |

#### Модель `BookSummaryResponse`:
```json
{
  "id": 1,
  "title": "Кобзар",
  "publisher": "А-БА-БА-ГА-ЛА-МА-ГА",
  "publicationYear": 2024,
  "isbn": "978-966-00-1234-5",
  "price": 350.00,
  "stock": 15,
  "genre": { "id": 1, "name": "Поезія", "description": "Поетичні твори", "booksCount": 12 },
  "authors": [{ "id": 1, "name": "Тарас Шевченко" }],
  "averageRating": 4.9,
  "ratingsCount": 28,
  "hasCover": true,
  "coverUrl": "/api/catalog/books/1/cover"
}
```

#### Модель `BookDetailsResponse`:
```json
{
  "id": 1,
  "title": "Кобзар",
  "publisher": "А-БА-БА-ГА-ЛА-МА-ГА",
  "publicationYear": 2024,
  "isbn": "978-966-00-1234-5",
  "description": "Повне зібрання поетичних творів",
  "price": 350.00,
  "stock": 15,
  "canDelete": false,
  "genre": { "id": 1, "name": "Поезія", "description": "Поетичні твори", "booksCount": 12 },
  "authors": [{ "id": 1, "name": "Тарас Шевченко" }],
  "averageRating": 4.9,
  "ratingsCount": 28,
  "hasCover": true,
  "coverUrl": "/api/catalog/books/1/cover",
  "createdAt": "2026-11-01T10:00:00Z"
}
```

#### Модель `GenreResponse`:
```json
{
  "id": 1,
  "name": "Поезія",
  "description": "Поетичні твори",
  "booksCount": 12
}
```

#### Модель `AuthorResponse`:
```json
{
  "id": 1,
  "fullName": "Тарас Шевченко"
}
```

---

### 2.4. Кошик покупця (`/api/cart`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/cart` | `USER` | **Отримання вмісту кошика (C1)**<br>Автоматично перевіряє актуальну наявність та залишки (`available`, `stock`). | `200 OK` (`CartResponse`), `401` |
| `POST` | `/api/cart/items` | `USER` | **Додавання книги до кошика (C1.1)**<br>Тіло: `{ bookId, quantity }` (за замовчуванням `quantity = 1`). Не дозволяє перевищувати `stock`. | `200 OK` (`CartResponse`), `400`, `401`, `404` |
| `PUT` | `/api/cart/items/{bookId}` | `USER` | **Зміна кількості книги в кошику (C1.2)**<br>Тіло: `{ quantity }` (від 1 до поточного `stock`). | `200 OK` (`CartResponse`), `400`, `401`, `404` |
| `DELETE` | `/api/cart/items/{bookId}` | `USER` | **Вилучення окремої позиції з кошика (C1.3)** | `200 OK` (`CartResponse`), `401`, `404` |
| `DELETE` | `/api/cart` | `USER` | **Повне очищення кошика користувача** | `204 No Content`, `401` |

#### Модель `CartResponse`:
```json
{
  "items": [
    {
      "bookId": 1,
      "title": "Кобзар",
      "isbn": "978-966-00-1234-5",
      "price": 350.00,
      "quantity": 2,
      "available": true,
      "stock": 15,
      "subtotal": 700.00,
      "hasCover": true,
      "coverUrl": "/api/catalog/books/1/cover"
    }
  ],
  "totalItems": 2,
  "totalPrice": 700.00,
  "allAvailable": true
}
```

---

### 2.5. Замовлення та бонуси (`/api/orders`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `POST` | `/api/orders/preview` | `USER` | **Попередній розрахунок вартості та знижки (C8)**<br>Тіло: `{ bonusPoints, items?: [{ bookId, quantity }] }`<br>Якщо `items` не вказано, розраховує для поточного вмісту кошика. Перевіряє правило ліміту знижки до 30% ($\min(\text{balance}, \lfloor \frac{0.30 \times \text{total}}{10} \rfloor)$). | `200 OK` (`OrderPreviewResponse`), `400`, `401` |
| `POST` | `/api/orders` | `USER` | **Оформлення замовлення (C2, C10, A.10)**<br>Тіло: `{ type, deliveryMethod, paymentMethod, recipientName, recipientPhone, deliveryAddress, institutionId, institutionName, institutionContactPerson, pointsUsed, items?: [{ bookId, quantity }] }`<br>Транзакційно списує залишки книг (із захистом від оверселінгу через `@Version`), списує бонуси (`SPENT`), фіксує незмінні знімки цін (`priceSnapshot`) і очищає кошик. Для типу `GIFT` обов'язковими є `institutionName` та `institutionContactPerson`, а спосіб оплати фіксується як `TRANSFER`. При оформленні сповіщає менеджерів через `NotificationService`. | `201 Created` (`OrderDetailsResponse`), `400`, `401`, `409 Conflict` (недостатній залишок / оверселінг) |
| `GET` | `/api/orders` | `USER` | **Історія замовлень користувача (C4)**<br>Пагінація: `?page=0&size=10&sort=createdAt,desc`. | `200 OK` (`Page<OrderSummaryResponse>`), `401` |
| `GET` | `/api/orders/{id}` | `USER` | **Детальна інформація про власне замовлення (C4)** | `200 OK` (`OrderDetailsResponse`), `401`, `404` |
| `POST` | `/api/orders/{id}/cancel` | `USER` | **Скасування замовлення покупцем (C3, 7.2.2.4)**<br>Дозволено лише для власних замовлень зі статусом `AWAITING_PAYMENT` та оплатою `TRANSFER`. Повертає залишки на склад та нараховує повернення бонусів (`REFUND`). | `200 OK` (`OrderDetailsResponse`), `400`, `401`, `404` |

#### Модель `OrderPreviewResponse`:
```json
{
  "itemsTotal": 1000.00,
  "userBonusBalance": 50,
  "maxBonusPoints": 30,
  "pointsUsed": 30,
  "discountAmount": 300.00,
  "totalToPay": 700.00,
  "items": [
    {
      "bookId": 1,
      "title": "Кобзар",
      "price": 350.00,
      "quantity": 2,
      "subtotal": 700.00,
      "available": true
    }
  ]
}
```

#### Модель `OrderDetailsResponse`:
```json
{
  "id": 101,
  "userId": 1,
  "type": "REGULAR",
  "status": "AWAITING_PAYMENT",
  "deliveryMethod": "COURIER",
  "paymentMethod": "TRANSFER",
  "recipientName": "Тарас Шевченко",
  "recipientPhone": "+380501234567",
  "deliveryAddress": "м. Київ, вул. Хрещатик, 1",
  "institutionId": null,
  "institutionName": null,
  "institutionContactPerson": null,
  "itemsTotal": 700.00,
  "discountAmount": 100.00,
  "pointsUsed": 10,
  "totalToPay": 600.00,
  "createdAt": "2026-11-13T14:30:00Z",
  "statusChangedAt": "2026-11-13T14:30:00Z",
  "deliveredAt": null,
  "items": [
    {
      "id": 1,
      "bookId": 1,
      "titleSnapshot": "Кобзар",
      "priceSnapshot": 350.00,
      "quantity": 2,
      "subtotal": 700.00,
      "userRating": 5
    }
  ],
  "allowedNextStatuses": ["CANCELLED"]
}
```

#### Модель `OrderSummaryResponse`:
```json
{
  "id": 101,
  "userId": 1,
  "type": "REGULAR",
  "status": "AWAITING_PAYMENT",
  "deliveryMethod": "COURIER",
  "paymentMethod": "TRANSFER",
  "recipientName": "Тарас Шевченко",
  "recipientPhone": "+380501234567",
  "institutionName": null,
  "institutionContactPerson": null,
  "itemsTotal": 700.00,
  "discountAmount": 100.00,
  "pointsUsed": 10,
  "totalToPay": 600.00,
  "totalItems": 2,
  "createdAt": "2026-11-13T14:30:00Z",
  "deliveredAt": null
}
```

---

### 2.6. Програма лояльності (`/api/loyalty`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/loyalty/balance` або `/api/loyalty/transactions` | `USER` | **Отримання поточного балансу та історії бонусних транзакцій (C7)** | `200 OK` (`LoyaltyBalanceResponse`), `401` |

#### Модель `LoyaltyBalanceResponse`:
```json
{
  "bonusBalance": 120,
  "transactions": [
    {
      "id": 1,
      "amount": 25,
      "type": "ORDER_REWARD",
      "orderId": 100,
      "bookId": null,
      "challengeId": null,
      "createdAt": "2026-11-10T09:00:00Z"
    },
    {
      "id": 2,
      "amount": -10,
      "type": "SPENT",
      "orderId": 101,
      "bookId": null,
      "challengeId": null,
      "createdAt": "2026-11-13T14:30:00Z"
    }
  ]
}
```

---

### 2.7. Читацькі виклики (`/api/challenges`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/challenges` | Публічний / `USER` | **Список активних викликів із розрахунком прогресу (C7)**<br>Для авторизованого користувача автоматично підраховує кількість придбаних/поставлених книг у замовленнях `DELIVERED` та статус виконання. | `200 OK` (`List<ChallengeResponse>`) |
| `GET` | `/api/challenges/{id}` | Публічний / `USER` | **Деталі конкретного читацького виклику з персональним прогресом** | `200 OK` (`ChallengeResponse`), `404` |

#### Модель `ChallengeResponse`:
```json
{
  "id": 1,
  "title": "Осінній марафон читання",
  "description": "Прочитайте 5 книг до кінця листопада",
  "requiredBooks": 5,
  "genre": { "id": 2, "name": "Фантастика", "description": "Наукова фантастика" },
  "deadline": "2026-11-30T23:59:59Z",
  "rewardPoints": 50,
  "status": "ACTIVE",
  "userProgress": 3,
  "completed": false,
  "completedAt": null
}
```

---

### 2.8. Благодійні заклади (`/api/institutions`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/institutions` | Публічний | **Список закладів-одержувачів для благодійних замовлень (M8, C10)**<br>Фільтри: `type` (`ORPHANAGE`, `BOARDING_SCHOOL`, `LIBRARY`), `query` (пошук за назвою/адресою). | `200 OK` (`List<InstitutionResponse>`) |
| `GET` | `/api/institutions/{id}` | Публічний | **Отримання детальної інформації про заклад** | `200 OK` (`InstitutionResponse`), `404` |

#### Модель `InstitutionResponse`:
```json
{
  "id": 1,
  "name": "Дитячий будинок 'Пролісок'",
  "address": "м. Київ, вул. Лісова, 12",
  "contactPerson": "Іваненко Оксана Петрівна",
  "type": "ORPHANAGE"
}
```

---

### 2.9. Менеджер: Керування каталогом (`/api/manager`)

Доступ до всіх ендпоінтів цього розділу вимагає ролі **`MANAGER`** (`403 Forbidden` для інших користувачів).

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `POST` | `/api/manager/books` | `MANAGER` | **Додавання нового видання книги (M1.1, A.4)**<br>Тіло: `{ title, publisher, publicationYear, isbn, description, price, stock, genreId, authorIds }`<br>Перевіряє унікальність активного ISBN. | `201 Created` (`BookDetailsResponse`), `400`, `403`, `409 Conflict` (дублікат ISBN) |
| `PUT` | `/api/manager/books/{id}` | `MANAGER` | **Редагування видання книги (M1.2, A.4)**<br>Тіло: `{ title, publisher, publicationYear, isbn, description, price, stock, genreId, authorIds }` | `200 OK` (`BookDetailsResponse`), `400`, `403`, `404`, `409 Conflict` |
| `DELETE` | `/api/manager/books/{id}` | `MANAGER` | **М'яке видалення книги (M1.3)**<br>Дозволено лише якщо `stock == 0` та книга не фігурує в активних замовленнях (`AWAITING_PAYMENT`, `PAID`). Встановлює `deleted_at`. | `204 No Content`, `400` (якщо stock > 0), `403`, `404`, `409 Conflict` (є активні замовлення) |
| `POST` | `/api/manager/books/{id}/cover` | `MANAGER` | **Завантаження файлу обкладинки для книги**<br>Формат: `multipart/form-data`, поле `file`. Зберігає `bytea` у таблиці `book_covers`. | `200 OK`, `400`, `403`, `404` |
| `POST` | `/api/manager/genres` | `MANAGER` | **Створення нового жанру (M1)**<br>Тіло: `{ name, description }` | `201 Created` (`GenreResponse`), `400`, `403`, `409 Conflict` |
| `PUT` | `/api/manager/genres/{id}` | `MANAGER` | **Редагування жанру (M1)**<br>Тіло: `{ name, description }` | `200 OK` (`GenreResponse`), `400`, `403`, `404` |
| `DELETE` | `/api/manager/genres/{id}` | `MANAGER` | **Видалення жанру (M1)**<br>Дозволено лише якщо до жанру не прив'язана жодна книга (`RESTRICT`). | `204 No Content`, `400`, `403`, `404`, `409 Conflict` |
| `POST` | `/api/manager/authors` | `MANAGER` | **Створення нового автора (M1)**<br>Тіло: `{ fullName }`<br>Створює запис автора або повертає 409 Conflict, якщо автор з таким ім'ям уже існує. | `201 Created` (`AuthorResponse`), `400`, `403`, `409 Conflict` |
| `POST` | `/api/manager/books/import-csv` | `MANAGER` | **Пакетний імпорт книг із CSV-файлу (M2)**<br>Формат: `multipart/form-data`, поле `file`. Підтримує автовизначення роздільників (`,`, `;`, `\t`), валідує поля (включаючи `publisher` та `publication_year`), завантажує обкладинки за URL, створює відсутні жанри й авторів. | `200 OK` (`CsvImportResponse`), `400`, `403` |

#### Модель `CsvImportResponse`:
```json
{
  "totalRows": 25,
  "addedCount": 23,
  "rejectedCount": 2,
  "rejectedRows": [
    {
      "rowNumber": 4,
      "isbn": "978-966-00-1111-1",
      "reason": "Книга з таким ISBN вже існує в базі даних"
    },
    {
      "rowNumber": 12,
      "isbn": "978-966-00-2222-2",
      "reason": "Ціна повинна бути більше 0"
    }
  ]
}
```

---

### 2.10. Менеджер: Керування замовленнями (`/api/manager/orders`)

Доступ до всіх ендпоінтів цього розділу вимагає ролі **`MANAGER`**.

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/manager/orders` | `MANAGER` | **Перегляд списку замовлень (M3)**<br>Фільтри:<br>- `status`: конкретний статус замовлення (`AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED`)<br>- `unpaidOverdue`: `true` (не оплачено понад 7 днів — п. 7.2.2.6)<br>- `archived`: `true` (поставлені або скасовані понад 3 роки тому — п. 7.2.2.7)<br>- Пагінація: `page`, `size`, `sort` | `200 OK` (`Page<OrderSummaryResponse>`), `403` |
| `GET` | `/api/manager/orders/{id}` | `MANAGER` | **Повна картка замовлення з переліком дозволених переходів статусів (M3)** | `200 OK` (`OrderDetailsResponse`), `403`, `404` |
| `PATCH` | `/api/manager/orders/{id}/status` | `MANAGER` | **Зміна статусу замовлення (M3, Таблиця 3 SRS, A.1)**<br>Тіло: `{ status, expectedStatus }`<br>Суворо контролює допустимі переходи:<br>- з `AWAITING_PAYMENT` дозволено в `PAID`, `DELIVERED` (лише при `ON_DELIVERY`, п. 7.2.2.3), `CANCELLED`<br>- з `PAID` дозволено в `DELIVERED`, `CANCELLED` (п. 7.2.2.10 — неможливість доставки з поверненням залишків та бонусів).<br>При розбіжності з `expectedStatus` або конфлікті стану повертає `409 Conflict` з полем `currentStatus`.<br>При зміні статусу надсилає сповіщення покупцю через `NotificationService`.<br>При переході в `DELIVERED` нараховує бали ($\lfloor \text{total}/100 \rfloor$), зараховує читацькі виклики та записує у `gift_log`. | `200 OK` (`OrderDetailsResponse`), `400`, `403`, `404`, `409 Conflict` |
| `POST` | `/api/manager/orders/cancel-unpaid-overdue` | `MANAGER` | **Групове скасування прострочених замовлень за списком ID (п. 7.2.2.6)**<br>Тіло: `{"orderIds": [1, 2, ...]}` (@NotEmpty).<br>Перевіряє для кожного ID: статус `AWAITING_PAYMENT`, метод оплати `TRANSFER` та вік > 7 днів. Якщо хоча б одне не відповідає — відкочує транзакцію та повертає `409 Conflict`. | `200 OK` `{ message, cancelledCount }`, `400`, `403`, `409 Conflict` |
| `DELETE` | `/api/manager/orders/archive` | `MANAGER` | **Очищення архівних замовлень за списком ID (п. 7.2.2.7)**<br>Тіло: `{"orderIds": [1, 2, ...]}` (@NotEmpty).<br>Перевіряє для кожного ID: статус `DELIVERED` або `CANCELLED` та вік > 3 років. Якщо хоча б одне не відповідає — повертає `409 Conflict`. Видаляє замовлення, зберігаючи оцінки та `gift_log`. | `200 OK` `{ message, deletedCount }`, `400`, `403`, `409 Conflict` |

---

### 2.11. Оцінки та відгуки (`/api/ratings`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `POST` | `/api/ratings` | `USER` | **Оцінювання придбаного видання книги (C6)**<br>Тіло: `{ bookId, orderId?, value }` (оцінка 1–5).<br>Дозволено лише якщо користувач має замовлення у статусі `DELIVERED`, яке містить цю книгу. Нараховує **+1 бонусний бал** (`RATING_REWARD`). | `201 Created` (`RatingResponse`), `400`, `401`, `409 Conflict` (якщо вже оцінено) |
| `GET` | `/api/ratings/me` | `USER` | **Перегляд списку власних оцінок поточного користувача** | `200 OK` (`List<RatingResponse>`), `401` |
| `GET` | `/api/ratings/book/{bookId}` | Публічний | **Публічний перегляд оцінок конкретної книги** | `200 OK` (`List<RatingResponse>`) |

#### Модель `RatingResponse`:
```json
{
  "id": 1,
  "bookId": 10,
  "bookTitle": "Інтернат",
  "orderId": 100,
  "value": 5,
  "userId": 42,
  "userFullName": "Тарас Читач",
  "createdAt": "2026-11-13T18:40:00Z"
}
```

---

### 2.12. Генератор подарункових наборів (`/api/gift-sets`)

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `POST` | `/api/gift-sets/generate` | Публічний | **Автоматичний підбір випадкового набору книг у наявності (C9)**<br>Тіло: `{ budget, genreIds: [...], bookCount? }`<br>Вимоги: бюджет кратний 50 грн, книги в наявності (`stock > 0`). При неможливості зібрати набір повертає одну з двох чітких бізнес-причин відмови. | `200 OK` (`GiftSetResponse`), `400 Bad Request` |
| `POST` | `/api/gift-sets/replace` | Публічний | **Заміна книги в наборі на іншу альтернативну в межах залишкового бюджету (C9)**<br>Тіло: `{ currentBookIds: [...], bookIdToReplace, genreIds: [...], budget }` | `200 OK` (`GiftSetResponse`), `400 Bad Request`, `404` |
| `POST` | `/api/gift-sets/replace-candidates` | Публічний | **Отримання переліку всіх доступних книг-кандидатів для заміни позиції** | `200 OK` (`ReplaceCandidatesResponse`), `400` |

#### Модель `GiftSetResponse`:
```json
{
  "items": [
    {
      "id": 1,
      "title": "Інтернат",
      "isbn": "978-617-679-456-1",
      "genre": { "id": 1, "name": "Художня література" },
      "authors": [{ "id": 1, "name": "Сергій Жадан" }],
      "price": 250.00,
      "stock": 10,
      "coverUrl": "/api/catalog/books/1/cover"
    },
    {
      "id": 2,
      "title": "Ворошиловград",
      "isbn": "978-617-679-456-2",
      "genre": { "id": 1, "name": "Художня література" },
      "authors": [{ "id": 1, "name": "Сергій Жадан" }],
      "price": 300.00,
      "stock": 15,
      "coverUrl": "/api/catalog/books/2/cover"
    }
  ],
  "totalPrice": 550.00,
  "budget": 550.00,
  "remainingBudget": 0.00,
  "genres": [
    { "id": 1, "name": "Художня література" }
  ]
}
```

---

### 2.13. Менеджер: Читацькі виклики (`/api/manager/challenges`)

Доступ до всіх ендпоінтів цього розділу вимагає ролі **`MANAGER`**.

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/manager/challenges` | `MANAGER` | **Перегляд усіх читацьких викликів (M4)**<br>Параметри: `status` (`ACTIVE`, `COMPLETED`, `EXPIRED`, `CANCELLED`). | `200 OK` (`List<ChallengeResponse>`), `403` |
| `POST` | `/api/manager/challenges` | `MANAGER` | **Створення нового читацького виклику (M4.1)**<br>Тіло: `{ title, description, requiredBooks, genreId?, deadline, rewardPoints }` | `201 Created` (`ChallengeResponse`), `400`, `403` |
| `PUT` | `/api/manager/challenges/{id}` | `MANAGER` | **Редагування активного виклику (M4.2)**<br>Тіло: `{ title, description, requiredBooks, genreId?, deadline, rewardPoints }`<br>При зміні умови прогрес користувачів автоматично динамічно перераховується. | `200 OK` (`ChallengeResponse`), `400`, `403`, `404` |
| `POST` | `/api/manager/challenges/{id}/finish` | `MANAGER` | **Дострокове завершення виклику зі сповіщенням користувачів (M4.3)**<br>Переводить виклик у статус `CANCELLED`, фіксує `finished_at` та сповіщає користувачів із ненульовим прогресом. | `200 OK` (`FinishChallengeResponse`), `400`, `403`, `404` |

---

## 3. Матриця переходів статусів замовлень (M3, A.1)

Згідно з Таблицею 3 SRS (зведення до 4 значень відповідно до А.1), переходи статусів виконуються за такою логікою:

```
[ Створено замовлення: AWAITING_PAYMENT ]
          │
          ├───(Покупець: скасування неоплаченого TRANSFER / Менеджер)───► CANCELLED
          │
          ├───(Оплата TRANSFER / підтвердження менеджером)──────────────► PAID
          │                                                                 │
          │                                                                 ├───(Менеджер: неможливість доставки)───► CANCELLED
          │                                                                 │
          │                                                                 └───(Менеджер: доставлено)──────────────► DELIVERED
          │
          └───(Лише при paymentMethod == ON_DELIVERY / після доставки)─────────────────────────────────────────────► DELIVERED
```

| Поточний статус | Дозволені наступні статуси | Хто ініціює / Умови |
|---|---|---|
| `AWAITING_PAYMENT` | `PAID` | Менеджер (підтвердження банківського переказу) |
| `AWAITING_PAYMENT` | `DELIVERED` | Менеджер (лише якщо `paymentMethod == ON_DELIVERY`, п. 7.2.2.3) |
| `AWAITING_PAYMENT` | `CANCELLED` | Покупець (п. 7.2.2.4) або Менеджер (прострочено > 7 днів, п. 7.2.2.6) |
| `PAID` | `DELIVERED` | Менеджер (успішна доставка замовлення, нарахування бонусів, викликів, `gift_log`) |
| `PAID` | `CANCELLED` | Менеджер (неможливість доставки, п. 7.2.2.10, з поверненням залишків і балів) |
| `DELIVERED` | *(немає — фінальний)* | Замовлення виконано |
| `CANCELLED` | *(немає — фінальний)* | Замовлення скасовано |

---

## 4. Заплановані ендпоінти наступних етапів (Roadmap)

Наступні ендпоінти узгоджені в SRS і будуть додані на відповідних етапах реалізації:

### 4.1. Менеджер: Адміністрування акаунтів (M5, M6, M7)
- `GET /api/manager/users?email=` — пошук користувача за email.
- `GET /api/manager/users/inactive` — вибірка акаунтів без входу понад 2 роки (M5).
- `POST /api/manager/users/inactive/delete` — видалення неактивних записів із анонімізацією замовлень.
- `POST /api/manager/users/{id}/block` — ручне блокування акаунта менеджером `{ reason }` (M6).
- `POST /api/manager/users/{id}/unblock` — розблокування акаунта (M7).

### 4.2. Менеджер: Заклади (M8)
- `POST /api/manager/institutions` — створення нового закладу-одержувача.
- `PUT /api/manager/institutions/{id}` — редагування закладу.
- `DELETE /api/manager/institutions/{id}` — видалення закладу.

### 4.3. Менеджер: Журнал дарунків та аналітика (M9, M10)
- `GET /api/manager/gift-log?institutionId=&from=&to=` — журнал переданих книг.
- `GET /api/manager/analytics/demand` — аналітика затребуваності книг і жанрів.
- `GET /api/manager/analytics/stock` — аналітика залишків на складі.
- `GET /api/manager/analytics/rating` — рейтинг жанрів і авторів за оцінками.
