import { lazyPage } from '../../shared/lazyPage.js';

// M3 pages inside «Панель менеджера» (paths are relative to /manager; access is checked by the panel).
export default [
  { path: 'orders', ...lazyPage(() => import('./pages/ManagerOrdersPage.jsx')) },
  { path: 'orders/:id', ...lazyPage(() => import('./pages/ManagerOrderDetailsPage.jsx')) },
];
