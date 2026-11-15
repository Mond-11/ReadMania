import { lazyPage } from '../../shared/lazyPage.js';

export default [
  { path: '/cart', handle: { roles: ['USER'] }, ...lazyPage(() => import('./pages/CartPage.jsx')) },
  { path: '/checkout', handle: { roles: ['USER'] }, ...lazyPage(() => import('./pages/CheckoutPage.jsx')) },
];
