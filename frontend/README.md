# ReadMania Frontend (React + Vite)

Клієнтська частина вебзастосунку книжкового інтернет-магазину **ReadMania**. Побудована як Single Page Application (SPA) на базі **React 19** та збирача **Vite** з перевіркою якості коду через **Oxlint**.

---

## Швидкий старт та розробка

### Встановлення залежностей
```bash
npm install
```

### Запуск локального dev-сервера
```bash
npm run dev
```
Додаток буде доступний за адресою `http://localhost:5173`.

### Перевірка якості коду (Linter)
```bash
npm run lint
```

### Збірка production-бандла
```bash
npm run build
```

---

## Запуск у Docker

Фронтенд контейнеризовано за допомогою multi-stage Dockerfile (Node 22 для збірки бандла + Nginx Alpine для роздачі статики та зворотного проксі до бекенду).

### 1. Запуск у складі всього проєкту (рекомендовано)
З кореня репозиторію:
```bash
./run-docker.sh up
```
Фронтенд буде доступний за адресою `http://localhost:5173`.

### 2. Окреме збирання та запуск Docker-образу фронтенду
```bash
# Збірка образу
docker build -t readmania-frontend .

# Запуск контейнера (за умови працюючого бекенду)
docker run -d -p 5173:80 --name readmania-frontend readmania-frontend
```

Конфігурація `nginx.conf` забезпечує:
- Роздачу статичних файлів із gzip-стисненням.
- Клієнтський роутинг SPA (`try_files $uri $uri/ /index.html`).
- Зворотне проксування запитів `/api/**` та `/swagger-ui/**` на бекенд-сервіс `http://backend:8080`.
- Базові заголовки безпеки (`X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`).

---

## Інтеграція з Backend REST API

### 1. Проксі для локальної розробки (Vite Dev Server)
Усі запити до `/api/**` автоматично перенаправляються на локальний бекенд (`http://localhost:8080`):
```javascript
// vite.config.js
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 2. Автентифікація та сесії (Session Cookies)
- Бекенд використовує сесійні cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`).
- **Обов'язкова вимога:** у всіх клієнтських HTTP-запитах (`fetch`, `axios`) повинен бути встановлений параметр передачі cookie:
  ```javascript
  // fetch API:
  fetch('/api/auth/me', { credentials: 'include' });

  // axios instance:
  axios.create({ withCredentials: true });
  ```

### 3. CSRF-захист (X-XSRF-TOKEN)
- Бекенд налаштований із `CookieCsrfTokenRepository` (`HttpOnly=false`).
- Для запитів, що змінюють стан (`POST`, `PUT`, `PATCH`, `DELETE`), клієнт повинен прочитати токен із cookie `XSRF-TOKEN` та додати заголовок `X-XSRF-TOKEN`:
  ```javascript
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
  }

  const csrfToken = getCookie('XSRF-TOKEN');

  fetch('/api/cart/items', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': csrfToken
    },
    credentials: 'include',
    body: JSON.stringify({ bookId: 1, quantity: 2 })
  });
  ```

### 4. Обробка помилок (RFC 9457 Problem Details)
Бекенд повертає помилки у стандартному форматі Problem Details (`application/problem+json`):
```json
{
  "type": "urn:readmania:error:account-blocked",
  "title": "Обліковий запис заблоковано",
  "status": 403,
  "detail": "Ваш акаунт заблоковано адміністратором.",
  "instance": "/api/auth/login",
  "blockReason": "Порушення правил спільноти",
  "blockedAt": "2026-11-14T18:00:00Z"
}
```
**Ключові типи помилок (`type`):**
- `urn:readmania:error:access-denied` (403) — недостатньо прав (потрібна роль `MANAGER`).
- `urn:readmania:error:account-blocked` (403) — користувач заблокований (можна відкрити форму подачі апеляції на `/api/auth/unblock-request`).
- `urn:readmania:error:login-rate-limited` (403) — тимчасове блокування після 5 невдалих спроб входу (поле `retryAfter`).
- `urn:readmania:error:order-status-conflict` (409) — конфлікт зміни статусу замовлення (поле `currentStatus`).

---

## Специфікація OpenAPI 3.1
Актуальна схема REST API для генерації TypeScript-типів або MSW-моків зберігається у файлі:
- `../docs/openapi.json`
- Інтерактивна документація Swagger UI доступна під час запуску бекенду за адресою `http://localhost:8080/swagger-ui.html`.
