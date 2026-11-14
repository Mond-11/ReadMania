import { useQuery } from '@tanstack/react-query';
import { api } from '../api/index.js';

export const GENRES_KEY = ['genres'];

export function useGenres() {
  return useQuery({
    queryKey: GENRES_KEY,
    queryFn: ({ signal }) => api.get('/api/catalog/genres', { signal }),
    staleTime: 5 * 60_000,
  });
}
