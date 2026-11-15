import { lazyPage } from '../../shared/lazyPage.js';

const access = { roles: ['MANAGER'] };

export default [
  { path: '/manager/books/new', handle: access, ...lazyPage(() => import('./pages/BookFormPage.jsx')) },
  { path: '/manager/books/:id/edit', handle: access, ...lazyPage(() => import('./pages/BookFormPage.jsx')) },
  { path: '/manager/genres', handle: access, ...lazyPage(() => import('./pages/GenresPage.jsx')) },
];
