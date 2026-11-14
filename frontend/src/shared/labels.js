
// All UI labels live here: one concept, one name. Keys are API enum values.
export const ROLES = {
  GUEST: 'Гість',
  USER: 'Користувач',
  MANAGER: 'Менеджер',
};

export const USER_STATUS = {
  ACTIVE: 'Активний',
  BLOCKED: 'Заблокований',
};

export const ORDER_STATUS = {
  AWAITING_PAYMENT: 'Очікує оплати',
  PAID: 'Оплачено',
  DELIVERED: 'Доставлено',
  CANCELLED: 'Скасовано',
};

export const ORDER_TYPE = {
  REGULAR: 'Звичайне',
  GIFT: 'У дар',
};

export const PAYMENT_METHOD = {
  ON_DELIVERY: 'Оплата при отриманні',
  TRANSFER: 'Переказ на рахунок',
};

export const DELIVERY_METHOD = {
  COURIER: 'Кур’єр',
  POST_OFFICE: 'Відділення пошти',
};

export const INSTITUTION_TYPE = {
  ORPHANAGE: 'Дитячий будинок',
  BOARDING_SCHOOL: 'Школа-інтернат',
  LIBRARY: 'Бібліотека',
};

export const CHALLENGE_STATUS = {
  ACTIVE: 'Активний',
  COMPLETED: 'Виконаний',
  EXPIRED: 'Прострочений',
  CANCELLED: 'Скасований',
};

export const BONUS_TRANSACTION_TYPE = {
  ORDER_REWARD: 'Нарахування за замовлення',
  RATING_REWARD: 'Нарахування за оцінку',
  CHALLENGE_REWARD: 'Нарахування за виклик',
  SPENT: 'Списання',
  REFUND: 'Повернення',
};

export const TERMS = {
  appName: 'ReadMania',
  bonusPoints: 'бонусні бали',
  currencyShort: 'грн',
};

export const COMMON = {
  loading: 'Завантаження…',
  retry: 'Спробувати ще раз',
  save: 'Зберегти',
  cancel: 'Скасувати',
  delete: 'Видалити',
  edit: 'Редагувати',
  back: 'Назад',
  search: 'Пошук',
  reset: 'Скинути',
  emptyDash: '—',
  skipToContent: 'Перейти до змісту',
  breadcrumbs: 'Навігаційний ланцюжок',
};

export const PAGES = {
  notFoundTitle: 'Сторінку не знайдено',
  notFoundText: 'Такої сторінки немає або її адресу змінено.',
  errorTitle: 'Щось пішло не так',
  errorText: 'Сталася неочікувана помилка. Спробуйте оновити сторінку.',
  goHome: 'До каталогу',
};

export const ERRORS = {
  network: 'Немає зв’язку із сервером. Перевірте інтернет і спробуйте ще раз.',
  server: 'Помилка на сервері. Спробуйте пізніше.',
  serverWithCode: 'Помилка на сервері. Спробуйте пізніше. Код помилки:',
  unauthorized: 'Сеанс завершено. Увійдіть знову.',
  forbidden: 'Недостатньо прав для цієї дії.',
  notFound: 'Потрібних даних не знайдено.',
  conflict: 'Дію неможливо виконати: дані вже змінилися. Оновіть сторінку.',
  validation: 'Перевірте введені дані.',
  unknown: 'Не вдалося виконати дію. Спробуйте ще раз.',
  title: 'Помилка',
};

export const toOptions = (map) =>
  Object.entries(map).map(([value, label]) => ({ value, label }));

// Falls back to the raw key so an unknown enum value stays visible instead of hiding a bug.
export const labelOf = (map, key) => (key == null ? COMMON.emptyDash : (map[key] ?? String(key)));
