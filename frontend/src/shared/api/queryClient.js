import { QueryClient } from '@tanstack/react-query';
import { ApiError } from './ApiError.js';

// Retry only network failures and 5xx, once; a 4xx is a definitive answer.
const shouldRetry = (failureCount, error) => {
  if (failureCount >= 1) return false;
  return !(error instanceof ApiError) || error.status === 0 || error.status >= 500;
};

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, refetchOnWindowFocus: false, retry: shouldRetry },
    mutations: { retry: false },
  },
});
