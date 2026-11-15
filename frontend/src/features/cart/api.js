import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { notifyError, notifySuccess } from '../../shared/notify.js';
import { CART } from '../../shared/labels.js';

export const CART_KEY = ['cart'];

// `enabled` is false for Guest and Менеджер: only a Користувач has a cart.
export function useCart({ enabled = true } = {}) {
  return useQuery({
    queryKey: CART_KEY,
    queryFn: ({ signal }) => api.get('/api/cart', { signal }),
    enabled,
  });
}

export function useAddToCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId, quantity = 1 }) => api.post('/api/cart/items', { bookId, quantity }),
    onSuccess: (cart) => {
      queryClient.setQueryData(CART_KEY, cart);
      notifySuccess(CART.added);
    },
    onError: (error) => {
      notifyError(error);
      queryClient.invalidateQueries({ queryKey: CART_KEY });
    },
  });
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId, quantity }) => api.put(`/api/cart/items/${bookId}`, { quantity }),
    onSuccess: (cart) => queryClient.setQueryData(CART_KEY, cart),
    onError: (error) => {
      notifyError(error);
      queryClient.invalidateQueries({ queryKey: CART_KEY });
    },
  });
}

export function useRemoveCartItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId }) => api.delete(`/api/cart/items/${bookId}`),
    onSuccess: (cart) => {
      queryClient.setQueryData(CART_KEY, cart);
      notifySuccess(CART.removed);
    },
    onError: (error) => {
      notifyError(error);
      queryClient.invalidateQueries({ queryKey: CART_KEY });
    },
  });
}

// For the header (B). Zero for Guest/Менеджер, who have no cart.
export function useCartCount(enabled = true) {
  const { data } = useCart({ enabled });
  return data?.totalItems ?? 0;
}
