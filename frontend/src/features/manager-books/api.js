import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/index.js';
import { GENRES_KEY } from '../../shared/hooks/useGenres.js';

const norm = (s) => s.trim().toLocaleLowerCase('uk');

// Finds an existing entity by name or creates it. On 409 (created meanwhile) the list is reloaded and searched again.
async function resolveByName(queryClient, { name, key, listPath, createPath, bodyOf }) {
  const find = (list) => list?.find((x) => norm(x.name) === norm(name));
  const cached = find(queryClient.getQueryData(key));
  if (cached) return cached.id;
  try {
    return (await api.post(createPath, bodyOf(name.trim()))).id;
  } catch (error) {
    if (error?.status !== 409) throw error;
    const fresh = find(await queryClient.fetchQuery({ queryKey: key, queryFn: ({ signal }) => api.get(listPath, { signal }), staleTime: 0 }));
    if (fresh) return fresh.id;
    throw error;
  }
}

// The cover goes as a separate multipart request after the book exists. Its failure must not hide that the book was saved.
export function useSaveBook(bookId) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ values, coverFile }) => {
      const genreId = await resolveByName(queryClient, {
        name: values.genre,
        key: GENRES_KEY,
        listPath: '/api/catalog/genres',
        createPath: '/api/manager/genres',
        bodyOf: (name) => ({ name }),
      });
      const authorIds = [];
      for (const name of values.authors) {
        const id = await resolveByName(queryClient, {
          name,
          key: ['authors'],
          listPath: '/api/catalog/authors',
          createPath: '/api/manager/authors',
          bodyOf: (fullName) => ({ fullName }),
        });
        if (!authorIds.includes(id)) authorIds.push(id);
      }

      const body = {
        title: values.title.trim(),
        publisher: values.publisher.trim(),
        publicationYear: Number(values.publicationYear),
        isbn: values.isbn.trim(),
        price: Number(values.price),
        stock: Number(values.stock),
        description: values.description.trim(),
        genreId,
        authorIds,
      };
      const book = bookId ? await api.put(`/api/manager/books/${bookId}`, body) : await api.post('/api/manager/books', body);

      let coverFailed = false;
      if (coverFile) {
        try {
          const form = new FormData();
          form.append('file', coverFile);
          await api.post(`/api/manager/books/${book.id}/cover`, form);
        } catch {
          coverFailed = true;
        }
      }
      return { book, coverFailed };
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['books'] });
      queryClient.invalidateQueries({ queryKey: ['book'] });
      queryClient.invalidateQueries({ queryKey: GENRES_KEY });
      queryClient.invalidateQueries({ queryKey: ['authors'] });
    },
  });
}
