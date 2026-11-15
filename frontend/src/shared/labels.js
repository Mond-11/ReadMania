// All UI labels live here: one concept, one name. Keys are API enum values.
export const ROLES = {
  GUEST: "Гість",
  USER: "Користувач",
  MANAGER: "Менеджер",
};

export const USER_STATUS = {
  ACTIVE: "Активний",
  BLOCKED: "Заблокований",
};

export const ORDER_STATUS = {
  AWAITING_PAYMENT: "Очікує оплати",
  PAID: "Оплачено",
  DELIVERED: "Доставлено",
  CANCELLED: "Скасовано",
};

export const ORDER_TYPE = {
  REGULAR: "Звичайне",
  GIFT: "У дар",
};

export const PAYMENT_METHOD = {
  ON_DELIVERY: "Оплата при отриманні",
  TRANSFER: "Переказ на рахунок",
};

export const DELIVERY_METHOD = {
  POST_OFFICE: "Відділення пошти",
  COURIER: "Кур’єр",
};

export const INSTITUTION_TYPE = {
  ORPHANAGE: "Дитячий будинок",
  BOARDING_SCHOOL: "Школа-інтернат",
  LIBRARY: "Бібліотека",
};

export const CHALLENGE_STATUS = {
  ACTIVE: "Активний",
  COMPLETED: "Виконаний",
  EXPIRED: "Прострочений",
  CANCELLED: "Скасований",
};

export const BONUS_TRANSACTION_TYPE = {
  ORDER_REWARD: "Нарахування за замовлення",
  RATING_REWARD: "Нарахування за оцінку",
  CHALLENGE_REWARD: "Нарахування за виклик",
  SPENT: "Списання",
  REFUND: "Повернення",
};

export const TERMS = {
  appName: "ReadMania",
  bonusPoints: "бонусні бали",
  currencyShort: "грн",
};

export const COMMON = {
  loading: "Завантаження…",
  retry: "Спробувати ще раз",
  save: "Зберегти",
  cancel: "Скасувати",
  delete: "Видалити",
  edit: "Редагувати",
  back: "Назад",
  search: "Пошук",
  reset: "Скинути",
  emptyDash: "—",
  skipToContent: "Перейти до змісту",
  breadcrumbs: "Навігаційний ланцюжок",
};

export const PAGES = {
  notFoundTitle: "Сторінку не знайдено",
  notFoundText: "Такої сторінки немає або її адресу змінено.",
  errorTitle: "Щось пішло не так",
  errorText: "Сталася неочікувана помилка. Спробуйте оновити сторінку.",
  goHome: "До каталогу",
};

export const ERRORS = {
  network: "Немає зв’язку із сервером. Перевірте інтернет і спробуйте ще раз.",
  server: "Помилка на сервері. Спробуйте пізніше.",
  serverWithCode: "Помилка на сервері. Спробуйте пізніше. Код помилки:",
  unauthorized: "Сеанс завершено. Увійдіть знову.",
  forbidden: "Недостатньо прав для цієї дії.",
  notFound: "Потрібних даних не знайдено.",
  conflict: "Дію неможливо виконати: дані вже змінилися. Оновіть сторінку.",
  validation: "Перевірте введені дані.",
  unknown: "Не вдалося виконати дію. Спробуйте ще раз.",
  title: "Помилка",
};

export const BOOK_SORT = {
  "createdAt,desc": "Дата внесення: нові спочатку",
  "publicationYear,desc": "Рік видання: нові спочатку",
  "publicationYear,asc": "Рік видання: старі спочатку",
  "price,asc": "Ціна: від найдешевших",
  "price,desc": "Ціна: від найдорожчих",
};
export const DEFAULT_BOOK_SORT = "createdAt,desc";

export const CATALOG = {
  title: "Каталог",
  description: "Видання з нашого асортименту",
  filters: "Фільтри",
  showFilters: "Більше фільтрів",
  hideFilters: "Менше фільтрів",
  search: "Пошук за назвою або автором",
  searchPlaceholder: "Наприклад: Кобзар",
  genre: "Жанр",
  genrePlaceholder: "Усі жанри",
  author: "Автор",
  authorPlaceholder: "Усі автори",
  priceMin: "Ціна від, грн",
  priceMax: "Ціна до, грн",
  stockMin: "Залишок від, шт.",
  stockMax: "Залишок до, шт.",
  inStock: "Лише ті, що є в наявності",
  sort: "Сортування",
  resetFilters: "Скинути фільтри",
  priceRangeError:
    "Ціна: «від» не може бути більшою за «до». Наприклад: від 100 до 300.",
  stockRangeError:
    "Залишок: «від» не може бути більшим за «до». Наприклад: від 1 до 20.",
  emptyTitle: "Видань за критеріями не знайдено",
  emptyHint: "Змініть або скиньте фільтри.",
  found: "Знайдено:",
  pagination: "Сторінки каталогу",
  pageSize: "Видань на сторінці",
  outOfStock: "Відсутнє в наявності",
  inStockCount: "У наявності:",
  pcs: "шт.",
  noRatings: "Оцінок ще немає",
  ratingOf: "Середня оцінка",
  coverMissing: "Без обкладинки",
  authorUnknown: "Автор не вказаний",
  annotationTitle: "Опис",
  detailsTitle: "Деталі видання",
  publisher: "Видавництво",
  publicationYear: "Рік видання",
  isbn: "ISBN",
  notFoundTitle: "Видання не знайдено",
  notFoundHint: "Можливо, його вилучили з каталогу.",
  backToCatalog: "До каталогу",
};

