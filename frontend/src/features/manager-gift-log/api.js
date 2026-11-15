import { useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';

export function useGiftLog({ institutionId, dateFrom, dateTo, enabled = true } = {}) {
  return useQuery({
    enabled,
    queryKey: ['manager-gift-log', { institutionId: institutionId ?? null, dateFrom: dateFrom ?? null, dateTo: dateTo ?? null }],
    queryFn: ({ signal }) => api.get('/api/manager/gift-log', { query: { institutionId, dateFrom, dateTo }, signal }),
  });
}
