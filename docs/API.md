## Попередній перелік API (чернетка від ШІ, наслово не вірити)

Це попередня домовленість між бекендом і фронтендом. Після реалізації джерелом правди стане OpenAPI/Swagger бекенду. Усі зміни перелічених шляхів, полів і значень узгоджуємо в PR.

### Загальні домовленості

- Базовий шлях `/api`, формат JSON, кодування UTF-8.
- Автентифікація: серверна сесія (cookie `JSESSIONID`). Фронтенд надсилає запити з `credentials: 'include'`.
- Ролі: `GUEST` (без входу), `USER`, `MANAGER`. Шляхи `/api/manager/**` доступні лише менеджеру.
- Помилки у форматі Problem Details (RFC 9457): `status`, `title`, `detail`, а для валідації ще `errors: [{ field, message }]`.
- Статуси відповідей: `400` валідація, `401` не автентифіковано, `403` немає прав або акаунт заблоковано, `404` не знайдено, `409` конфлікт (одночасна зміна статусу, останній примірник, дубль ISBN).
- Пагінація списків: `?page=0&size=20&sort=field,asc`.
- Суми в гривнях числом з двома знаками, дати та час у форматі ISO-8601.
- Значення переліків передаються англійськими кодами:
    - статус замовлення: `AWAITING_PAYMENT`, `PROCESSING`, `ASSEMBLED`, `DELIVERING`, `DELIVERED`, `CANCELLED`
    - тип замовлення: `REGULAR`, `GIFT`
    - доставка: `COURIER`, `POST_OFFICE`
    - оплата: `ON_DELIVERY`, `TRANSFER`
    - стан облікового запису: `ACTIVE`, `BLOCKED`

### Автентифікація (Гість)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| POST | `/api/auth/register` | Реєстрація (email, пароль, підтвердження) | G3 |
| POST | `/api/auth/login` | Вхід; для заблокованого акаунта повертає причину та адресу для запиту на розблокування | G4 |
| POST | `/api/auth/logout` | Вихід | G4 |
| GET | `/api/auth/me` | Поточний користувач і роль | G4 |

### Каталог (Гість, Користувач)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/catalog/books` | Список книг із середніми оцінками. Фільтри: `author`, `genre` (можна кілька), `priceMin`, `priceMax`, `stockMin`, `stockMax`, `inStock` | G1, G2 |
| GET | `/api/catalog/books/{id}` | Сторінка книги з повними відомостями | G1 |
| GET | `/api/catalog/genres` | Список жанрів | G2 |

### Кошик (Користувач)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/cart` | Вміст кошика, ціни позицій, вартість замовлення | C1 |
| POST | `/api/cart/items` | Додати книгу `{ bookId, quantity }` | C1 |
| POST | `/api/cart/items/batch` | Додати кілька книг (наприклад, подарунковий набір) | C9 |
| PUT | `/api/cart/items/{bookId}` | Змінити кількість `{ quantity }` | C1 |
| DELETE | `/api/cart/items/{bookId}` | Вилучити книгу з кошика | C1 |

### Замовлення (Користувач)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| POST | `/api/orders/preview` | Розрахунок: вартість, знижка за бали, сума до сплати | C2, C8 |
| POST | `/api/orders` | Оформити замовлення з кошика `{ deliveryMethod, address, contacts, paymentMethod, bonusPoints }` | C2, C8 |
| POST | `/api/orders/gift` | Оформити замовлення в дар `{ institutionId, items, bonusPoints }` | C10, C8 |
| GET | `/api/orders` | Власні замовлення (зокрема в дар) | C4 |
| GET | `/api/orders/{id}` | Деталі замовлення зі статусом | C4 |
| POST | `/api/orders/{id}/cancel` | Скасувати замовлення зі статусом `PROCESSING` або `AWAITING_PAYMENT` | C3 |

