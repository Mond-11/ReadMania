import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

const KEY = ['manager-challenges'];

export function useManagerChallenges(status) {
  return useQuery({
    queryKey: [...KEY, 'list', status ?? null],
    queryFn: ({ signal }) => api.get('/api/manager/challenges', { query: { status }, signal }),
  });
}

// Users see challenges in their account (C7); those lists must refresh too.
function invalidateAll(queryClient) {
  queryClient.invalidateQueries({ queryKey: KEY });
  queryClient.invalidateQueries({ queryKey: ['challenges'] });
}

// M4.1 (no id) and M4.2 (with id). The backend changes users' progress when the condition changes.
export function useSaveChallenge() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...body }) =>
      id ? api.put(`/api/manager/challenges/${id}`, body) : api.post('/api/manager/challenges', body),
    onSuccess: () => invalidateAll(queryClient),
  });
}

// M4.3: the answer says how many users were notified.
export function useFinishChallenge() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => api.post(`/api/manager/challenges/${id}/finish`),
    onSuccess: () => invalidateAll(queryClient),
  });
}
