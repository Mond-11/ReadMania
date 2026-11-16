import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { CURRENT_USER_KEY } from '../auth/index.js';

// C5. Every change needs the current password. The answer is the updated user, so the header and /me stay in sync.
export function useChangeEmail() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ email, currentPassword }) => api.put('/api/account', { email: email.trim(), currentPassword }),
    onSuccess: (user) => queryClient.setQueryData(CURRENT_USER_KEY, user),
  });
}

export function useChangePassword() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ currentPassword, newPassword, confirmPassword }) =>
      api.put('/api/account/password', { currentPassword, newPassword, confirmPassword }),
    onSuccess: (user) => queryClient.setQueryData(CURRENT_USER_KEY, user),
  });
}

// C7: balance and the bonus log come in one response.
export function useLoyalty() {
  return useQuery({
    queryKey: ['loyalty'],
    queryFn: ({ signal }) => api.get('/api/loyalty/balance', { signal }),
  });
}

// Challenges with the user's progress: all active ones plus the ones this user completed (also already ended).
export function useChallenges() {
  return useQuery({
    queryKey: ['challenges', 'my'],
    queryFn: ({ signal }) => api.get('/api/challenges', { signal }),
  });
}
