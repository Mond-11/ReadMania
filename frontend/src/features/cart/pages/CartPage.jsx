import { Button, Divider, Group, SimpleGrid, Stack, Text } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, Navigate, useLocation } from 'react-router';
import { CART, CHECKOUT, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, Money, PageHeader, PageLoader } from '../../../shared/ui/index.js';
import { AUTH_PATHS, useCurrentUser } from '../../auth/index.js';
import { useCart, useRemoveCartItem, useUpdateCartItem } from '../api.js';
import { CartItemRow } from '../components/CartItemRow.jsx';

export default function CartPage() {
  useDocumentTitle(`${CART.title} — ${TERMS.appName}`);
  const location = useLocation();
  const session = useCurrentUser();
  const cart = useCart({ enabled: session.isUser });
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();

  if (session.isLoading) return <PageLoader />;

  // After login the user returns to `from` (see features/auth ProtectedRoute).
  if (session.isGuest) {
    return <Navigate to={AUTH_PATHS.login} state={{ from: location.pathname }} replace />;
  }

  // Assumption: Менеджер has no cart (unconfirmed).
  if (session.isManager) return <Navigate to="/" replace />;

  if (cart.isPending) {
    return (
      <>
        <PageHeader title={CART.title} />
        <PageLoader />
      </>
    );
  }

  if (cart.isError) {
    return (
      <>
        <PageHeader title={CART.title} />
        <ErrorState error={cart.error} onRetry={cart.refetch} />
      </>
    );
  }

  const items = cart.data.items ?? [];

  if (items.length === 0) {
    return (
      <>
        <PageHeader title={CART.title} />
        <EmptyState
          title={CART.emptyTitle}
          hint={CART.emptyHint}
          action={<Button component={Link} to="/" variant="default">{CART.backToCatalog}</Button>}
        />
      </>
    );
  }

  return (
    <>
      <PageHeader title={CART.title} />
      <SimpleGrid cols={{ base: 1, md: 2, xl: 3 }} spacing="sm" component="ul" style={{ padding: 0, margin: 0 }}>
        {items.map((item) => (
          <CartItemRow
            key={item.bookId}
            item={item}
            updating={updateItem.isPending && updateItem.variables?.bookId === item.bookId}
            removing={removeItem.isPending && removeItem.variables?.bookId === item.bookId}
            onChangeQuantity={(quantity) => updateItem.mutate({ bookId: item.bookId, quantity })}
            onRemove={() => removeItem.mutate({ bookId: item.bookId })}
          />
        ))}
      </SimpleGrid>

      <Divider my="md" />

      <Group justify="space-between" gap="xl" align="flex-end">
        <Button component={Link} to="/checkout">{CHECKOUT.checkoutAction}</Button>
        <Stack gap={0} align="flex-end">
          <Text c="dimmed" size="sm">{CART.totalItems}: {items.length}</Text>
          <Text c="dimmed" size="sm">{CART.totalCopies}: {cart.data.totalItems}</Text>
          <Group gap={6}>
            <Text fw={600}>{CART.totalPrice}:</Text>
            <Money value={cart.data.totalPrice} fw={700} size="lg" />
          </Group>
        </Stack>
      </Group>
    </>
  );
}
