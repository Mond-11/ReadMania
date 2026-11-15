import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

const KEY = ['manager-users'];
export const USERS_PAGE_SIZE = 10;
export const DEFAULT_USER_SORT = 'createdAt,desc';
export const managerUserPath = (id) => `/manager/users/${id}`;

// M11: search by e-mail, filter by state, sort by registration or last login.
export function useManagerUsers({ email, status, sort, page }) {
  return useQuery({
    queryKey: [...KEY, 'list', { email: email ?? null, status: status ?? null, sort, page }],
    queryFn: ({ signal }) =>
      api.get('/api/manager/users', {
        query: { email, status, sort, page: page - 1, size: USERS_PAGE_SIZE },
        signal,
      }),
    placeholderData: keepPreviousData,
  });
}

export function useManagerUser(id) {
  return useQuery({
    queryKey: [...KEY, 'detail', id],
    queryFn: ({ signal }) => api.get(`/api/manager/users/${id}`, { signal }),
    enabled: id != null,
    retry: (count, error) => error?.status !== 404 && count < 1,
  });
}

// M6: the answer is the updated account card.
export function useBlockUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }) => api.post(`/api/manager/users/${id}/block`, { reason: reason.trim() }),
    onSuccess: (user) => {
      queryClient.setQueryData([...KEY, 'detail', user.id], user);
      queryClient.invalidateQueries({ queryKey: [...KEY, 'list'] });
    },
  });
}

// M7: requests sent by blocked users from the login page.
export function useUnblockRequests() {
  return useQuery({
    queryKey: [...KEY, 'unblock-requests'],
    queryFn: ({ signal }) => api.get('/api/manager/users/unblock-requests', { signal }),
  });
}

export function useUnblockUsers() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userIds) => api.post('/api/manager/users/unblock', { userIds }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: KEY }),
  });
}

// M5: accounts without activity for more than 2 years (computed by the backend).
export function useInactiveUsers() {
  return useQuery({
    queryKey: [...KEY, 'inactive'],
    queryFn: ({ signal }) => api.get('/api/manager/users/inactive', { signal }),
  });
}

export function useDeleteInactiveUsers() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userIds) => api.delete('/api/manager/users/inactive', { body: { userIds } }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: KEY }),
  });
}
