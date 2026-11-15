import { createBrowserRouter } from 'react-router';
import { ProtectedRoute } from '../features/auth/index.js';
import { PageLoader } from '../shared/ui/index.js';
import { Layout } from './Layout.jsx';
import { ErrorPage } from './pages/ErrorPage.jsx';
import { NotFoundPage } from './pages/NotFoundPage.jsx';
import { featureRoutes } from './routes.js';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    errorElement: <ErrorPage standalone />,
    hydrateFallbackElement: <PageLoader minHeight={400} />,
    children: [
      {
        // Pathless route: page errors render inside Layout, so the header stays.
        // ProtectedRoute checks `handle.roles` / `handle.guestOnly` of the matched feature route.
        element: <ProtectedRoute />,
        errorElement: <ErrorPage />,
        children: [...featureRoutes, { path: '*', element: <NotFoundPage /> }],
      },
    ],
  },
]);
