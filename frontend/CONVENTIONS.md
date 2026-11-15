# CONVENTIONS — ReadMania frontend (1 сторінка)

**Стек:** React 19, Vite, react-router 8, TanStack Query 5, Mantine 9 (усі `@mantine/*` — однієї версії), JS без TypeScript, лише світла тема. UI — українська. Перед пушем: `npm run lint` і `npm run build` без помилок.

## Структура й імпорти
```
src/app/       роутер, провайдери, Layout, Header (B), сторінки 404/500
src/shared/    api/, hooks/, ui/, labels.js, format.js, theme.js, notify.js, lazyPage.js
src/features/<фіча>/{api.js, components/, pages/, routes.jsx, index.js}
```
- Фіча імпортує з `shared/*` і з **інших фіч лише через їхній `index.js`**. `app/` імпортує фічі лише через `routes.jsx`.
- Зміни в `src/shared/` — спершу написати партнеру в чат. Комітимо дрібно, у `main`.

## Нова фіча = `routes.jsx`
```jsx
import { lazyPage } from '../../shared/lazyPage.js';
export default [{ path: '/cart', ...lazyPage(() => import('./pages/CartPage.jsx')) }];
```
Сторінка — `export default` компонент. `app/routes.js` підхоплює `features/*/routes.jsx` сам. Обмеження за роллю — через `handle: { roles: ['USER'] }` + обгортка B (`ProtectedRoute`).

## Запити до API
- `import { api } from '../../shared/api/index.js'`; **шлях повний, як у cheatsheet**: `api.get('/api/catalog/books', { query, signal })`, `api.post(path, body)`, `api.delete(path, { body })`. FormData передається як є.
- CSRF, cookie, розбір Problem Details робить клієнт. Помилка — `ApiError`: `.status`, `.type` (`'account-blocked'`, `'login-rate-limited'`, `'order-status-conflict'`, `'access-denied'`), `.fieldErrors`, `.blockReason`, `.currentStatus`, `.retryAfter`, `.errorId`; перевірка — `error.is('account-blocked')`.
- Читання — хук із `useQuery` у `features/<фіча>/api.js`. Ключі: `['genres']`, `['books', filters]`, `['cart']`. Після мутації — `invalidateQueries`. Порожню відповідь (200/204) клієнт повертає як `null`.
- Загальні хуки: `useGenres()`, `useInstitutions({ type, query })` — в `shared/hooks`.

## Стани й форми
- Кожна сторінка має 3 стани: завантаження (`PageLoader`), помилка (`ErrorState` + `onRetry`), порожньо (`EmptyState`) — з `shared/ui`.
- Форми — `@mantine/form`. Помилки бекенду біля полів: `onError: (e) => { if (!applyFormErrors(form, e)) notifyError(e); }`. Назви полів форми = назви полів запиту (вкладені — `contacts.recipientName`). Клієнтські перевірки (U3): поле, причина, приклад.
- Сповіщення: `notifySuccess('Збережено')`, `notifyError(error)` (`shared/notify.js`).

## Тексти, формати, кольори
- Жодних «голих» підписів статусів, ролей, способів оплати/доставки в компонентах — лише `shared/labels.js` (`labelOf(ORDER_STATUS, s)`, `toOptions(MAP)` для Select). Терміни: «Користувач», «видання», «бонусні бали».
- Гроші/дати/множина — `shared/format.js` (`formatMoney`, `formatDate`, `toApiDate` для фільтрів, `formatCount(n, BOOK_FORMS)`). **Не** `toISOString()` для дат фільтрів.
- Кольори — токени з `shared/theme.js` / `--rm-*` / Mantine (`c="brand"`), не hex у компонентах. **Текст:** `#223125`. **Не для тексту:** `#729969` (3.0:1), `#A1C0C2` (1.8:1) — лише декор, графіки, фон бейджів. Помилка — `color="red"` (= `#B3261E`).
- Фокус не вимикати. У кожного поля — `label`. Мінімальна ширина — 320 px.

## Контракти між A і B (пропозиція, узгодити)
- B: `features/auth/index.js` → `useCurrentUser()` = `{ user, isLoading, isGuest, isUser, isManager }` (запит `GET /api/auth/me`), і виклик `setUnauthorizedHandler(fn)` з `shared/api`.
- A: `features/cart/index.js` → `useCartCount()` для шапки.
