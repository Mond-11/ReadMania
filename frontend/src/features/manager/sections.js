import { MANAGER } from '../../shared/labels.js';

// Each feature adds its manager pages in features/<name>/managerRoutes.jsx (paths relative to /manager),
// the same way app/routes.js picks up routes.jsx. Example:
//   export default [{ path: 'institutions', ...lazyPage(() => import('./pages/InstitutionsPage.jsx')) }];
const modules = import.meta.glob('../*/managerRoutes.jsx', { eager: true });
export const managerSectionRoutes = Object.values(modules).flatMap((m) => m.default ?? []);

// The six sections of «Панель менеджера» (UX lab 3) in menu order.
export const MANAGER_SECTIONS = [
  { path: 'orders', label: MANAGER.orders },
  { path: 'challenges', label: MANAGER.challenges },
  { path: 'users', label: MANAGER.users },
  { path: 'institutions', label: MANAGER.institutions },
  { path: 'gift-log', label: MANAGER.giftLog },
  { path: 'analytics', label: MANAGER.analytics },
];

// A section is a link only once some feature provides its route; until then it is shown as «у розробці».
const provided = new Set(managerSectionRoutes.map((r) => String(r.path ?? '').split('/')[0]));
export const isSectionReady = (path) => provided.has(path);
