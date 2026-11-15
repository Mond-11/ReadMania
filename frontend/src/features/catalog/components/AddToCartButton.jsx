import { Button, Tooltip } from '@mantine/core';
import { useLocation, useNavigate } from 'react-router';
import { CART } from '../../../shared/labels.js';
import { AUTH_PATHS, useCurrentUser } from '../../auth/index.js';
import { useAddToCart, useCart } from '../../cart/index.js';

export function AddToCartButton({ book, fullWidth = true }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isGuest, isUser, isManager } = useCurrentUser();
  const cart = useCart({ enabled: isUser });
  const add = useAddToCart();

  // Assumption: Менеджер has no cart (unconfirmed).
  if (isManager) return null;

  const outOfStock = book.stock <= 0;
  const inCart = cart.data?.items?.some((i) => i.bookId === book.id) ?? false;

  if (isGuest) {
    // After login the user returns to `from` (see features/auth ProtectedRoute).
    const goToLogin = () => navigate(AUTH_PATHS.login, { state: { from: location.pathname + location.search } });
    return (
      <Button fullWidth={fullWidth} onClick={goToLogin} disabled={outOfStock}>
        {CART.add}
      </Button>
    );
  }

  const disabled = outOfStock || inCart || !isUser || cart.isPending;
  const reason = outOfStock ? CART.outOfStockHint : null;
  const button = (
    <Button
      fullWidth={fullWidth}
      disabled={disabled}
      loading={add.isPending}
      onClick={() => add.mutate({ bookId: book.id })}
    >
      {inCart ? CART.inCart : CART.add}
    </Button>
  );
  return reason ? <Tooltip label={reason}><span style={{ display: 'block' }}>{button}</span></Tooltip> : button;
}
