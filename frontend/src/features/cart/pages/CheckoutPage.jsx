import { Button } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, Navigate, useLocation } from 'react-router';
import { CART, CHECKOUT, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageHeader, PageLoader } from '../../../shared/ui/index.js';
import { AUTH_PATHS, useCurrentUser } from '../../auth/index.js';
import { useCart } from '../api.js';
import { CheckoutForm } from '../components/CheckoutForm.jsx';

export default function CheckoutPage() {
  useDocumentTitle(`${CHECKOUT.title} — ${TERMS.appName}`);
  const location = useLocation();
  const session = useCurrentUser();
  const cart = useCart({ enabled: session.isUser });

  if (session.isLoading) return <PageLoader />;

  // After login the user returns to `from` (see features/auth ProtectedRoute).
  if (session.isGuest) {
    return <Navigate to={AUTH_PATHS.login} state={{ from: location.pathname }} replace />;
  }

  // Assumption: Менеджер has no cart (unconfirmed).
  if (session.isManager) return <Navigate to="/" replace />;

  const header = (
    <PageHeader
      title={CHECKOUT.title}
      breadcrumbs={[{ label: CART.title, to: '/cart' }, { label: CHECKOUT.title }]}
    />
  );

  if (cart.isPending) {
    return (
      <>
        {header}
        <PageLoader />
      </>
    );
  }

  if (cart.isError) {
    return (
      <>
        {header}
        <ErrorState error={cart.error} onRetry={cart.refetch} />
      </>
    );
  }

  if ((cart.data.items ?? []).length === 0) {
    return (
      <>
        {header}
        <EmptyState
          title={CHECKOUT.emptyTitle}
          hint={CHECKOUT.emptyHint}
          action={<Button component={Link} to="/" variant="default">{CART.backToCatalog}</Button>}
        />
      </>
    );
  }

  return (
    <>
      {header}
      <CheckoutForm user={session.user} />
    </>
  );
}
