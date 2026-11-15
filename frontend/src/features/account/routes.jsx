import { Navigate } from 'react-router';
import { lazyPage } from '../../shared/lazyPage.js';

// Особистий кабінет is for Користувач only; ProtectedRoute reads `handle.roles` of the parent route.
export default [
  {
    path: '/account',
    handle: { roles: ['USER'] },
    ...lazyPage(() => import('./components/AccountLayout.jsx')),
    children: [
      { index: true, element: <Navigate to="orders" replace /> },
      { path: 'orders', ...lazyPage(() => import('./pages/OrdersPage.jsx')) },
      { path: 'orders/:id', ...lazyPage(() => import('./pages/OrderDetailsPage.jsx')) },
      { path: 'profile', ...lazyPage(() => import('./pages/ProfilePage.jsx')) },
      { path: 'challenges', ...lazyPage(() => import('./pages/ChallengesPage.jsx')) },
    ],
  },
];
