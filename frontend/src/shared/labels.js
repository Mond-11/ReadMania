
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

export const BOOK_SORT = {
  'createdAt,desc': 'Дата внесення: нові спочатку',
  'publicationYear,desc': 'Рік видання: нові спочатку',
  'publicationYear,asc': 'Рік видання: старі спочатку',
  'price,asc': 'Ціна: від найдешевших',
  'price,desc': 'Ціна: від найдорожчих',
};
export const DEFAULT_BOOK_SORT = 'createdAt,desc';

export const CATALOG = {
  title: 'Каталог',
  description: 'Видання з нашого асортименту',
  filters: 'Фільтри',
  showFilters: 'Більше фільтрів',
  hideFilters: 'Менше фільтрів',
  search: 'Пошук за назвою або автором',
  searchPlaceholder: 'Наприклад: Кобзар',
  genre: 'Жанр',
  genrePlaceholder: 'Усі жанри',
  author: 'Автор',
  authorPlaceholder: 'Усі автори',
  priceMin: 'Ціна від, грн',
  priceMax: 'Ціна до, грн',
  stockMin: 'Залишок від, шт.',
  stockMax: 'Залишок до, шт.',
  inStock: 'Лише ті, що є в наявності',
  sort: 'Сортування',
  resetFilters: 'Скинути фільтри',
  priceRangeError: 'Ціна: «від» не може бути більшою за «до». Наприклад: від 100 до 300.',
  stockRangeError: 'Залишок: «від» не може бути більшим за «до». Наприклад: від 1 до 20.',
  emptyTitle: 'Видань за критеріями не знайдено',
  emptyHint: 'Змініть або скиньте фільтри.',
  found: 'Знайдено:',
  pagination: 'Сторінки каталогу',
  outOfStock: 'Відсутнє в наявності',
  inStockCount: 'У наявності:',
  pcs: 'шт.',
  noRatings: 'Оцінок ще немає',
  ratingOf: 'Середня оцінка',
  coverMissing: 'Без обкладинки',
  authorUnknown: 'Автор не вказаний',
  annotationTitle: 'Опис',
  detailsTitle: 'Деталі видання',
  publisher: 'Видавництво',
  publicationYear: 'Рік видання',
  isbn: 'ISBN',
  notFoundTitle: 'Видання не знайдено',
  notFoundHint: 'Можливо, його вилучили з каталогу.',
  backToCatalog: 'До каталогу',
};

export const CART = {
  add: 'Додати до кошика',
  inCart: 'У кошику',
  added: 'Додано до кошика',
  loginHint: 'Увійдіть, щоб додати видання до кошика.',
  outOfStockHint: 'Видання відсутнє в наявності.',
  title: 'Кошик',
  emptyTitle: 'Кошик порожній',
  emptyHint: 'Додайте видання з каталогу.',
  backToCatalog: 'До каталогу',
  decreaseQuantity: 'Зменшити кількість',
  increaseQuantity: 'Збільшити кількість',
  quantity: 'Кількість',
  remove: 'Вилучити',
  removeConfirmTitle: 'Вилучити з кошика?',
  removeConfirmMessage: (title) => `Видання «${title}» буде вилучено з кошика.`,
  removed: 'Вилучено з кошика',
  unavailable: 'Видання стало недоступним',
  subtotal: 'Сума',
  totalItems: 'Позицій у кошику',
  totalCopies: 'Усього примірників',
  totalPrice: 'Разом до сплати',
};

export const toOptions = (map) =>
  Object.entries(map).map(([value, label]) => ({ value, label }));

// Falls back to the raw key so an unknown enum value stays visible instead of hiding a bug.
export const labelOf = (map, key) => (key == null ? COMMON.emptyDash : (map[key] ?? String(key)));