export const CART = {
  add: "Додати до кошика",
  inCart: "У кошику",
  added: "Додано до кошика",
  loginHint: "Увійдіть, щоб додати видання до кошика.",
  outOfStockHint: "Видання відсутнє в наявності.",
  title: "Кошик",
  emptyTitle: "Кошик порожній",
  emptyHint: "Додайте видання з каталогу.",
  backToCatalog: "До каталогу",
  decreaseQuantity: "Зменшити кількість",
  increaseQuantity: "Збільшити кількість",
  quantity: "Кількість",
  remove: "Вилучити",
  removeConfirmTitle: "Вилучити з кошика?",
  removeConfirmMessage: (title) => `Видання «${title}» буде вилучено з кошика.`,
  removed: "Вилучено з кошика",
  unavailable: "Видання стало недоступним",
  subtotal: "Сума",
  totalItems: "Позицій у кошику",
  totalCopies: "Усього примірників",
  totalPrice: "Разом до сплати",
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
  city: 'Місто / Населений пункт',
  cityPlaceholder: 'Оберіть місто',
  warehouse: 'Відділення або поштомат',
  warehousePlaceholder: 'Оберіть відділення або поштомат',
  warehouseSelectCityFirst: 'Спочатку оберіть місто',
  manualAddressToggle: 'Ввести відділення вручну',
  manualAddressHint: 'Якщо потрібно вказати іншого поштового оператора (Укрпошта, Meest)',
  manualAddressPlaceholder: 'м. Київ, Укрпошта, Відділення 01001',
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
    recipientName:
      "Ім’я одержувача: вкажіть повне ім’я. Наприклад: Тарас Шевченко.",
    phone: "Телефон: вкажіть номер у форматі +380501234567.",
    postOffice: "Відділення пошти: оберіть місто та відділення зі списку. Наприклад: Київ, відділення №1.",
    address:
      "Адреса доставки: вкажіть місто, вулицю й будинок. Наприклад: м. Київ, вул. Хрещатик, 1.",
    institution: "Заклад: оберіть заклад-одержувач зі списку.",
  },
  requisitesTitle: "Реквізити для переказу",
  requisitesHint:
    "Після оформлення сплатіть замовлення переказом. У призначенні платежу вкажіть номер замовлення.",
  requisitesRecipient: "Одержувач",
  requisitesIban: "Рахунок (IBAN)",
  requisitesPurpose: "Призначення платежу",
  requisitesPurposeValue: "Оплата замовлення (номер буде після оформлення)",
  unavailableTitle: "Деякі видання недоступні",
  unavailableText:
    "Вилучіть їх із кошика або зменшіть кількість, щоб оформити замовлення.",
  conflictTitle: "Залишок змінився",
  conflictText:
    "Хтось купив останні примірники раніше. Перевірте кошик: кількість оновлено.",
  blockedTitle: "Обліковий запис заблоковано",
  blockedText: "Оформлення замовлень недоступне.",
  blockReason: "Причина:",
  submitUnavailableReason: "Є недоступні видання в кошику.",
  created: "Замовлення оформлено",
  previewError: "Не вдалося розрахувати знижку. Спробуйте ще раз.",
};

export const GIFT_SET = {
  title: "Подарунковий набір",
  description:
    "Оберіть жанри та бюджет — ми випадково підберемо видання, які є в наявності.",
  paramsSection: "Параметри набору",
  genres: "Жанри",
  genresPlaceholder: "Оберіть жанри",
  budget: "Бюджет, грн",
  budgetHint: "Кратний 50 грн. Наприклад: 300.",
  bookCount: "Кількість видань (необов’язково)",
  bookCountHint: "Якщо не вказано, кількість підбере система.",
  generate: "Підібрати набір",
  retry: "Підібрати інший набір",
  resultSection: "Ваш набір",
  total: "Вартість набору",
  budgetLabel: "Бюджет",
  remaining: "Залишок бюджету",
  replace: "Замінити",
  replaceTitle: "Замінити видання",
  replaceHint: (title, maxPrice) =>
    `Замість «${title}». Показано видання, які коштують не більше ніж ${maxPrice}.`,
  replaceRandom: "Підібрати випадкове",
  replaceNone: "Альтернатив немає",
  replaceNoneHint:
    "У межах залишкового бюджету та обраних жанрів інших видань у наявності немає.",
  replaced: "Видання замінено",
  removeBook: (title) => `Вилучити «${title}» з набору`,
  removed: "Вилучено з набору",
  showMore: "Показати більше",
  addToCart: "Додати набір до кошика",
  addedToCart: "Набір додано до кошика",
  partialAdd: (done, total) =>
    `Додано ${done} із ${total} видань. Решту не вдалося додати: спробуйте ще раз.`,
  goToCart: "До кошика",
  failTitle: "Не вдалося скласти набір",
  genresRequired: "Жанри: оберіть хоча б один жанр. Наприклад: «Класика».",
  budgetInvalid: "Бюджет: вкажіть суму від 50 грн, кратну 50. Наприклад: 300.",
  bookCountInvalid: "Кількість видань: вкажіть ціле число від 1. Наприклад: 3.",
  inStock: "У наявності:",
};

