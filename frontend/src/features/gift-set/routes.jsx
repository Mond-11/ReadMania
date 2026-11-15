import { lazyPage } from '../../shared/lazyPage.js';

export default [{ path: '/gift-set', handle: { roles: ['USER'] }, ...lazyPage(() => import('./pages/GiftSetPage.jsx')) }];
