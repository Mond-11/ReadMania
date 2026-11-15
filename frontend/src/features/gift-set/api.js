import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { CART_KEY } from '../cart/index.js';

// Business failures (budget too low / not enough books) come back as 400 with a Ukrainian `detail`; the page shows it as is.
export function useGenerateGiftSet() {
  return useMutation({
    mutationFn: ({ budget, genreIds, bookCount, excludeBookIds }) =>
      api.post('/api/gift-sets/generate', {
        budget,
        genreIds,
        ...(bookCount ? { bookCount } : {}),
        ...(excludeBookIds && excludeBookIds.length > 0 ? { excludeBookIds } : {}),
      }),
  });
}

export function useReplaceCandidates() {
  return useMutation({
    mutationFn: (body) => api.post('/api/gift-sets/replace-candidates', body),
  });
}

export function useReplaceGiftSetBook() {
  return useMutation({
    mutationFn: (body) => api.post('/api/gift-sets/replace', body),
  });
}

// No bulk endpoint: books go in one by one. Stops at the first failure and reports how many were added.
export function useAddGiftSetToCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (bookIds) => {
      let added = 0;
      try {
        for (const bookId of bookIds) {
          const cart = await api.post('/api/cart/items', { bookId, quantity: 1 });
          queryClient.setQueryData(CART_KEY, cart);
          added += 1;
        }
      } catch (error) {
        error.added = added;
        throw error;
      }
      return added;
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: CART_KEY }),
  });
}