export const MANAGER_BOOK = {
  addAction: "Додати видання",
  editAction: "Редагувати видання",
  createTitle: "Нове видання",
  createDescription:
    "Заповніть усі поля. Обкладинку можна завантажити зараз або пізніше.",
  editTitle: "Редагування видання",
  mainSection: "Основні дані",
  salesSection: "Ціна й залишок",
  coverSection: "Обкладинка",
  title: "Назва",
  authors: "Автори",
  authorsPlaceholder: "Оберіть зі списку або введіть ім’я",
  authorsHint:
    "Натисніть Enter, щоб додати автора. Якщо автора немає у списку, його буде створено.",
  authorsNew: (names) => `Буде створено нових авторів: ${names.join(", ")}.`,
  genre: "Жанр",
  genrePlaceholder: "Оберіть зі списку або введіть назву",
  genreHint: "Якщо жанру немає у списку, його буде створено.",
  genreNew: (name) => `Буде створено новий жанр «${name}».`,
  publisher: "Видавництво",
  publicationYear: "Рік видання",
  isbn: "ISBN",
  isbnPlaceholder: "978-966-00-1234-5",
  price: "Ціна, грн",
  stock: "Залишок, шт.",
  description: "Опис",
  cover: "Файл обкладинки",
  coverHint: "JPEG або PNG.",
  coverCurrent: "Поточна обкладинка",
  coverReplaceHint: "Оберіть файл, щоб замінити обкладинку.",
  submitCreate: "Зберегти видання",
  submitEdit: "Зберегти зміни",
  created: "Видання збережено",
  updated: "Зміни збережено",
  coverFailed:
    "Видання збережено, але обкладинку не завантажено. Спробуйте ще раз: оберіть файл і збережіть.",
  duplicateIsbn:
    "ISBN: видання з таким ISBN уже є в каталозі. Перевірте номер або відредагуйте наявне видання.",
  errors: {
    title: "Назва: вкажіть назву, до 255 символів. Наприклад: Кобзар.",
    authors: "Автори: вкажіть хоча б одного автора. Наприклад: Тарас Шевченко.",
    authorLength: "Автори: ім’я автора не довше 255 символів.",
    genre:
      "Жанр: оберіть жанр або введіть назву нового, до 100 символів. Наприклад: Поезія.",
    publisher:
      "Видавництво: вкажіть видавництво, до 255 символів. Наприклад: А-БА-БА-ГА-ЛА-МА-ГА.",
    publicationYear:
      "Рік видання: вкажіть рік від 1450 до 2100. Наприклад: 2024.",
    isbn: "ISBN: вкажіть номер до 20 символів. Наприклад: 978-966-00-1234-5.",
    price: "Ціна: вкажіть суму більшу за 0. Наприклад: 350.",
    stock: "Залишок: вкажіть ціле число від 0. Наприклад: 15.",
    description:
      "Опис: коротко опишіть видання. Наприклад: Повне зібрання поетичних творів.",
    cover: "Обкладинка: оберіть файл у форматі JPEG або PNG.",
  },
};

export const MANAGER_CATALOG = {
  deleteBook: "Видалити видання",
  deleteBookTitle: "Видалити видання?",
  deleteBookMessage: (title) =>
    `Видання «${title}» буде видалено з каталогу. Цю дію не можна скасувати.`,
  deleteBookBlocked:
    "Видалити видання не можна: є залишок або воно є в активних замовленнях. Спершу обнуліть залишок і дочекайтеся завершення замовлень.",
  bookDeleted: "Видання видалено",
  genresAction: "Керувати жанрами",
  genresTitle: "Жанри",
  genresDescription: "Створення, перейменування й видалення жанрів каталогу.",
  genresList: "Список жанрів",
  genresEmptyTitle: "Жанрів ще немає",
  genresEmptyHint: "Додайте перший жанр.",
  booksCount: "Видань",
  addGenre: "Додати жанр",
  genreName: "Назва жанру",
  genreNamePlaceholder: "Наприклад: Поезія",
  genreNameError:
    "Назва жанру: вкажіть назву до 100 символів. Наприклад: Поезія.",
  genreDuplicate: "Назва жанру: такий жанр уже існує. Оберіть іншу назву.",
  createGenreTitle: "Новий жанр",
  renameGenreTitle: "Перейменування жанру",
  renameGenre: (name) => `Перейменувати жанр «${name}»`,
  deleteGenre: (name) => `Видалити жанр «${name}»`,
  deleteGenreBlocked: "Видалити жанр не можна: до нього прив’язані видання.",
  deleteGenreTitle: "Видалити жанр?",
  deleteGenreMessage: (name) =>
    `Жанр «${name}» буде видалено. Цю дію не можна скасувати.`,
  genreCreated: "Жанр додано",
  genreRenamed: "Жанр перейменовано",
  genreDeleted: "Жанр видалено",
  importAction: "Імпорт із CSV",
  importTitle: "Імпорт видань із CSV",
  importDescription:
    "Завантажте CSV-файл: видання, які пройшли перевірку, буде додано до каталогу, решту буде показано з номером рядка й причиною.",
  importFormatHint:
    "Роздільник визначається автоматично (кома, крапка з комою або табуляція). Відсутні жанри й автори створюються, обкладинки завантажуються за посиланням.",
  importUploadTitle: "Завантаження",
  importColumnsHint: 'Перший рядок файлу — назви колонок. Обов’язкові: Назва, ISBN, Ціна, Видавництво, Рік видання. Необов’язкові: Залишок (без нього — 0), Опис, Жанр (без нього — «Загальне»), Автор (без нього — «Невідомий автор», кілька авторів — через кому), Обкладинка (посилання http або https).',
  importFile: "Файл CSV",
  importFilePlaceholder: "Оберіть файл .csv",
  importFileRequired:
    "Файл CSV: оберіть файл у форматі .csv. Наприклад: books.csv.",
  importFileType:
    "Файл CSV: потрібен файл із розширенням .csv. Наприклад: books.csv.",
  importSubmit: "Імпортувати",
  importResult: "Результат імпорту",
  importTotal: "Рядків у файлі",
  importAdded: "Додано",
  importRejected: "Відхилено",
  importAllAdded: "Усі рядки успішно імпортовано.",
  importNoneAdded: "Жодного видання не додано: усі рядки відхилено.",
  importRejectedTitle: "Відхилені рядки",
  importRowNumber: "Рядок",
  importIsbn: "ISBN",
  importBookTitle: "Назва",
  importReason: "Причина",
  importMissingColumns: 'Файл не містить обов’язкових колонок: Назва, ISBN, Ціна, Видавництво, Рік видання.',
  importAgain: "Імпортувати інший файл",
  importDone: (added) => `Імпорт завершено, додано: ${added}`,
  toCatalog: "До каталогу",
};

