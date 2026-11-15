import { formatNumber, plural } from '../../shared/format.js';
import { MANAGER_ANALYTICS as L } from '../../shared/labels.js';

const SHOWN_NAMES = 3;

const nameList = (names) => {
  const head = names.slice(0, SHOWN_NAMES).join(', ');
  const rest = names.length - SHOWN_NAMES;
  return rest > 0 ? `${head} ${L.more} ${rest}` : head;
};

// A card listing items that need attention; `alert` is false when the list is empty.
const listCard = (key, label, emptyText, items) => ({
  key,
  label,
  alert: items.length > 0,
  value: items.length ? `${items.length}: ${nameList(items.map((i) => i.name))}` : emptyText,
});

const itemCard = (key, label, item, valueText) => ({ key, label, alert: false, value: `${item.name} — ${valueText}` });

// kind: 'demand' | 'stock' | 'ratings'; entity: 'genres' | 'books'.
// rows: the tab's data for this entity. catalog: all active items from the stock tab (or undefined while it loads),
// used to find items the demand endpoint does not return at all (never ordered).
export function buildInsights(kind, entity, rows, catalog) {
  const T = L.insights[entity];
  const cards = [];

  if (kind === 'demand') {
    const sorted = [...rows].sort((a, b) => b.orderedQuantity - a.orderedQuantity);
    const qty = (r) => `${formatNumber(r.orderedQuantity)} ${plural(r.orderedQuantity, L.copies)}`;
    if (sorted.length) cards.push(itemCard('top', T.demandTop, sorted[0], qty(sorted[0])));
    if (sorted.length > 1) cards.push(itemCard('bottom', T.demandBottom, sorted.at(-1), qty(sorted.at(-1))));
    if (catalog) {
      const ordered = new Set(rows.filter((r) => r.orderedQuantity > 0).map((r) => r.id));
      cards.push(listCard('none', T.demandNone, T.demandNoneEmpty, catalog.filter((c) => !ordered.has(c.id))));
    }
  } else if (kind === 'stock') {
    const out = rows.filter((r) => r.stock === 0);
    const low = rows.filter((r) => r.stock > 0 && r.stock <= L.lowStockLimit);
    cards.push(listCard('out', T.stockOut, T.stockOutEmpty, out));
    cards.push(listCard('low', T.stockLow, T.stockLowEmpty, low));
  } else {
    const rate = (r) => `${formatNumber(r.averageRating)} (${r.ratingsCount} ${plural(r.ratingsCount, L.ratingsCountForms)})`;
    const best = rows.reduce((m, r) => (!m || r.averageRating > m.averageRating ? r : m), null);
    if (best) cards.push(itemCard('top', T.ratingTop, best, rate(best)));
    const low = rows
      .filter((r) => r.averageRating < L.lowRatingLimit)
      .sort((x, y) => x.averageRating - y.averageRating)
      .map((r) => ({ ...r, name: `${r.name} (${formatNumber(r.averageRating)})` }));
    cards.push(listCard('low', T.ratingLow, T.ratingLowEmpty, low));
  }
  return cards;
}
