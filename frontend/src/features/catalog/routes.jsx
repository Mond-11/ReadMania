import { lazyPage } from '../../shared/lazyPage.js';

export default [
  { path: '/', ...lazyPage(() => import('./pages/CatalogPage.jsx')) },
];
