import { BOOK_SORT, DEFAULT_BOOK_SORT } from '../../shared/labels.js';

// All divisible by 1, 2 and 3 (grid columns), so the last row is never ragged.
export const PAGE_SIZES = [12, 24, 48];
export const PAGE_SIZE = PAGE_SIZES[0];

const num = (v) => (v === null || v === '' || Number.isNaN(Number(v)) ? '' : v);

// URL → filters. Page is 1-based in the URL, 0-based in the API.
export function parseFilters(sp) {
  const sort = sp.get('sort');
  const page = Number.parseInt(sp.get('page') ?? '1', 10);
  const size = Number.parseInt(sp.get('size') ?? '', 10);
  return {
    size: PAGE_SIZES.includes(size) ? size : PAGE_SIZE,
    q: sp.get('q') ?? '',
    genreId: sp.get('genreId') ?? '',
    authorId: sp.get('authorId') ?? '',
    priceMin: num(sp.get('priceMin')),
    priceMax: num(sp.get('priceMax')),
    stockMin: num(sp.get('stockMin')),
    stockMax: num(sp.get('stockMax')),
    inStock: sp.get('inStock') === 'true',
    sort: sort && BOOK_SORT[sort] ? sort : DEFAULT_BOOK_SORT,
    page: Number.isFinite(page) && page > 0 ? page : 1,
  };
}

// Keeps the URL short: defaults are omitted.
export function toSearchParams(filters) {
  const sp = new URLSearchParams();
  for (const key of ['q', 'genreId', 'authorId', 'priceMin', 'priceMax', 'stockMin', 'stockMax']) {
    if (filters[key] !== '' && filters[key] != null) sp.set(key, String(filters[key]));
  }
  if (filters.inStock) sp.set('inStock', 'true');
  if (filters.sort !== DEFAULT_BOOK_SORT) sp.set('sort', filters.sort);
  if (filters.size !== PAGE_SIZE) sp.set('size', String(filters.size));
  if (filters.page > 1) sp.set('page', String(filters.page));
  return sp;
}

export const isRangeInvalid = (min, max) => min !== '' && max !== '' && Number(min) > Number(max);

// Filters → API query. An inverted range is not sent: the form shows the error instead.
export function toApiQuery(f) {
  const priceBad = isRangeInvalid(f.priceMin, f.priceMax);
  const stockBad = isRangeInvalid(f.stockMin, f.stockMax);
  return {
    query: f.q.trim(),
    genreId: f.genreId,
    authorId: f.authorId,
    priceMin: priceBad ? '' : f.priceMin,
    priceMax: priceBad ? '' : f.priceMax,
    stockMin: stockBad ? '' : f.stockMin,
    stockMax: stockBad ? '' : f.stockMax,
    inStock: f.inStock ? true : '',
    sort: f.sort,
    page: f.page - 1,
    size: f.size,
  };
}

export const hasActiveFilters = (f) =>
  Boolean(
    f.q || f.genreId || f.authorId || f.priceMin !== '' || f.priceMax !== '' ||
      f.stockMin !== '' || f.stockMax !== '' || f.inStock,
  );
