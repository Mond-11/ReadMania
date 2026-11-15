import { lazyPage } from '../../shared/lazyPage.js';

export default [
  { path: '/cart', ...lazyPage(() => import('./pages/CartPage.jsx')) },
];
