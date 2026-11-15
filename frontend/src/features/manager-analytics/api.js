import { useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

// kind: 'demand' | 'stock' | 'ratings'. Stock is a current snapshot and takes no period.
export function useAnalytics(kind, { from, to, enabled = true } = {}) {
  const query = kind === 'stock' ? undefined : { from, to };
  return useQuery({
    enabled,
    queryKey: ['manager-analytics', kind, query ?? null],
    queryFn: ({ signal }) => api.get(`/api/manager/analytics/${kind}`, { query, signal }),
  });
}
