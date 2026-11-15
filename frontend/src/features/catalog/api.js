import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

export function useBooks(params) {
  return useQuery({
    queryKey: ['books', params],
    queryFn: ({ signal }) => api.get('/api/catalog/books', { query: params, signal }),
    placeholderData: keepPreviousData,
  });
}

export function useBook(id) {
  return useQuery({
    queryKey: ['book', id],
    queryFn: ({ signal }) => api.get(`/api/catalog/books/${id}`, { signal }),
    enabled: id != null,
  });
}

export function useAuthors() {
  return useQuery({
    queryKey: ['authors'],
    queryFn: ({ signal }) => api.get('/api/catalog/authors', { signal }),
    staleTime: 5 * 60_000,
  });
}
