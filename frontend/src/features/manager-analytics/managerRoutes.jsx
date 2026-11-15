import { lazyPage } from '../../shared/lazyPage.js';

// Page of «Панель менеджера»: path is relative to /manager, access is checked by the panel.
export default [{ path: 'analytics', ...lazyPage(() => import('./pages/AnalyticsPage.jsx')) }];
