import { useQuery } from '@tanstack/react-query';
import { api } from '../api/index.js';

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
