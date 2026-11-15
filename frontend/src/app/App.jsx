import { RouterProvider } from 'react-router/dom';
import { Providers } from './providers.jsx';
import { router } from './router.jsx';

export function App() {
  return (
    <Providers>
      <RouterProvider router={router} />
    </Providers>
  );
}
