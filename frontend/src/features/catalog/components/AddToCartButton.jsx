import { Button, Tooltip } from '@mantine/core';
import { useLocation, useNavigate } from 'react-router';
import { CART } from '../../../shared/labels.js';
import { useAddToCart, useCart } from '../../cart/index.js';
import { useSession } from '../../../shared/hooks/useSession.js';

export function AddToCartButton({ book, fullWidth = true }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isGuest, isUser, isManager } = useSession();
  const cart = useCart({ enabled: isUser });
  const add = useAddToCart();

  // Assumption: Менеджер has no cart (unconfirmed).
  if (isManager) return null;

  const outOfStock = book.stock <= 0;
  const inCart = cart.data?.items?.some((i) => i.bookId === book.id) ?? false;

  if (isGuest) {
    // TODO(B-auth): confirm the login route and the `from` state name with B.
    const goToLogin =() => navigate('/login', { state: { from: location.pathname + location.search } });
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
