import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

// Same key the catalog used before this feature existed, so every component shares one cache entry.
export const CURRENT_USER_KEY = ['auth', 'me'];

// 401 means Гість, not an error.
async function fetchCurrentUser({ signal }) {
  try {
    return await api.get('/api/auth/me', { signal });
  } catch (e) {
    if (e?.status === 401) return null;
    throw e;
  }
}

export function useCurrentUser() {
  const query = useQuery({
    queryKey: CURRENT_USER_KEY,
    queryFn: fetchCurrentUser,
    retry: false,
    staleTime: 60_000,
  });
  const user = query.data ?? null;
  return {
    user,
    isLoading: query.isPending,
    isGuest: !query.isPending && !user,
    isUser: user?.role === 'USER',
    isManager: user?.role === 'MANAGER',
  };
}

const isAuthQuery = (query) => query.queryKey[0] === CURRENT_USER_KEY[0];

// After the user changes, data of the previous one (cart, orders, …) must not stay in the cache.
export function resetSession(queryClient, user) {
  queryClient.setQueryData(CURRENT_USER_KEY, user);
  queryClient.removeQueries({ predicate: (q) => !isAuthQuery(q) });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ email, password }) => api.post('/api/auth/login', { email: email.trim(), password }),
    onSuccess: (user) => resetSession(queryClient, user),
  });
}

// The backend does not open a session on registration (G2), so a successful registration logs in right away.
export function useRegister() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ email, password, confirmPassword }) => {
      const body = { email: email.trim(), password, confirmPassword };
      await api.post('/api/auth/register', body);
      return api.post('/api/auth/login', { email: body.email, password });
    },
    onSuccess: (user) => resetSession(queryClient, user),
  });
}

// C10: even if the request fails (session already gone), the client forgets the user.
export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.post('/api/auth/logout'),
    onSettled: () => resetSession(queryClient, null),
  });
}

export function useRequestPasswordReset() {
  return useMutation({
    mutationFn: ({ email }) => api.post('/api/auth/password-reset/request', { email: email.trim() }),
  });
}

export function useConfirmPasswordReset() {
  return useMutation({
    mutationFn: ({ token, newPassword, confirmPassword }) =>
      api.post('/api/auth/password-reset/confirm', { token, newPassword, confirmPassword }),
  });
}

export function useUnblockRequest() {
  return useMutation({
    mutationFn: ({ email, password, reason }) =>
      api.post('/api/auth/unblock-request', { email: email.trim(), password, reason: reason.trim() }),
  });
}
