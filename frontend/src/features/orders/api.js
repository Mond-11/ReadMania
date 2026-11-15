import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { CURRENT_USER_KEY } from '../auth/index.js';

// Користувач's own orders (C3, C4, C6). Manager hooks for M3 will live next to these.
export const MY_ORDERS_KEY = ['orders', 'my'];
export const myOrderKey = (id) => ['orders', 'my', 'detail', id];
export const MY_RATINGS_KEY = ['ratings', 'me'];
export const ORDERS_PAGE_SIZE = 10;

export function useMyOrders(page) {
  return useQuery({
    queryKey: [...MY_ORDERS_KEY, 'list', page],
    queryFn: ({ signal }) =>
      api.get('/api/orders', { query: { page: page - 1, size: ORDERS_PAGE_SIZE, sort: 'createdAt,desc' }, signal }),
    placeholderData: keepPreviousData,
  });
}

export function useMyOrder(id) {
  return useQuery({
    queryKey: myOrderKey(id),
    queryFn: ({ signal }) => api.get(`/api/orders/${id}`, { signal }),
    enabled: id != null,
  });
}

// C3. Cancelling returns copies and bonus points, so the balance in the user cache changes too.
export function useCancelOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => api.post(`/api/orders/${id}/cancel`),
    onSuccess: (order) => {
      queryClient.setQueryData(myOrderKey(order.id), order);
      queryClient.invalidateQueries({ queryKey: MY_ORDERS_KEY });
      queryClient.invalidateQueries({ queryKey: CURRENT_USER_KEY });
      queryClient.invalidateQueries({ queryKey: ['loyalty'] });
    },
  });
}

// C6. +1 bonus point for each rating.
export function useRateBook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bookId, orderId, value }) => api.post('/api/ratings', { bookId, orderId, value }),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: myOrderKey(orderId) });
      queryClient.invalidateQueries({ queryKey: MY_RATINGS_KEY });
      queryClient.invalidateQueries({ queryKey: CURRENT_USER_KEY });
      queryClient.invalidateQueries({ queryKey: ['loyalty'] });
      // The book's average rating in the catalog changed.
      queryClient.invalidateQueries({ queryKey: ['books'] });
      queryClient.invalidateQueries({ queryKey: ['book'] });
    },
  });
}

export function useMyRatings() {
  return useQuery({
    queryKey: MY_RATINGS_KEY,
    queryFn: ({ signal }) => api.get('/api/ratings/me', { signal }),
  });
}

// C3: the Скасувати button exists only for own unpaid orders paid by transfer.
export const canUserCancel = (order) => order.status === 'AWAITING_PAYMENT' && order.paymentMethod === 'TRANSFER';
