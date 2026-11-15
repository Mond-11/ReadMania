import { Badge, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { BOOK_FORMS, formatCount } from '../../../shared/format.js';
import { TERMS } from '../../../shared/labels.js';
import { useGenres } from '../../../shared/hooks/useGenres.js';
import { EmptyState, ErrorState, PageLoader } from '../../../shared/ui/index.js';

// Temporary smoke-test page; block 1 replaces it.
export default function CatalogPage() {
  useDocumentTitle(`Каталог — ${TERMS.appName}`);
  const genres = useGenres();

  return (
    <Stack gap="md">
      <Title order={1}>Каталог</Title>
      <Paper p="md">
        <Text fw={600} mb="xs">Жанри (перевірка зв’язку з бекендом)</Text>
        {genres.isPending && <PageLoader minHeight={80} />}
        {genres.isError && <ErrorState error={genres.error} onRetry={genres.refetch} />}
        {genres.isSuccess && genres.data.length === 0 && <EmptyState title="Жанрів поки немає" />}
        {genres.isSuccess && genres.data.length > 0 && (
          <Group gap="xs">
            {genres.data.map((g) => (
              <Badge key={g.id} variant="light" color="brand" size="lg" radius="sm" styles={{ root: { textTransform: 'none' } }}>
                {g.name} · {formatCount(g.booksCount ?? 0, BOOK_FORMS)}
              </Badge>
            ))}
          </Group>
        )}
      </Paper>
    </Stack>
  );
}
