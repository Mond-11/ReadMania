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

### 2.14. Менеджер: Керування обліковими записами (`/api/manager/users`)

Доступ до всіх ендпоінтів цього розділу вимагає ролі **`MANAGER`** (`403 Forbidden` для інших користувачів).

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/manager/users` | `MANAGER` | **Пошук і перегляд списку користувачів (M6)**<br>Показує лише акаунти з роллю `USER`.<br>Параметри: `email` (підрядок, регістронезалежний), `status` (`ACTIVE`, `BLOCKED`), пагінація та сортування (`createdAt`, `lastLoginAt`). | `200 OK` (`Page<ManagerUserSummaryResponse>`), `403` |
| `GET` | `/api/manager/users/{id}` | `MANAGER` | **Детальна картка облікового запису**<br>Містить агреговану кількість замовлень за 4 статусами (`ordersByStatus`), `totalOrders`, баланс балів, причину та дату блокування. | `200 OK` (`ManagerUserDetailsResponse`), `403`, `404` |
| `POST` | `/api/manager/users/{id}/block` | `MANAGER` | **Блокування облікового запису (M6)**<br>Тіло: `{"reason": "Причина блокування"}` (@NotBlank, до 500 символів).<br>Встановлює `status = BLOCKED`, фіксує `blockedAt` та `blockReason`, надсилає email через `NotificationService`. Повертає 409 Conflict, якщо акаунт уже заблоковано або це менеджер. | `200 OK` (`ManagerUserDetailsResponse`), `400`, `403`, `404`, `409 Conflict` |
| `GET` | `/api/manager/users/unblock-requests` | `MANAGER` | **Перегляд списку поданих апеляцій на розблокування (M6, M7)**<br>Повертає перелік запитів із даними користувача та обґрунтуванням (`justification`). | `200 OK` (`List<ManagerUnblockRequestResponse>`), `403` |
| `POST` | `/api/manager/users/unblock` | `MANAGER` | **Масове розблокування облікових записів (M7)**<br>Тіло: `{"userIds": [1, 2, ...]}` (@NotEmpty).<br>Перевіряє для кожного користувача стан `BLOCKED`, змінює статус на `ACTIVE`, скидає причину, лічильник невдалих спроб, видаляє запис із `unblock_requests` та надсилає сповіщення. | `200 OK` `{"message": "Облікові записи успішно розблоковано"}`, `400`, `403`, `404`, `409 Conflict` |
| `GET` | `/api/manager/users/inactive` | `MANAGER` | **Перелік облікових записів, що підлягають видаленню (M5)**<br>Повертає користувачів ролі `USER`, які не входили понад 2 роки (`COALESCE(last_login_at, created_at) < cutoff`) або заблоковані понад 2 роки (`blocked_at < cutoff`). | `200 OK` (`List<InactiveUserResponse>`), `403` |
| `DELETE` | `/api/manager/users/inactive` | `MANAGER` | **Пакетне видалення неактивних акаунтів з анонімізацією історії (M5)**<br>Тіло: `{"userIds": [1, 2, ...]}` (@NotEmpty).<br>Перевіряє відповідність критерію неактивності > 2 роки (інакше `409 Conflict`), очищує персональні дані в `orders` (залишаючи склад і суми для звітності), відв'язує `ratings` (`user_id = NULL`), каскадно видаляє `cart_items`, `bonus_transactions`, `unblock_requests` та запис із `users`. | `200 OK` `{"message": "Неактивні облікові записи успішно видалено"}`, `400`, `403`, `409 Conflict` |

#### Модель `ManagerUserSummaryResponse`:
```json
{
  "id": 1,
  "email": "ivan.petrenko@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-11-10T12:00:00Z",
  "lastLoginAt": "2026-11-14T10:30:00Z"
}
```

#### Модель `ManagerUserDetailsResponse`:
```json
{
  "id": 1,
  "email": "ivan.petrenko@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-11-10T12:00:00Z",
  "lastLoginAt": "2026-11-14T10:30:00Z",
  "bonusBalance": 50,
  "totalOrders": 4,
  "ordersByStatus": {
    "AWAITING_PAYMENT": 0,
    "PAID": 1,
    "DELIVERED": 2,
    "CANCELLED": 1
  },
  "blockedAt": null,
  "blockReason": null
}
```

#### Модель `ManagerUnblockRequestResponse`:
```json
{
  "requestId": 10,
  "userId": 5,
  "email": "blocked.user@example.com",
  "blockedAt": "2026-11-14T08:00:00Z",
  "blockReason": "Порушення правил замовлень",
  "justification": "Прошу вибачення, надалі своєчасно оплачуватиму замовлення",
  "createdAt": "2026-11-14T09:15:00Z"
}
```

#### Модель `InactiveUserResponse`:
```json
{
  "id": 3,
  "email": "old.inactive@example.com",
  "status": "ACTIVE",
  "referenceDate": "2024-05-10T11:00:00Z"
}
```

---

### 2.15. Менеджер: Керування закладами-одержувачами (`/api/manager/institutions`)

Доступ до всіх ендпоінтів цього розділу вимагає ролі **`MANAGER`** (`403 Forbidden` для інших користувачів).

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/manager/institutions` | `MANAGER` | **Пошук і перегляд списку закладів (M8)**<br>Фільтри: `query` (пошук підрядка за назвою або адресою), `type` (`ORPHANAGE`, `BOARDING_SCHOOL`, `LIBRARY`). | `200 OK` (`List<InstitutionDetailsResponse>`), `403` |
| `GET` | `/api/manager/institutions/{id}` | `MANAGER` | **Детальна сторінка закладу**<br>Повертає дані закладу, кількість благодійних замовлень `giftOrdersCount` та обчислюваний прапорець `canDelete` (true, якщо немає активних замовлень `AWAITING_PAYMENT` / `PAID`). | `200 OK` (`InstitutionDetailsResponse`), `403`, `404` |
| `POST` | `/api/manager/institutions` | `MANAGER` | **Додавання нового закладу (M8)**<br>Тіло: `{ name, type, address, contactPerson }` | `200 OK` (`InstitutionDetailsResponse`), `400`, `403` |
| `PUT` | `/api/manager/institutions/{id}` | `MANAGER` | **Редагування закладу (M8)**<br>Тіло: `{ name, type, address, contactPerson }`<br>Оновлює дані закладу; вже створені замовлення зберігають свої незмінні знімки. | `200 OK` (`InstitutionDetailsResponse`), `400`, `403`, `404` |
| `DELETE` | `/api/manager/institutions` | `MANAGER` | **Пакетне видалення закладів (M8)**<br>Тіло: `{"institutionIds": [1, 2, ...]}` (@NotEmpty).<br>Перевіряє відсутність активних замовлень (`AWAITING_PAYMENT`, `PAID`) — при наявності повертає `409 Conflict`. При успіху у транзакції видаляє завершені замовлення (`DELIVERED`, `CANCELLED`) для цих закладів та самі заклади. | `200 OK` `{"message": "Заклади успішно видалено"}`, `400`, `403`, `404`, `409 Conflict` |

