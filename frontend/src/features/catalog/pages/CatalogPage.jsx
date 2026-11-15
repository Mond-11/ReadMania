import { Grid, Group, Pagination, SimpleGrid, Stack, Text } from '@mantine/core';
import { useDocumentTitle, useMediaQuery } from '@mantine/hooks';
import { useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { formatCount, BOOK_FORMS } from '../../../shared/format.js';
import { CATALOG, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageHeader, PageLoader, Section } from '../../../shared/ui/index.js';
import { useBooks } from '../api.js';
import { BookCard } from '../components/BookCard.jsx';
import { CatalogFilters } from '../components/CatalogFilters.jsx';
import { hasActiveFilters, parseFilters, toApiQuery, toSearchParams } from '../filters.js';

export default function CatalogPage() {
  useDocumentTitle(`${CATALOG.title} — ${TERMS.appName}`);
  const sidebar = useMediaQuery('(min-width: 75em)');
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseFilters(searchParams), [searchParams]);
  const books = useBooks(useMemo(() => toApiQuery(filters), [filters]));

  // Any filter change goes back to page 1; plain page change keeps the filters.
  const update = (patch) => {
    const next = { ...filters, page: 1, ...patch };
    setSearchParams(toSearchParams(next));
  };
  const reset = () => setSearchParams(new URLSearchParams());

  const page = books.data?.page;
  const totalPages = page?.totalPages ?? 0;

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [filters.page]);

  // A stale URL like ?page=99 would show an empty page: fall back to the last one.
  useEffect(() => {
    if (books.isSuccess && totalPages > 0 && filters.page > totalPages) {
      setSearchParams(toSearchParams({ ...filters, page: totalPages }), { replace: true });
    }
  }, [books.isSuccess, totalPages, filters, setSearchParams]);

  return (
    <>
      <PageHeader title={CATALOG.title} description={CATALOG.description} />
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 3 }} style={sidebar ? { position: 'sticky', top: 80 } : undefined}>
          <Section title={CATALOG.filters}>
            <CatalogFilters filters={filters} onChange={update} onReset={reset} vertical={sidebar} />
          </Section>
        </Grid.Col>
        <Grid.Col span={{ base: 12, lg: 9 }}>
          <Stack gap="md">
        {books.isPending ? (
          <PageLoader />
        ) : books.isError ? (
          <ErrorState error={books.error} onRetry={books.refetch} />
        ) : books.data.content.length === 0 ? (
          <EmptyState
            title={CATALOG.emptyTitle}
            hint={hasActiveFilters(filters) ? CATALOG.emptyHint : undefined}
          />
        ) : (
          <>
            <Text size="sm" c="dimmed" aria-live="polite">
              {CATALOG.found} {formatCount(page.totalElements, BOOK_FORMS)}
            </Text>
            <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="md" style={{ opacity: books.isPlaceholderData ? 0.6 : 1 }}>
              {books.data.content.map((book) => (
                <BookCard key={book.id} book={book} />
              ))}
            </SimpleGrid>
            {totalPages > 1 && (
              <Group justify="center">
                <Pagination
                  total={totalPages}
                  value={filters.page}
                  onChange={(p) => update({ page: p })}
                  getControlProps={(control) => ({ 'aria-label': PAGINATION_ARIA[control] })}
                  aria-label={CATALOG.pagination}
                  component="nav"
                />
              </Group>
            )}
          </>
        )}
          </Stack>
        </Grid.Col>
      </Grid>
    </>
  );
}

const PAGINATION_ARIA = {
  first: 'Перша сторінка',
  previous: 'Попередня сторінка',
  next: 'Наступна сторінка',
  last: 'Остання сторінка',
};