export const MANAGER_INSTITUTION = {
  title: "Заклади-одержувачі",
  search: "Пошук за назвою або адресою",
  searchPlaceholder: "Наприклад: Сонечко",
  typeFilter: "Тип закладу",
  typeFilterPlaceholder: "Усі типи",
  emptyTitle: "Закладів ще немає",
  emptyHint: "Додайте перший заклад.",
  emptyFilteredTitle: "Закладів за цими умовами не знайдено",
  emptyFilteredHint: "Змініть пошуковий запит або тип закладу.",
  resetFilters: "Скинути умови",
  add: "Додати заклад",
  name: "Назва",
  type: "Тип",
  address: "Адреса",
  contactPerson: "Контактна особа",
  giftOrdersCount: "Благодійних замовлень",
  selectAll: "Обрати всі заклади в списку",
  selectOne: (name) => `Обрати заклад «${name}»`,
  editOne: (name) => `Редагувати заклад «${name}»`,
  selected: (n) => `Обрано: ${n}`,
  deleteSelected: "Видалити обрані заклади",
  deleteOne: (name) => `Видалити заклад «${name}»`,
  deleteBlocked:
    "Видалити заклад не можна: є замовлення, що очікують оплати або вже оплачені. Дочекайтеся їх завершення.",
  deleteTitle: "Видалити заклад?",
  deleteSelectedTitle: "Видалити обрані заклади?",
  deleteMessage: (name) =>
    `Заклад «${name}» буде видалено разом із його завершеними замовленнями. Цю дію не можна скасувати.`,
  deleteSelectedMessage: (n) =>
    `Буде видалено закладів: ${n}, разом із їхніми завершеними замовленнями. Цю дію не можна скасувати.`,
  deleted: "Заклад видалено",
  deletedMany: (n) => `Закладів видалено: ${n}`,
  createTitle: "Новий заклад",
  editTitle: "Редагування закладу",
  created: "Заклад додано",
  updated: "Заклад збережено",
  nameError: "Назва: вкажіть назву закладу. Наприклад: Дитячий будинок «Сонечко».",
  nameDuplicate: "Заклад із такою назвою вже існує.",
  typeError: "Тип закладу: оберіть зі списку. Наприклад: Дитячий будинок.",
  addressError: "Адреса: вкажіть адресу закладу. Наприклад: м. Київ, вул. Лісова, 10.",
  contactPersonError: "Контактна особа: вкажіть ім’я. Наприклад: Олена Петрівна.",
  namePlaceholder: "Дитячий будинок «Сонечко»",
  addressPlaceholder: "м. Київ, вул. Лісова, 10",
  contactPersonPlaceholder: "Олена Петрівна",
};

export const MANAGER_GIFT_LOG = {
  title: "Журнал подарованих видань",
  institution: "Заклад",
  institutionPlaceholder: "Усі заклади",
  dateFrom: "Доставлено від",
  dateTo: "Доставлено до",
  datePlaceholder: "дд.мм.рррр",
  rangeError: "Період: дата «до» не може бути раніше за дату «від». Наприклад: від 01.11.2026 до 15.11.2026.",
  futureError: "Період: дата не може бути з майбутнього. Вкажіть сьогоднішню або ранішу дату, наприклад 15.11.2026.",
  totalDonated: "Усього подаровано видань",
  deliveredAt: "Дата доставки",
  order: "Замовлення",
  institutionName: "Заклад",
  bookTitle: "Видання",
  quantity: "Кількість",
  emptyTitle: "Подарованих видань ще немає",
  emptyHint: "Записи з’являться після доставки благодійних замовлень.",
  emptyFilteredTitle: "За цими умовами записів не знайдено",
  emptyFilteredHint: "Змініть заклад або період.",
  resetFilters: "Скинути умови",
};

