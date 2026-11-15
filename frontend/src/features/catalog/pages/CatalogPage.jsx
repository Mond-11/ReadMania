import { Button, Grid, Group, Pagination, Select, SimpleGrid, Stack, Text } from '@mantine/core';
import { useDocumentTitle, useMediaQuery } from '@mantine/hooks';
import { useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router';
import { formatCount, BOOK_FORMS } from '../../../shared/format.js';
import { CATALOG, MANAGER_BOOK, MANAGER_CATALOG, TERMS } from '../../../shared/labels.js';
import { useCurrentUser } from '../../auth/index.js';
import { EmptyState, ErrorState, PageHeader, PageLoader, Section } from '../../../shared/ui/index.js';
import { useBooks } from '../api.js';
import { BookCard } from '../components/BookCard.jsx';
import { CatalogFilters } from '../components/CatalogFilters.jsx';
import { PAGE_SIZES, hasActiveFilters, parseFilters, toApiQuery, toSearchParams } from '../filters.js';

export default function CatalogPage() {
  useDocumentTitle(`${CATALOG.title} — ${TERMS.appName}`);
  const { isManager } = useCurrentUser();
  const sidebar = useMediaQuery('(min-width: 75em)');
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseFilters(searchParams), [searchParams]);
  const books = useBooks(useMemo(() => toApiQuery(filters), [filters]));

  // Any filter change goes back to page 1; plain page change keeps the filters.
  const update = (patch) => {
    const next = { ...filters, page: 1, ...patch };
    setSearchParams(toSearchParams(next));
  };
  // Page size is a view setting, not a filter: reset keeps it.
  const reset = () => setSearchParams(toSearchParams({ ...parseFilters(new URLSearchParams()), size: filters.size }));

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
      <PageHeader
        title={CATALOG.title}
        description={CATALOG.description}
        actions={isManager && (
          <Group gap="sm">
            <Button component={Link} to="/manager/books/import" variant="default">{MANAGER_CATALOG.importAction}</Button>
            <Button component={Link} to="/manager/genres" variant="default">{MANAGER_CATALOG.genresAction}</Button>            <Button component={Link} to="/manager/books/new">{MANAGER_BOOK.addAction}</Button>
          </Group>
        )}
      />
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
            <Group justify="space-between" align="center" gap="sm">
              <Text size="sm" c="dimmed" aria-live="polite">
                {CATALOG.found} {formatCount(page.totalElements, BOOK_FORMS)}
              </Text>
              <Group gap="xs" wrap="nowrap">
                <Text component="label" htmlFor="catalog-page-size" size="sm" c="dimmed">{CATALOG.pageSize}:</Text>
                <Select
                  id="catalog-page-size"
                  aria-label={CATALOG.pageSize}
                  data={PAGE_SIZES.map(String)}
                  value={String(filters.size)}
                  onChange={(value) => value && update({ size: Number(value) })}
                  allowDeselect={false}
                  w={84}
                />
              </Group>
            </Group>
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
