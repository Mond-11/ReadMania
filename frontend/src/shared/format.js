import { COMMON } from './labels.js';

const money = new Intl.NumberFormat('uk-UA', {
  style: 'currency',
  currency: 'UAH',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const integer = new Intl.NumberFormat('uk-UA');
const dateFmt = new Intl.DateTimeFormat('uk-UA', { day: '2-digit', month: '2-digit', year: 'numeric' });
const dateTimeFmt = new Intl.DateTimeFormat('uk-UA', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

const isEmpty = (v) => v === null || v === undefined || v === '';

export function formatMoney(value) {
  if (isEmpty(value) || Number.isNaN(Number(value))) return COMMON.emptyDash;
  return money.format(Number(value));
}

export function formatNumber(value) {
  if (isEmpty(value) || Number.isNaN(Number(value))) return COMMON.emptyDash;
  return integer.format(Number(value));
}

function toDate(value) {
  if (isEmpty(value)) return null;
  const d = value instanceof Date ? value : new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function formatDate(value) {
  const d = toDate(value);
  return d ? dateFmt.format(d) : COMMON.emptyDash;
}

export function formatDateTime(value) {
  const d = toDate(value);
  return d ? dateTimeFmt.format(d) : COMMON.emptyDash;
}

// Local calendar date; toISOString() would shift it to UTC.
export function toApiDate(value) {
  const d = toDate(value);
  if (!d) return undefined;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function plural(n, forms) {
  const abs = Math.abs(Math.trunc(Number(n)));
  const mod10 = abs % 10;
  const mod100 = abs % 100;
  if (mod10 === 1 && mod100 !== 11) return forms[0];
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return forms[1];
  return forms[2];
}

export function formatCount(n, forms) {
  return `${formatNumber(n)} ${plural(n, forms)}`;
}

export const BOOK_FORMS = ['видання', 'видання', 'видань'];
export const POINT_FORMS = ['бал', 'бали', 'балів'];