export const toOptions = (map) =>
  Object.entries(map).map(([value, label]) => ({ value, label }));

// Falls back to the raw key so an unknown enum value stays visible instead of hiding a bug.
export const labelOf = (map, key) =>
  key == null ? COMMON.emptyDash : (map[key] ?? String(key));

// Person B: header, access, authentication (G2, G3, M7).
export const NAV = {
  main: "Основна навігація",
  catalog: "Каталог",
  giftSet: "Подарунковий набір",
  cart: "Кошик",
  cartWithCount: "Кошик, примірників:",
  account: "Особистий кабінет",
  managerPanel: "Панель менеджера",
  help: "Довідка",
  helpSoon: "Настанова користувача готується",
  login: "Увійти",
  logout: "Вийти",
  loggedOut: "Ви вийшли з облікового запису",
  accountMenu: "Меню облікового запису",
  openMenu: "Відкрити меню",
  closeMenu: "Закрити меню",
  menu: "Меню",
};

export const ACCESS = {
  forbiddenTitle: "Немає доступу",
  forbiddenText: "Ця сторінка доступна лише для іншої ролі.",
  sessionExpired: "Сеанс завершено. Увійдіть знову.",
};

export const AUTH = {
  pageTitle: "Вхід і реєстрація",
  loginTab: "Вхід",
  registerTab: "Реєстрація",
  email: "Email",
  emailPlaceholder: "user@example.com",
  password: "Пароль",
  newPassword: "Новий пароль",
  confirmPassword: "Підтвердження пароля",
  passwordHint: "Щонайменше 6 символів.",
  loginSubmit: "Увійти",
  registerSubmit: "Зареєструватися",
  forgotLink: "Забули пароль?",
  registered: "Обліковий запис створено. Ласкаво просимо!",
  welcome: "Ви увійшли до облікового запису",

  emailRequired: "Вкажіть email. Приклад: user@example.com",
  emailInvalid:
    "Невірний формат email: потрібні «@» і домен. Приклад: user@example.com",
  passwordRequired: "Вкажіть пароль.",
  passwordTooShort:
    "Пароль закороткий: потрібно щонайменше 6 символів. Приклад: Pass123!",
  confirmMismatch: "Паролі не збігаються. Введіть той самий пароль ще раз.",
  emailTaken:
    "Обліковий запис із цим email уже існує. Увійдіть або скористайтеся «Забули пароль?».",

  invalidCredentialsTitle: "Не вдалося увійти",
  invalidCredentials:
    "Невірний email або пароль. Перевірте розкладку клавіатури та Caps Lock.",
  rateLimitedTitle: "Вхід тимчасово недоступний",
  rateLimited:
    "Після 5 невдалих спроб вхід для цього email призупинено на 15 хвилин.",
  retryAt: "Повторіть спробу після",

  blockedTitle: "Обліковий запис заблоковано",
  blockedReason: "Причина:",
  blockedAt: "Дата блокування:",
  blockedHint:
    "Каталог можна переглядати без входу. Щоб відновити доступ, надішліть запит Менеджеру.",
  unblockButton: "Запит на розблокування",

  forgotTitle: "Відновлення пароля",
  forgotDescription:
    "Вкажіть email облікового запису — ми надішлемо посилання для зміни пароля. Посилання діє 1 годину й лише один раз.",
  forgotSubmit: "Надіслати посилання",
  forgotSent:
    "Якщо обліковий запис існує, лист надіслано. Перевірте пошту, зокрема теку «Спам».",
  backToLogin: "Повернутися до входу",

  resetTitle: "Новий пароль",
  resetDescription: "Придумайте новий пароль для облікового запису.",
  resetSubmit: "Змінити пароль",
  resetDone: "Пароль змінено. Увійдіть із новим паролем.",
  resetNoToken: "Посилання для зміни пароля недійсне",
  resetNoTokenHint:
    "Відкрийте посилання з листа повністю або запросіть новий лист.",
  resetFailedTitle: "Не вдалося змінити пароль",
  requestNewLink: "Запросити нове посилання",

  unblockTitle: "Запит на розблокування",
  unblockDescription:
    "Підтвердьте, що це ваш обліковий запис, і поясніть, чому його варто розблокувати. Менеджер розгляне запит і надішле відповідь на email.",
  reason: "Обґрунтування",
  reasonPlaceholder:
    "Наприклад: Прошу вибачення, надалі вчасно оплачуватиму замовлення.",
  reasonRequired:
    "Опишіть причину розблокування. Приклад: «Надалі вчасно оплачуватиму замовлення».",
  reasonTooLong:
    "Обґрунтування задовге: максимум 1000 символів. Скоротіть текст.",
  unblockSubmit: "Надіслати запит",
  unblockSentTitle: "Запит надіслано",
  unblockSent:
    "Менеджер розгляне запит. Після розблокування ви отримаєте лист і зможете увійти.",
  unblockFailedTitle: "Запит не надіслано",
};

