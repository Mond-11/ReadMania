import { Navigate } from 'react-router';
import { lazyPage } from '../../shared/lazyPage.js';
import { managerSectionRoutes } from './sections.js';

// «Панель менеджера»: Менеджер only. Sections come from features/*/managerRoutes.jsx.
export default [
  {
    path: '/manager',
    handle: { roles: ['MANAGER'] },
    ...lazyPage(() => import('./components/ManagerLayout.jsx')),
    children: [{ index: true, element: <Navigate to="orders" replace /> }, ...managerSectionRoutes],
  },
];
