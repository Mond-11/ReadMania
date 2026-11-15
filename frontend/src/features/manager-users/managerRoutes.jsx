import { lazyPage } from '../../shared/lazyPage.js';

// Pages of «Панель менеджера»: paths are relative to /manager, access is checked by the panel.
export default [
  { path: 'users', ...lazyPage(() => import('./pages/UsersPage.jsx')) },
  { path: 'users/:id', ...lazyPage(() => import('./pages/UserDetailsPage.jsx')) },
];