// Accessible names of Pagination controls, for any paged list.
export const PAGINATION_CONTROLS = {
  first: "Перша сторінка",
  previous: "Попередня сторінка",
  next: "Наступна сторінка",
  last: "Остання сторінка",
};

// Person B: personal account (C3–C7).
export const ACCOUNT = {
  title: "Особистий кабінет",
  tabs: "Розділи особистого кабінету",
  ordersTab: "Замовлення",
  profileTab: "Профіль",
  challengesTab: "Виклики і бали",
};

export const ORDERS = {
  listTitle: "Мої замовлення",
  emptyTitle: "Замовлень ще немає",
  emptyHint: "Оберіть видання в каталозі й оформіть перше замовлення.",
  pagination: "Сторінки замовлень",
  number: "Номер",
  numberShort: "№",
  orderTitle: "Замовлення №",
  date: "Дата",
  type: "Тип",
  status: "Статус",
  totalToPay: "До сплати",
  open: "Відкрити",
  openOrder: "Відкрити замовлення №",
  backToList: "До всіх замовлень",
  notFoundTitle: "Замовлення не знайдено",
  notFoundHint: "Можливо, номер указано з помилкою.",

  itemsTitle: "Склад замовлення",
  book: "Видання",
  price: "Ціна",
  quantity: "Кількість",
  subtotal: "Сума",
  rating: "Оцінка",

  summaryTitle: "Вартість",
  itemsTotal: "Вартість замовлення",
  discount: "Знижка за бонусні бали",
  pointsUsed: "Використано балів",

  deliveryTitle: "Доставка й оплата",
  deliveryMethod: "Спосіб доставки",
  giftDelivery: "У дар закладу",
  address: "Адреса",
  recipient: "Отримувач",
  phone: "Телефон",
  institution: "Заклад-одержувач",
  contactPerson: "Контактна особа",
  paymentMethod: "Спосіб оплати",
  createdAt: "Створено",
  statusChangedAt: "Статус змінено",
  deliveredAt: "Доставлено",

  historyTitle: "Історія статусів",
  historyChangedBy: "Змінив(-ла)",
  historyComment: "Коментар",

  cancel: "Скасувати замовлення",
  cancelConfirmTitle: "Скасувати замовлення?",
  cancelConfirmMessage: (id) =>
    `Замовлення №${id} буде скасовано. Примірники повернуться до каталогу, а використані бонусні бали — на ваш рахунок.`,
  cancelDone: "Замовлення скасовано",
  cancelHint:
    "Скасувати можна лише неоплачене замовлення з оплатою переказом. Від замовлення з оплатою під час отримання можна відмовитися при отриманні.",
  statusChanged: "Статус замовлення вже змінився. Показано актуальний стан.",

  rateLabel: (title) => `Оцінка видання «${title}»`,
  rateSubmit: "Оцінити",
  rateDone: "Дякуємо за оцінку! Нараховано 1 бонусний бал.",
  rateHint:
    "Оцініть видання від 1 до 5 і натисніть «Оцінити». За кожну оцінку — 1 бонусний бал.",
  rated: "Ваша оцінка",
  notRated: "Без оцінки",

  myRatingsTitle: "Мої оцінки",
  myRatingsEmpty: "Ви ще не оцінювали видання",
  myRatingsHint: "Оцінити можна видання з доставленого замовлення.",
};

export const PROFILE = {
  dataTitle: "Дані облікового запису",
  registeredAt: "Дата реєстрації",
  lastLoginAt: "Останній вхід",
  emailTitle: "Змінити email",
  newEmail: "Новий email",
  emailSame: "Новий email збігається з поточним. Вкажіть іншу адресу.",
  emailSaved: "Email змінено",
  passwordTitle: "Змінити пароль",
  currentPassword: "Поточний пароль",
  currentPasswordHint: "Потрібен для підтвердження будь-якої зміни.",
  passwordSaved: "Пароль змінено",
  saveEmail: "Зберегти email",
  savePassword: "Змінити пароль",
};

export const LOYALTY = {
  balanceTitle: "Бонусні бали",
  balance: "Баланс",
  balanceHint:
    "1 бал = 10 грн знижки; бали можна використати на оформленні замовлення (до 30% вартості).",
  activeTitle: "Активні читацькі виклики",
  activeEmpty: "Зараз немає активних викликів",
  activeEmptyHint:
    "Нові виклики з’являться тут — ви берете участь у них автоматично.",
  completedTitle: "Виконані виклики",
  completedEmpty: "Ви ще не виконали жодного виклику",
  condition: "Умова",
  // `count` is already formatted with the right plural form: «5 видань».
  conditionText: (count, genre) =>
    genre
      ? `Придбати ${count} жанру «${genre}»`
      : `Придбати ${count} будь-якого жанру`,
  progress: (done, total) => `Виконано ${done} з ${total} видань`,
  deadline: "Строк",
  reward: "Нагорода",
  completedAt: "Виконано",
  progressHint: "Зараховуються видання з доставлених замовлень.",
  historyTitle: "Журнал бонусних балів",
  historyEmpty: "Операцій з балами ще не було",
  operation: "Операція",
  date: "Дата",
  points: "Бали",
  details: "Деталі",
  orderRef: "Замовлення №",
};

