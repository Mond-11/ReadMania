import { useQuery } from '@tanstack/react-query';
import { api } from '../api/index.js';

export const INSTITUTIONS_KEY = ['institutions'];

export function useInstitutions({ type, query } = {}) {
  return useQuery({
    queryKey: [...INSTITUTIONS_KEY, { type: type ?? null, query: query ?? null }],
    queryFn: ({ signal }) => api.get('/api/institutions', { query: { type, query }, signal }),
    staleTime: 5 * 60_000,
  });
}
