import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { INSTITUTIONS_KEY } from '../../shared/hooks/useInstitutions.js';

const KEY = ['manager-institutions'];

export function useManagerInstitutions({ type, query } = {}) {
  return useQuery({
    queryKey: [...KEY, 'list', { type: type ?? null, query: query ?? null }],
    queryFn: ({ signal }) => api.get('/api/manager/institutions', { query: { type, query }, signal }),
  });
}

export function useManagerInstitution(id) {
  return useQuery({
    queryKey: [...KEY, 'one', id],
    queryFn: ({ signal }) => api.get(`/api/manager/institutions/${id}`, { signal }),
    retry: (count, error) => error?.status !== 404 && count < 2,
  });
}

// The public list (checkout select) is cached for 5 minutes, so it is invalidated together with the manager one.
function invalidateAll(queryClient) {
  queryClient.invalidateQueries({ queryKey: KEY });
  queryClient.invalidateQueries({ queryKey: INSTITUTIONS_KEY });
}

export function useSaveInstitution() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...body }) =>
      id ? api.put(`/api/manager/institutions/${id}`, body) : api.post('/api/manager/institutions', body),
    onSuccess: () => invalidateAll(queryClient),
  });
}

export function useDeleteInstitutions() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (institutionIds) => api.delete('/api/manager/institutions', { body: { institutionIds } }),
    onSettled: () => invalidateAll(queryClient),
  });
}
