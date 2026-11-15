import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

export function useBooks(params) {
  return useQuery({
    queryKey: ['books', params],
    queryFn: ({ signal }) => api.get('/api/catalog/books', { query: params, signal }),
    placeholderData: keepPreviousData,
  });
}

export function useAuthors() {
  return useQuery({
    queryKey: ['authors'],
    queryFn: ({ signal }) => api.get('/api/catalog/authors', { signal }),
    staleTime: 5 * 60_000,
  });
}

// TODO(B-auth): replace with useCurrentUser() from features/auth once B delivers it.
// 401 means Guest, not an error.
export function useSession() {
  const query = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: async ({ signal }) => {
      try {
        return await api.get('/api/auth/me', { signal });
      } catch (e) {
        if (e?.status === 401) return null;
        throw e;
      }
    },
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
