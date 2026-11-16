import dayjs from 'dayjs';
import { formatNumber, plural, toApiDate } from '../../shared/format.js';
import { MANAGER_ANALYTICS as L } from '../../shared/labels.js';

const sum = (rows, key) => rows.reduce((total, r) => total + Number(r[key] ?? 0), 0);

// The period of the same length right before [from, to]: 01.10–31.10 → 31.08–30.09 (31 days each).
export function previousPeriod({ from, to }) {
  const days = dayjs(to).diff(dayjs(from), 'day') + 1;
  const prevTo = dayjs(from).subtract(1, 'day');
  return { from: toApiDate(prevTo.subtract(days - 1, 'day').toDate()), to: toApiDate(prevTo.toDate()) };
}

// Whole percent change; null when the previous value is 0 (a change from nothing has no percent).
export function percentChange(current, previous) {
  if (!previous) return null;
  return Math.round(((current - previous) / previous) * 100);
}

export const signed = (n) => (n > 0 ? '+' : n < 0 ? '−' : '');

// Average of all ratings: each book weighs by its number of ratings, as in the backend genre rating (SRS M10).
export function overallRating(rows) {
  const count = sum(rows, 'ratingsCount');
  if (!count) return null;
  const total = rows.reduce((acc, r) => acc + Number(r.averageRating) * Number(r.ratingsCount), 0);
  return Math.round((total / count) * 100) / 100;
}

// Summary cards of a tab: { key, label, value, hint, tone } where tone colours the value ('danger' | 'success').
// data: { demand, prevDemand, stock, ratings } — API responses, any of them may still be undefined.
export function buildSummary(kind, { demand, prevDemand, stock, ratings }) {
  const K = L.kpi;
  const catalogBooks = stock?.byBooks ?? [];
  const catalogGenres = stock?.byGenres ?? [];

  if (kind === 'demand') {
    if (!demand) return [];
    const ordered = sum(demand.byBooks, 'orderedQuantity');
    const prev = prevDemand ? sum(prevDemand.byBooks, 'orderedQuantity') : null;
    const change = prev == null ? undefined : percentChange(ordered, prev);
    const orderedIds = new Set(demand.byBooks.filter((r) => r.orderedQuantity > 0).map((r) => r.id));
    const genresOrdered = demand.byGenres.filter((r) => r.orderedQuantity > 0).length;
    return [
      {
        key: 'ordered',
        label: K.ordered,
        value: formatNumber(ordered),
        hint: change === undefined ? null : change === null ? K.noPrevious : K.change(signed(change), formatNumber(Math.abs(change))),
        hintTone: change > 0 ? 'success' : change < 0 ? 'danger' : undefined,
      },
      stock && {
        key: 'without',
        label: K.booksWithoutOrders,
        value: formatNumber(catalogBooks.filter((b) => !orderedIds.has(b.id)).length),
        hint: K.ofCatalog(formatNumber(catalogBooks.length)),
      },
      stock && {
        key: 'genres',
        label: K.genresWithOrders,
        value: formatNumber(genresOrdered),
        hint: K.ofCatalog(formatNumber(catalogGenres.length)),
      },
    ].filter(Boolean);
  }

  if (kind === 'stock') {
    if (!stock) return [];
    const out = catalogBooks.filter((b) => b.stock === 0).length;
    const low = catalogBooks.filter((b) => b.stock > 0 && b.stock <= L.lowStockLimit).length;
    return [
      { key: 'total', label: K.stockTotal, value: formatNumber(sum(catalogBooks, 'stock')), hint: K.ofCatalog(formatNumber(catalogBooks.length)) },
      { key: 'out', label: K.outOfStock, value: formatNumber(out), tone: out ? 'danger' : undefined },
      { key: 'low', label: K.lowStock, value: formatNumber(low), hint: K.lowStockHint(L.lowStockLimit), tone: low ? 'warning' : undefined },
    ];
  }

  if (!ratings) return [];
  const average = overallRating(ratings.byBooks);
  const count = sum(ratings.byBooks, 'ratingsCount');
  const lowRated = ratings.byBooks.filter((r) => r.averageRating < L.lowRatingLimit).length;
  return [
    {
      key: 'average',
      label: K.averageRating,
      value: average == null ? '—' : formatNumber(average),
      hint: K.ratingsCount(`${formatNumber(count)} ${plural(count, L.ratingsCountForms)}`),
    },
    stock && {
      key: 'rated',
      label: K.ratedBooks,
      value: formatNumber(ratings.byBooks.length),
      hint: K.ofCatalog(formatNumber(catalogBooks.length)),
    },
    {
      key: 'low',
      label: K.lowRated,
      value: formatNumber(lowRated),
      hint: K.lowRatedHint(formatNumber(L.lowRatingLimit)),
      tone: lowRated ? 'danger' : undefined,
    },
  ].filter(Boolean);
}

const TIPS_PER_KIND = 3;

// «Що варто зробити»: actions for books, combining the period's demand and ratings with the current stock.
// - restock: ordered in the period and the stock does not cover the same demand again (stock < ordered);
// - review: not ordered in the period although the stock is above the low-stock limit;
// - rating: average rating below the limit.
// Returns { tips: [{ key, kind, text }], hidden } — at most TIPS_PER_KIND per kind, the rest counted in `hidden`.
export function buildTips({ demand, stock, ratings }) {
  if (!demand || !stock) return { tips: [], hidden: 0 };
  const stockById = new Map(stock.byBooks.map((b) => [b.id, b]));
  const orderedById = new Map(demand.byBooks.map((b) => [b.id, Number(b.orderedQuantity)]));

  const restock = demand.byBooks
    .filter((b) => stockById.has(b.id) && b.orderedQuantity > 0 && stockById.get(b.id).stock < b.orderedQuantity)
    .sort((a, b) => b.orderedQuantity - stockById.get(b.id).stock - (a.orderedQuantity - stockById.get(a.id).stock))
    .map((b) => ({
      key: `restock-${b.id}`,
      kind: 'restock',
      text: L.tipRestockText(b.name, formatNumber(b.orderedQuantity), formatNumber(stockById.get(b.id).stock)),
    }));

  const review = stock.byBooks
    .filter((b) => !orderedById.get(b.id) && b.stock > L.lowStockLimit)
    .sort((a, b) => b.stock - a.stock)
    .map((b) => ({ key: `review-${b.id}`, kind: 'review', text: L.tipReviewText(b.name, formatNumber(b.stock)) }));

  const rating = (ratings?.byBooks ?? [])
    .filter((b) => b.averageRating < L.lowRatingLimit)
    .sort((a, b) => a.averageRating - b.averageRating)
    .map((b) => ({
      key: `rating-${b.id}`,
      kind: 'rating',
      text: L.tipRatingText(b.name, formatNumber(b.averageRating), `${formatNumber(b.ratingsCount)} ${plural(b.ratingsCount, L.ratingsCountForms)}`),
    }));

  const groups = [restock, review, rating];
  return {
    tips: groups.flatMap((g) => g.slice(0, TIPS_PER_KIND)),
    hidden: groups.reduce((n, g) => n + Math.max(0, g.length - TIPS_PER_KIND), 0),
  };
}