#### Модель `InstitutionDetailsResponse`:
```json
{
  "id": 1,
  "name": "Дитячий будинок 'Сонечко'",
  "type": "ORPHANAGE",
  "address": "вул. Лісова, 10, Київ",
  "contactPerson": "Олена Петрівна",
  "giftOrdersCount": 5,
  "canDelete": true
}
```

---

### 2.16. Менеджер: Журнал подарованих книг (`/api/manager/gift-log`)

Доступ до ендпоінтів цього розділу вимагає ролі **`MANAGER`** (`403 Forbidden` для інших користувачів).

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/manager/gift-log` | `MANAGER` | **Перегляд журналу подарованих книг зі статистикою (M9)**<br>Фільтри: `institutionId` (Long), `institutionName` (String, підрядок назви навіть видалених закладів), `dateFrom` (LocalDate, `YYYY-MM-DD`), `dateTo` (LocalDate, `YYYY-MM-DD`).<br>Валідація: якщо `dateTo < dateFrom` — `400 Bad Request`. | `200 OK` (`GiftLogPageResponse`), `400`, `403` |

#### Модель `GiftLogPageResponse`:
```json
{
  "items": [
    {
      "id": 1,
      "orderId": 105,
      "institutionName": "Дитячий будинок «Сонечко»",
      "bookTitle": "Кобзар",
      "quantity": 3,
      "deliveredAt": "2026-11-14"
    }
  ],
  "totalDonatedBooks": 3
}
```

---

### 2.17. Менеджер: Аналітична панель (`/api/manager/analytics`)

Доступ до ендпоінтів цього розділу вимагає ролі **`MANAGER`** (`403 Forbidden` для інших користувачів).

Загальні правила періоду (`from`, `to`):
- Якщо параметри не передані, за замовчуванням вибираються останні 3 місяці (`to = LocalDate.now()`, `from = to.minusMonths(3)`).
- Якщо `to < from` або `to > LocalDate.now()` — повертається `400 Bad Request` (Problem Details).

| Метод | Ендпоінт | Доступ | Опис та параметри | Коди відповідей |
|---|---|---|---|---|
| `GET` | `/api/manager/analytics/demand` | `MANAGER` | **Вкладка 1 — «Затребуваність» (M10)**<br>Параметри: `from`, `to`.<br>Підраховує суму замовлених примірників `SUM(quantity)` за період для незамовлень зі статусом `<> 'CANCELLED'`. Повертає згруповані списки `byGenres` та `byBooks`, відсортовані за спаданням кількості. | `200 OK` (`DemandAnalyticsResponse`), `400`, `403` |
| `GET` | `/api/manager/analytics/stock` | `MANAGER` | **Вкладка 2 — «Залишки» (M10)**<br>Відображає поточну кількість примірників у каталозі для активних видань (`deleted_at IS NULL`). Повертає списки `byGenres` та `byBooks`. | `200 OK` (`StockAnalyticsResponse`), `403` |
| `GET` | `/api/manager/analytics/ratings` | `MANAGER` | **Вкладка 3 — «Рейтинг» (M10)**<br>Параметри: `from`, `to`.<br>Обчислює середнє арифметичне оцінок для жанрів (`byGenres`) та окремих видань (`byBooks`), округлене до 2 знаків після коми, разом із кількістю оцінок `ratingsCount`. | `200 OK` (`RatingsAnalyticsResponse`), `400`, `403` |

#### Модель `DemandAnalyticsResponse`:
```json
{
  "byGenres": [
    { "id": 1, "name": "Фентезі", "orderedQuantity": 45 },
    { "id": 2, "name": "Наукова фантастика", "orderedQuantity": 30 }
  ],
  "byBooks": [
    { "id": 10, "name": "Володар Перснів", "orderedQuantity": 25 },
    { "id": 12, "name": "Дюна", "orderedQuantity": 20 }
  ]
}
```

#### Модель `StockAnalyticsResponse`:
```json
{
  "byGenres": [
    { "id": 1, "name": "Фентезі", "stock": 120 },
    { "id": 2, "name": "Наукова фантастика", "stock": 85 }
  ],
  "byBooks": [
    { "id": 10, "name": "Володар Перснів", "stock": 15 },
    { "id": 12, "name": "Дюна", "stock": 30 }
  ]
}
```

#### Модель `RatingsAnalyticsResponse`:
```json
{
  "byGenres": [
    { "id": 1, "name": "Фентезі", "averageRating": 4.65, "ratingsCount": 20 }
  ],
  "byBooks": [
    { "id": 10, "name": "Володар Перснів", "averageRating": 4.85, "ratingsCount": 14 }
  ]
}
```

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

## 4. Стан реалізації Бекенду

Усі заплановані зрізи бекенд-системи (Зрізи 1–5, вимоги C1–C10, M1–M12, A.1–A.10, G1–G4) **повністю реалізовані та протестовані**. Поточний стан та схема доступні у `docs/CURRENT_STATE.md` та `docs/openapi.json`.
