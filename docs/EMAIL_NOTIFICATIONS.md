# Архітектура та функціонування поштової підсистеми (Email Notifications)

Цей документ описує архітектуру, налаштування та особливості роботи підсистеми відправлення електронних листів у проєкті **ReadMania** на різних середовищах виконання (Production, Development, Local, Test).

> **Детальна документація:** Повний опис поштової підсистеми, взаємодії з сервісами та шаблонів листів наведено в розділах 7.13 та 12.4 [Настанови розробника (DEVELOPER_GUIDE.md)](DEVELOPER_GUIDE.md).

---

## 1. Загальна архітектура та ключові принципи

Підсистема сповіщень спроєктована за принципом інверсії залежностей (Dependency Inversion Principle) та забезпечує повну ізоляцію середовищ:

- **Єдиний інтерфейс:** Усі бізнес-сервіси (`AuthService`, `OrderService`, `ManagerOrderService`, `ManagerUserService`, `ChallengeService`) взаємодіють виключно з інтерфейсом `ua.readmania.service.notification.NotificationService`.
- **Асинхронність та ізоляція:**
  - У продуктивному профілі `SmtpNotificationService` (`@Profile("prod")`) методи позначено `@Async` (активовано `@EnableAsync` у `ReadmaniaBackendApplication`). Відправка виконується в окремому пулі потоків і не блокує HTTP-запити.
  - У профілях `local`, `test`, `default` реалізація `LoggingNotificationService` працює **синхронно**, що дозволяє автоматизованим тестам одразу перевіряти список надісланих повідомлень через `getSentEmails()`.
- **Відмовостійкість (Fault Tolerance):** Помилки транспорту чи збої мережі ізольовані блоками `try-catch` з логуванням винятків (`log.error(...)`). Тимчасова недоступність поштового сервера не призводить до відкату основної бізнес-транзакції (наприклад, створення замовлення чи блокування акаунта успішно фіксуються в БД навіть за збою поштового шлюзу).

---

## 2. Реалізація для Production (`@Profile("prod")`)

На продакшн-середовищі використовується компонент `SmtpNotificationService`:

- **Транспорт:** Базується на `JavaMailSender` зі складу `spring-boot-starter-mail`.
- **Формування повідомлень:** Створюються об'єкти `SimpleMailMessage` з вказанням адреси одержувача, теми та текстового тіла листа.
- **Адреса відправника (`from`):** Зчитується з властивості `spring.mail.username` (за замовчуванням `noreply@readmania.ua`).
- **Конфігурація середовища:** Параметри SMTP-підключення передаються через стандартні змінні оточення Spring Boot:

| Змінна середовища | Опис | Приклад значення |
|---|---|---|
| `SPRING_MAIL_HOST` | Хост SMTP-сервера | `smtp.sendgrid.net`, `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | Порт SMTP-сервера | `587` (STARTTLS) або `465` (SSL) |
| `SPRING_MAIL_USERNAME` | Логін / email для автентифікації | `apikey`, `noreply@readmania.ua` |
| `SPRING_MAIL_PASSWORD` | Пароль або API-токен | `secret-password` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | Увімкнення SMTP-автентифікації | `true` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | Увімкнення шифрування TLS | `true` |

---

## 3. Реалізація для DEV, LOCAL та TEST (`@Profile({"local", "test", "default"})`)

На середовищах розробки та автоматизованого тестування активується компонент `LoggingNotificationService`:

- **Безпека та ізоляція:** Зовнішні поштові сервери не викликаються, що унеможливлює випадкове надсилання тестових повідомлень реальним користувачам та усуває залежність локального середовища від інтернет-з'єднання чи поштових креденшелів.
- **Логування повідомлень:** Кожне сформоване повідомлення фіксується в логах додатку зі статусом `INFO` у стандартизованому форматі:
  ```text
  [NOTIFICATION LOG] To: user@example.com | Subject: Відновлення пароля у ReadMania | Body: ...
  ```
- **In-Memory буфер для тестів:** Листи зберігаються у потокобезпечному списку `CopyOnWriteArrayList<SentEmail>`.
- **Службові методи для перевірки в тестах:**
  - `getSentEmails()` — повертає незмінний список усіх відправлених листів з моменту запуску/очищення;
  - `getLastSentEmail()` — повертає останній сформований лист;
  - `clear()` — очищає список накопичених повідомлень між тестовими сценаріями.

Завдяки цьому інтеграційні тести (`AuthIntegrationTest`, `OrderIntegrationTest`, `Slice4GamificationAndGiftSetIT`, `ManagerUserIntegrationTest`) можуть надійно верифікувати факти надсилання листів, коректність адрес одержувачів та вміст одноразових токенів без необхідності мокування або підняття Mock SMTP серверів.

---

## 4. Бізнес-події та сценарії надсилання листів

| Подія / Метод | Одержувач | Тригер у системі | Вміст листа |
|---|---|---|---|
| **Нове замовлення**<br>`sendNewOrderAlertToManagers(Order order)` | `managers@readmania.ua` (команда менеджерів) | Створення звичайного або благодійного замовлення клієнтом (`POST /api/orders`, `POST /api/orders/gift`) | Номер замовлення, сума, позиції та адреса доставки / назва закладу. |
| **Зміна статусу замовлення**<br>`sendOrderStatusChanged(Order order)` | Email покупця (`order.getUser().getEmail()`) | Зміна статусу менеджером (`PATCH /api/manager/orders/{id}/status`, масові операції) або скасування покупцем (`POST /api/orders/{id}/cancel`) | Номер замовлення та новий встановлений статус (`AWAITING_PAYMENT`, `PAID`, `DELIVERED`, `CANCELLED`). |
| **Відновлення пароля**<br>`sendPasswordResetLink(User user, String rawToken)` | Email користувача (`user.getEmail()`) | Запит на скидання пароля (`POST /api/auth/password-reset/request`) | Посилання виду `/reset-password?token={rawToken}` із попередженням про термін дії токена (1 година). |
| **Блокування облікового запису**<br>`sendAccountBlocked(User user)` | Email заблокованого користувача (`user.getEmail()`) | Ручне блокування менеджером (`POST /api/manager/users/{id}/block`) | Повідомлення про блокування та причина (`user.getBlockReason()`), вказана менеджером. |
| **Розблокування облікового запису**<br>`sendAccountUnblocked(User user)` | Email користувача (`user.getEmail()`) | Схвалення розблокування менеджером (`POST /api/manager/users/unblock`) | Повідомлення про відновлення доступу до облікового запису. |
| **Дострокове завершення виклику**<br>`sendChallengeTerminatedEarly(User user, Challenge challenge)` | Учасники виклику з прогресом > 0 | Дострокове закриття челенджу менеджером (`POST /api/manager/challenges/{id}/finish`) | Назва виклику та сповіщення про його дострокове завершення. |

---

## 5. Відомі особливості реалізації

1. **Адреса менеджерів:** `managers@readmania.ua` жорстко зафіксована у коді `sendNewOrderAlertToManagers`.
2. **Відносне посилання скидання пароля:** базову URL-адресу клієнта не винесено в конфігурацію, тому в листі формується відносний шлях `/reset-password?token=...`.
3. **Логування токенів у dev-режимі:** `LoggingNotificationService` записує токен скидання пароля в журнал `INFO`, тому ця реалізація призначена виключно для профілів `local`, `test`, `default` та не повинна активуватися в `prod`.
