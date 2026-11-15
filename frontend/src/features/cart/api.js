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

// A read-only POST: recalculates the discount for the cart contents and the requested bonus points.
export function useOrderPreview({ bonusPoints, enabled = true }) {
  return useQuery({
    queryKey: ['orderPreview', bonusPoints],
    queryFn: ({ signal }) => api.post('/api/orders/preview', { bonusPoints }, { signal }),
    enabled,
    placeholderData: (previous) => previous,
    staleTime: 0,
  });
}

// Creates a regular or gift order from the current cart; both clear the cart and spend bonus points.
export function useCreateOrder({ gift }) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body) => api.post(gift ? '/api/orders/gift' : '/api/orders', body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CART_KEY });
      queryClient.invalidateQueries({ queryKey: ['orderPreview'] });
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
  });
}

// For the header (B). Zero for Guest/Менеджер, who have no cart.
export function useCartCount(enabled = true) {
  const { data } = useCart({ enabled });
  return data?.totalItems ?? 0;
}

export function useDeliveryCities(query = '') {
  return useQuery({
    queryKey: ['delivery', 'cities', query],
    queryFn: ({ signal }) => api.get('/api/delivery/cities', { query: query ? { query } : undefined, signal }),
    staleTime: 1000 * 60 * 10,
  });
}

export function useDeliveryWarehouses(cityRef, query = '') {
  return useQuery({
    queryKey: ['delivery', 'warehouses', cityRef, query],
    queryFn: ({ signal }) =>
      api.get('/api/delivery/warehouses', {
        query: { cityRef, ...(query ? { query } : {}) },
        signal,
      }),
    enabled: Boolean(cityRef),
    staleTime: 1000 * 60 * 10,
  });
}
