import { ApiError } from './ApiError.js';

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';
const MUTATING = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const PRIME_URL = '/api/auth/me';

function readCookie(name) {
  const row = document.cookie.split('; ').find((r) => r.startsWith(`${name}=`));
  return row ? decodeURIComponent(row.slice(name.length + 1)) : null;
}

// Without the XSRF-TOKEN cookie the first mutating request would get 403, so fetch it with a cheap GET first.
let primePromise = null;
function primeCsrf(force = false) {
  const existing = readCookie(CSRF_COOKIE);
  if (existing && !force) return Promise.resolve(existing);
  if (!primePromise) {
    primePromise = fetch(PRIME_URL, { credentials: 'include' })
      .catch(() => {})
      .then(() => readCookie(CSRF_COOKIE))
      .finally(() => {
        primePromise = null;
      });
  }
  return primePromise;
}

export function buildUrl(path, query) {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value)) value.forEach((v) => params.append(key, String(v)));
    else params.append(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${path}?${qs}` : path;
}

// DELETE may return 200 with an empty body or 204.
async function parseBody(res) {
  let text;
  try {
    text = await res.text();
  } catch {
    throw ApiError.network();
  }
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

// Set by the auth feature (person B).
let unauthorizedHandler = null;
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

// Set by the auth feature (person B): the account was blocked during an open session (M6).
// Login and the unblock request answer `account-blocked` themselves, so their forms handle it.
const BLOCKED_HANDLED_BY_FORM = new Set(['/api/auth/login', '/api/auth/unblock-request']);
let accountBlockedHandler = null;
export function setAccountBlockedHandler(fn) {
  accountBlockedHandler = fn;
}

async function request(method, path, { query, body, signal, retried = false } = {}) {
  const headers = { Accept: 'application/json, application/problem+json' };
  let payload;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let sentToken = null;
  if (MUTATING.has(method)) {
    sentToken = await primeCsrf();
    if (sentToken) headers[CSRF_HEADER] = sentToken;
  }

  let res;
  try {
    res = await fetch(buildUrl(path, query), { method, headers, body: payload, credentials: 'include', signal });
  } catch (e) {
    if (e?.name === 'AbortError') throw e;
    throw ApiError.network();
  }

  const data = await parseBody(res);
  if (res.ok) return data;

  const error = ApiError.fromResponse(res.status, data);

  // A 403 without a specific type may be a stale CSRF token: refresh and retry once, only if the token changed.
  if (res.status === 403 && MUTATING.has(method) && !retried && (!error.type || error.type === 'access-denied')) {
    const fresh = await primeCsrf(true);
    if (fresh && fresh !== sentToken) return request(method, path, { query, body, signal, retried: true });
  }

  if (res.status === 401 && path !== PRIME_URL) unauthorizedHandler?.(error);
  if (error.is('account-blocked') && !BLOCKED_HANDLED_BY_FORM.has(path)) accountBlockedHandler?.(error);
  throw error;
}

export const api = {
  get: (path, opts) => request('GET', path, opts),
  post: (path, body, opts) => request('POST', path, { ...opts, body }),
  put: (path, body, opts) => request('PUT', path, { ...opts, body }),
  patch: (path, body, opts) => request('PATCH', path, { ...opts, body }),
  delete: (path, opts) => request('DELETE', path, opts),
};
