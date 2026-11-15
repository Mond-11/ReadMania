// Semantic URLs of the auth pages (D1). /reset-password is fixed: the backend puts it into the e-mail.
export const AUTH_PATHS = {
  login: '/login',
  register: '/register',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  unblockRequest: '/unblock-request',
};

const AUTH_PAGES = new Set(Object.values(AUTH_PATHS));

// Where to go after login when there is no page to return to.
// TODO(B-manager): '/manager' for Менеджер once the manager panel exists.
export function homeFor() {
  return '/';
}

// `from` comes from history state: accept only an internal path and never send the user back to an auth page.
export function safeReturnPath(from) {
  if (typeof from !== 'string' || !from.startsWith('/') || from.startsWith('//')) return null;
  const pathname = from.split(/[?#]/)[0];
  return AUTH_PAGES.has(pathname) ? null : from;
}
