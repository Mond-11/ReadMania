import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { CURRENT_USER_KEY } from '../auth/index.js';

// Користувач's own orders (C3, C4, C6). Manager hooks for M3 will live next to these.
export const MY_ORDERS_KEY = ['orders', 'my'];
export const myOrderKey = (id) => ['orders', 'my', 'detail', id];
export const MY_RATINGS_KEY = ['ratings', 'me'];
export const ORDERS_PAGE_SIZE = 10;

// Page of the Користувач's own order in the personal account.
export const myOrderPath = (id) => `/account/orders/${id}`;

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

// ---------------------------------------------------------------------------
// Менеджер: M3 order processing.

export const MANAGER_ORDERS_KEY = ['orders', 'manager'];
export const managerOrderKey = (id) => ['orders', 'manager', 'detail', id];
export const managerOrderPath = (id) => `/manager/orders/${id}`;

// The four lists of M3 (7.2.1.1, 7.2.2.6, 7.2.2.7) and the API filter of each. The backend sorts newest first.
export const MANAGER_ORDER_LISTS = {
  awaiting: { query: { status: 'AWAITING_PAYMENT' } },
  paid: { query: { status: 'PAID' } },
  overdue: { query: { unpaidOverdue: true }, batch: 'cancel' },
  archive: { query: { archived: true }, batch: 'delete' },
};

export function useManagerOrders(list, page) {
  return useQuery({
    queryKey: [...MANAGER_ORDERS_KEY, 'list', list, page],
    queryFn: ({ signal }) =>
      api.get('/api/manager/orders', {
        query: { ...MANAGER_ORDER_LISTS[list].query, page: page - 1, size: ORDERS_PAGE_SIZE },
        signal,
      }),
    placeholderData: keepPreviousData,
  });
}

export function useManagerOrder(id) {
  return useQuery({
    queryKey: managerOrderKey(id),
    queryFn: ({ signal }) => api.get(`/api/manager/orders/${id}`, { signal }),
    enabled: id != null,
  });
}

// Table 3 of the SRS (and the backend state machine): statuses only move forward.
export function allowedTransitions(order) {
  switch (order.status) {
    case 'AWAITING_PAYMENT':
      return order.paymentMethod === 'TRANSFER' ? ['PAID', 'CANCELLED'] : ['DELIVERED', 'CANCELLED'];
    case 'PAID':
      return ['DELIVERED', 'CANCELLED'];
    default:
      return [];
  }
}

// `expectedStatus` makes the backend reject the change if someone changed the status first (7.2.2.8).
export function useChangeOrderStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status, expectedStatus, comment }) =>
      api.patch(`/api/manager/orders/${id}/status`, { status, expectedStatus, comment: comment?.trim() || undefined }),
    onSuccess: (order) => {
      queryClient.setQueryData(managerOrderKey(order.id), order);
      queryClient.invalidateQueries({ queryKey: MANAGER_ORDERS_KEY, exact: false, refetchType: 'none' });
      // Stock, ratings and the gift log may change after DELIVERED / CANCELLED.
      queryClient.invalidateQueries({ queryKey: ['books'] });
      queryClient.invalidateQueries({ queryKey: ['book'] });
    },
  });
}

function useBatchOrderAction(request) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderIds) => request(orderIds),
    onSettled: () => queryClient.invalidateQueries({ queryKey: MANAGER_ORDERS_KEY }),
  });
}

// 7.2.2.6: all or nothing — if one order no longer qualifies, the backend rejects the whole batch with 409.
export const useCancelOverdueOrders = () =>
  useBatchOrderAction((orderIds) => api.post('/api/manager/orders/cancel-unpaid-overdue', { orderIds }));

// 7.2.2.7
export const useDeleteArchivedOrders = () =>
  useBatchOrderAction((orderIds) => api.delete('/api/manager/orders/archive', { body: { orderIds } }));

// C3: the Скасувати button exists only for own unpaid orders paid by transfer.
export const canUserCancel = (order) => order.status === 'AWAITING_PAYMENT' && order.paymentMethod === 'TRANSFER';
