
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

export const CHECKOUT = {
  title: 'Оформлення замовлення',
  checkoutAction: 'Оформити замовлення',
  backToCart: 'Повернутися до кошика',
  emptyTitle: 'Немає що оформлювати',
  emptyHint: 'Додайте видання до кошика.',
  orderType: 'Тип замовлення',
  typeRegular: 'Для себе',
  typeGift: 'У дар закладу',
  deliverySection: 'Доставка',
  giftSection: 'Заклад-одержувач',
  contactsSection: 'Контакти одержувача',
  paymentSection: 'Оплата',
  bonusSection: 'Бонусні бали',
  summarySection: 'Підсумок',
  deliveryMethod: 'Спосіб доставки',
  address: 'Адреса доставки',
  addressPlaceholder: 'м. Київ, вул. Хрещатик, 1',
  recipientName: 'Ім’я одержувача',
  recipientNamePlaceholder: 'Тарас Шевченко',
  phone: 'Телефон',
  phonePlaceholder: '+380501234567',
  paymentMethod: 'Спосіб оплати',
  institution: 'Заклад',
  institutionPlaceholder: 'Оберіть заклад',
  institutionContact: 'Контактна особа',
  giftPaymentHint: 'Для замовлення в дар доступний лише переказ на рахунок.',
  bonusPoints: 'Скільки бонусних балів списати',
  bonusBalance: 'На вашому рахунку',
  bonusMax: 'Можна списати не більше',
  bonusLimitHint: 'Знижка не перевищує 30% суми замовлення.',
  itemsTotal: 'Сума видань',
  discount: 'Знижка бонусними балами',
  pointsUsed: 'Списано балів',
  totalToPay: 'До сплати',
  required: {
    recipientName: 'Ім’я одержувача: вкажіть повне ім’я. Наприклад: Тарас Шевченко.',
    phone: 'Телефон: вкажіть номер у форматі +380501234567.',
    address: 'Адреса доставки: вкажіть місто, вулицю й будинок. Наприклад: м. Київ, вул. Хрещатик, 1.',
    institution: 'Заклад: оберіть заклад-одержувач зі списку.',
  },
  requisitesTitle: 'Реквізити для переказу',
  requisitesHint: 'Після оформлення сплатіть замовлення переказом. У призначенні платежу вкажіть номер замовлення.',
  requisitesRecipient: 'Одержувач',
  requisitesIban: 'Рахунок (IBAN)',
  requisitesPurpose: 'Призначення платежу',
  requisitesPurposeValue: 'Оплата замовлення (номер буде після оформлення)',
  unavailableTitle: 'Деякі видання недоступні',
  unavailableText: 'Вилучіть їх із кошика або зменшіть кількість, щоб оформити замовлення.',
  conflictTitle: 'Залишок змінився',
  conflictText: 'Хтось купив останні примірники раніше. Перевірте кошик: кількість оновлено.',
  blockedTitle: 'Обліковий запис заблоковано',
  blockedText: 'Оформлення замовлень недоступне.',
  blockReason: 'Причина:',
  submitUnavailableReason: 'Є недоступні видання в кошику.',
  created: 'Замовлення оформлено',
  previewError: 'Не вдалося розрахувати знижку. Спробуйте ще раз.',
};

export const toOptions = (map) =>
  Object.entries(map).map(([value, label]) => ({ value, label }));

// Falls back to the raw key so an unknown enum value stays visible instead of hiding a bug.
export const labelOf = (map, key) => (key == null ? COMMON.emptyDash : (map[key] ?? String(key)));
