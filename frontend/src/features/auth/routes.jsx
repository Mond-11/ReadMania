import { lazyPage } from '../../shared/lazyPage.js';
import { AUTH_PATHS } from './paths.js';

// All auth pages are for Гість only (`guestOnly`): a logged-in user is sent on by ProtectedRoute.
const guestOnly = { guestOnly: true };
const authPage = () => import('./pages/AuthPage.jsx');

export default [
  { path: AUTH_PATHS.login, handle: guestOnly, ...lazyPage(authPage) },
  { path: AUTH_PATHS.register, handle: guestOnly, ...lazyPage(authPage) },
  { path: AUTH_PATHS.forgotPassword, handle: guestOnly, ...lazyPage(() => import('./pages/ForgotPasswordPage.jsx')) },
  { path: AUTH_PATHS.resetPassword, handle: guestOnly, ...lazyPage(() => import('./pages/ResetPasswordPage.jsx')) },
  { path: AUTH_PATHS.unblockRequest, handle: guestOnly, ...lazyPage(() => import('./pages/UnblockRequestPage.jsx')) },
];