### Оцінки, виклики, бонуси (Користувач)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| POST | `/api/ratings` | Оцінити книгу з поставленого замовлення `{ orderId, bookId, value }` | C6 |
| GET | `/api/ratings/me` | Власні оцінки | C4 |
| GET | `/api/challenges` | Читацькі виклики з прогресом (активні та виконані) | C7 |
| GET | `/api/loyalty` | Баланс бонусних балів | C7 |

### Подарунки (Користувач)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| POST | `/api/gift-sets/generate` | Згенерувати набір `{ amount, genres[], count? }` | C9 |
| POST | `/api/gift-sets/replace-candidates` | Варіанти заміни однієї книги набору тими самими жанрами в межах суми | C9 |
| GET | `/api/institutions` | Перелік закладів-одержувачів для вибору | C10 |

### Обліковий запис (Користувач)

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/account` | Контактні дані | C5 |
| PUT | `/api/account` | Змінити контактні дані | C5 |
| PUT | `/api/account/password` | Змінити пароль `{ currentPassword, newPassword, confirmPassword }` | C5 |

### Менеджер: каталог

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| POST | `/api/manager/books` | Додати видання | M1 |
| PUT | `/api/manager/books/{id}` | Редагувати видання | M1 |
| DELETE | `/api/manager/books/{id}` | Видалити видання | M1 |
| POST | `/api/manager/books/import` | Імпорт із CSV (`multipart/form-data`), у відповіді звіт про додані та відхилені рядки | M2 |
| POST | `/api/manager/genres` | Додати жанр | M1 |
| PUT | `/api/manager/genres/{id}` | Редагувати жанр | M1 |
| DELETE | `/api/manager/genres/{id}` | Видалити жанр (якщо до нього не належать книги) | M1 |

### Менеджер: замовлення

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/manager/orders` | Список замовлень, фільтр `status`, сортування за датою | M3 |
| GET | `/api/manager/orders/{id}` | Усі відомості замовлення та допустимі наступні статуси | M3 |
| PATCH | `/api/manager/orders/{id}/status` | Змінити статус `{ status, expectedStatus }`; при розбіжності `expectedStatus` повертає `409` | M3 |

### Менеджер: читацькі виклики

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/manager/challenges` | Список викликів | M4 |
| POST | `/api/manager/challenges` | Створити виклик | M4 |
| PUT | `/api/manager/challenges/{id}` | Редагувати активний виклик | M4 |
| POST | `/api/manager/challenges/{id}/finish` | Завершити достроково | M4 |

### Менеджер: облікові записи

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/manager/users?email=` | Знайти обліковий запис за email | M6, M7 |
| GET | `/api/manager/users/inactive` | Активні записи без входу понад 2 роки | M5 |
| POST | `/api/manager/users/inactive/delete` | Видалити всі або вибрані записи `{ ids? }` | M5 |
| POST | `/api/manager/users/{id}/block` | Заблокувати `{ reason }` | M6 |
| POST | `/api/manager/users/{id}/unblock` | Розблокувати | M7 |

### Менеджер: заклади, журнал, аналітика

| Метод | Шлях | Опис | ВВ |
|---|---|---|---|
| GET | `/api/manager/institutions` | Перелік закладів | M8 |
| POST | `/api/manager/institutions` | Додати заклад (назва, адреса, контактна особа) | M8 |
| PUT | `/api/manager/institutions/{id}` | Редагувати заклад | M8 |
| DELETE | `/api/manager/institutions/{id}` | Видалити заклад | M8 |
| GET | `/api/manager/gift-log?institutionId=&from=&to=` | Журнал подарованих книг із загальною кількістю | M9 |
| GET | `/api/manager/analytics/demand?from=&to=` | Затребуваність за жанрами та книгами | M10 |
| GET | `/api/manager/analytics/stock` | Поточні залишки | M10 |
| GET | `/api/manager/analytics/rating?from=&to=` | Рейтинг жанрів і книг | M10 |