// Person B: manager panel and order processing (M3).
export const MANAGER = {
  title: 'Панель менеджера',
  sections: 'Розділи панелі менеджера',
  section: 'Розділ',
  orders: 'Замовлення',
  challenges: 'Читацькі виклики',
  users: 'Облікові записи',
  institutions: 'Заклади',
  giftLog: 'Журнал подарованих книг',
  analytics: 'Аналітична панель',
  inProgress: 'Розділ у розробці',
};

export const MANAGER_ORDERS = {
  lists: 'Переліки замовлень',
  awaiting: 'Очікує оплати',
  paid: 'Оплачено',
  overdue: 'Не оплачено понад 7 днів',
  archive: 'Архів',
  awaitingHint: 'Нові замовлення зверху. Відкрийте замовлення, щоб підтвердити оплату або доставку.',
  paidHint: 'Оплачені замовлення, що чекають на доставку.',
  overdueHint: 'Замовлення з оплатою переказом, не оплачені понад 7 днів. Позначте їх і скасуйте: примірники й бали повернуться, Користувачі отримають лист.',
  archiveHint: 'Доставлені або скасовані замовлення, створені понад 3 роки тому. Записи журналу подарованих книг і оцінки збережуться.',
  emptyTitle: 'У цьому переліку замовлень немає',
  selectAll: 'Позначити всі замовлення на сторінці',
  selectOrder: 'Позначити замовлення №',
  selected: 'Позначено:',
  cancelSelected: 'Скасувати позначені',
  deleteSelected: 'Видалити позначені',
  cancelConfirmTitle: 'Скасувати позначені замовлення?',
  cancelConfirmMessage: (n) =>
    `Буде скасовано замовлень: ${n}. Примірники повернуться до каталогу, бали — на рахунки Користувачів, Користувачі отримають лист. Дію не можна відмінити.`,
  deleteConfirmTitle: 'Видалити позначені замовлення?',
  deleteConfirmMessage: (n) =>
    `Буде остаточно видалено замовлень: ${n}. Записи журналу подарованих книг і оцінки збережуться. Дію не можна відмінити.`,
  cancelledCount: (n) => `Скасовано замовлень: ${n}`,
  deletedCount: (n) => `Видалено замовлень: ${n}`,
  backToList: 'До переліку замовлень',

  statusTitle: 'Зміна статусу',
  currentStatus: 'Поточний статус',
  nextStatus: 'Новий статус',
  finalStatus: 'Статус фінальний — змінити його неможливо.',
  comment: 'Коментар',
  commentPlaceholder: 'Наприклад: Оплату отримано 15.11, квитанція №123',
  commentHint: 'Необов’язково. Зберігається в історії статусів.',
  saveStatus: 'Зберегти статус',
  chooseStatus: 'Оберіть новий статус.',
  confirmTitle: (label) => `Змінити статус на «${label}»?`,
  confirmMessage: 'Повернути попередній статус буде неможливо.',
  statusSaved: (label) => `Статус змінено на «${label}»`,
  conflictTitle: 'Статус уже змінено',
  conflict: (label) =>
    `Поки ви працювали, статус змінив інший Менеджер або Користувач скасував замовлення. Актуальний статус: «${label}». Оберіть дію ще раз.`,
  saveFailedTitle: 'Статус не збережено',
  saveFailed: 'Замовлення залишилося в попередньому статусі. Спробуйте ще раз.',
  transitions: {
    PAID: 'Оплату переказом отримано.',
    DELIVERED: 'Замовлення доставлено. Користувачу нарахуються бали (1 за кожні повні 100 грн), оновиться прогрес читацьких викликів; для замовлення в дар з’явиться запис у журналі.',
    CANCELLED: 'Примірники повернуться до каталогу, використані бали — на рахунок Користувача; Користувач отримає лист.',
  },
  customerHint: 'Користувач отримає лист про зміну статусу.',
};

// Person B: reading challenges in the manager panel (M4.1–M4.3).
export const MANAGER_CHALLENGES = {
  title: 'Читацькі виклики',
  description: 'Користувачі беруть участь в усіх активних викликах автоматично; прогрес рахується за доставленими замовленнями.',
  create: 'Створити виклик',
  statusFilter: 'Стан',
  allStatuses: 'Усі',
  emptyTitle: 'Викликів немає',
  emptyHint: 'Створіть перший читацький виклик.',
  emptyFiltered: 'Викликів із таким станом немає',
  name: 'Назва',
  namePlaceholder: 'Наприклад: Осінній марафон класики',
  nameRequired: 'Вкажіть назву виклику. Приклад: «Осінній марафон класики».',
  descriptionLabel: 'Опис',
  descriptionPlaceholder: 'Наприклад: Прочитайте 3 книги класичної літератури до кінця року',
  requiredBooks: 'Кількість видань (умова)',
  requiredBooksHint: 'Скільки видань має придбати Користувач.',
  requiredBooksError: 'Вкажіть ціле число від 1. Приклад: 3.',
  genre: 'Жанр (необов’язково)',
  genreAny: 'Будь-який жанр',
  deadline: 'Строк (дата завершення)',
  deadlineHint: 'Виклик діє до кінця обраного дня.',
  deadlineRequired: 'Оберіть дату завершення в майбутньому.',
  deadlinePast: 'Строк має бути в майбутньому: оберіть сьогоднішню або пізнішу дату.',
  reward: 'Нагорода, бонусні бали',
  rewardError: 'Вкажіть ціле число від 0. Приклад: 50.',
  condition: 'Умова',
  status: 'Стан',
  actions: 'Дії',
  createTitle: 'Новий читацький виклик',
  editTitle: 'Редагування виклику',
  editWarning: 'Після зміни умови прогрес Користувачів буде перераховано; уже нараховані бали залишаться.',
  edit: (name) => `Редагувати «${name}»`,
  created: 'Виклик створено',
  updated: 'Зміни збережено',
  finish: 'Завершити достроково',
  finishOne: (name) => `Завершити «${name}» достроково`,
  finishTitle: 'Завершити виклик достроково?',
  finishMessage: (name) =>
    `Виклик «${name}» буде завершено зараз. Користувачі з ненульовим прогресом отримають лист. Відновити виклик неможливо.`,
  finished: (n) => `Виклик завершено. Сповіщено Користувачів: ${n}`,
  onlyActive: 'Змінювати можна лише активний виклик.',
};

// Person B: user accounts in the manager panel (M5, M6, M7, M11).
export const MANAGER_USERS = {
  title: 'Облікові записи',
  tabs: 'Розділи облікових записів',
  searchTab: 'Пошук',
  requestsTab: 'Запити на розблокування',
  inactiveTab: 'Неактивні',
  email: 'Email',
  emailSearch: 'Пошук за email',
  emailSearchPlaceholder: 'Наприклад: ivan@',
  status: 'Стан',
  allStatuses: 'Усі',
  sort: 'Сортування',
  sortOptions: {
    'createdAt,desc': 'Реєстрація: нові спочатку',
    'createdAt,asc': 'Реєстрація: старі спочатку',
    'lastLoginAt,desc': 'Останній вхід: нещодавні спочатку',
    'lastLoginAt,asc': 'Останній вхід: давні спочатку',
  },
  registeredAt: 'Дата реєстрації',
  lastLoginAt: 'Останній вхід',
  never: 'Не входив(-ла)',
  emptyTitle: 'Облікових записів не знайдено',
  emptyHint: 'Змініть пошуковий запит або стан.',
  open: (email) => `Відкрити обліковий запис ${email}`,
  pagination: 'Сторінки облікових записів',
  backToList: 'До облікових записів',
  notFoundTitle: 'Обліковий запис не знайдено',
  notFoundHint: 'Можливо, його вже видалено.',

  ordersTitle: 'Замовлення',
  totalOrders: 'Усього замовлень',
  bonusBalance: 'Баланс бонусних балів',
  accountTitle: 'Обліковий запис',
  blockedAt: 'Дата блокування',
  blockReason: 'Причина блокування',

  block: 'Заблокувати',
  blockTitle: 'Заблокувати обліковий запис',
  blockHint:
    'Користувач утратить доступ одразу, навіть у відкритому сеансі, і отримає лист із причиною. Оформлені замовлення не скасовуються; кошик, бали й оцінки зберігаються.',
  reason: 'Причина блокування',
  reasonPlaceholder: 'Наприклад: Тричі не оплатив оформлені замовлення',
  reasonRequired: 'Вкажіть причину блокування — її побачить Користувач. Приклад: «Тричі не оплатив замовлення».',
  reasonTooLong: 'Причина задовга: максимум 500 символів.',
  blocked: 'Обліковий запис заблоковано',
  unblockHint: 'Розблокувати можна з вкладки «Запити на розблокування», коли Користувач надішле запит.',

  requestsHint: 'Запити, які надіслали заблоковані Користувачі. Позначте ті, які варто задовольнити, і підтвердьте розблокування. Непозначені запити залишаться в переліку.',
  requestsEmpty: 'Нових запитів на розблокування немає',
  justification: 'Обґрунтування',
  requestedAt: 'Дата запиту',
  selectAllRequests: 'Позначити всі запити',
  selectRequest: (email) => `Позначити запит ${email}`,
  selected: 'Позначено:',
  unblockSelected: 'Розблокувати позначені',
  unblockTitle: 'Розблокувати позначені облікові записи?',
  unblockMessage: (n) =>
    `Буде розблоковано облікових записів: ${n}. Користувачі знову отримають доступ і лист про розблокування.`,
  unblocked: 'Облікові записи розблоковано',

  inactiveHint:
    'Активні записи без входу понад 2 роки (без жодного входу — від дати реєстрації) і записи, не розблоковані понад 2 роки від дати блокування.',
  inactiveEmpty: 'Облікових записів без активності немає',
  referenceDate: 'Відповідна дата',
  referenceLogin: 'останній вхід або реєстрація',
  referenceBlocked: 'дата блокування',
  selectAllInactive: 'Позначити всі облікові записи',
  selectInactive: (email) => `Позначити ${email}`,
  deleteSelected: 'Видалити позначені',
  deleteTitle: 'Видалити позначені облікові записи?',
  deleteMessage: (n) =>
    `Буде остаточно видалено облікових записів: ${n}. Видаляються email, пароль, кошик і бали; у замовленнях — контакти отримувача. Склад, суми, дати й статуси замовлень та оцінки залишаються для обліку й аналітики. Дію не можна відмінити.`,
  deleted: (n) => `Видалено облікових записів: ${n}`,
};
