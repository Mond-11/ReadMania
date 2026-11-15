import { ActionIcon, AspectRatio, Badge, Button, Grid, Group, Image, Rating, Stack, Text, Title, VisuallyHidden } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, useParams } from 'react-router';
import { formatNumber } from '../../../shared/format.js';
import { CATALOG, COMMON, MANAGER_BOOK, TERMS } from '../../../shared/labels.js';
import { useCurrentUser } from '../../auth/index.js';
import { EmptyState, ErrorState, IconPencil, Money, PageHeader, PageLoader, Section } from '../../../shared/ui/index.js';
import { DeleteBookButton } from '../../manager-books/index.js';
import { useBook } from '../api.js';
import { parseBookId } from '../bookSlug.js';
import { AddToCartButton } from '../components/AddToCartButton.jsx';

export default function BookDetailsPage() {
  const { slug } = useParams();
  const id = parseBookId(slug);
  const book = useBook(id);
  const { isManager } = useCurrentUser();

  useDocumentTitle(book.data ? `${book.data.title} — ${TERMS.appName}` : TERMS.appName);

  const breadcrumbs = [
    { label: CATALOG.title, to: '/' },
    { label: book.data?.title ?? CATALOG.title },
  ];

  if (id == null) {
    return (
      <>
        <PageHeader title={CATALOG.title} breadcrumbs={breadcrumbs} />
        <EmptyState
          title={CATALOG.notFoundTitle}
          hint={CATALOG.notFoundHint}
          action={<Button component={Link} to="/" variant="default">{CATALOG.backToCatalog}</Button>}
        />
      </>
    );
  }

  if (book.isPending) {
    return (
      <>
        <PageHeader title={CATALOG.title} breadcrumbs={breadcrumbs} />
        <PageLoader />
      </>
    );
  }

  if (book.isError) {
    if (book.error?.status === 404) {
      return (
        <>
          <PageHeader title={CATALOG.title} breadcrumbs={breadcrumbs} />
          <EmptyState
            title={CATALOG.notFoundTitle}
            hint={CATALOG.notFoundHint}
            action={<Button component={Link} to="/" variant="default">{CATALOG.backToCatalog}</Button>}
          />
        </>
      );
    }
    return (
      <>
        <PageHeader title={CATALOG.title} breadcrumbs={breadcrumbs} />
        <ErrorState error={book.error} onRetry={book.refetch} />
      </>
    );
  }

  const data = book.data;
  const outOfStock = data.stock <= 0;
  const authors = data.authors?.length ? data.authors.map((a) => a.name).join(', ') : CATALOG.authorUnknown;
  const hasRating = data.ratingsCount > 0 && data.averageRating != null;

  return (
    <>
      <PageHeader
        title={<VisuallyHidden>{data.title}</VisuallyHidden>}
        breadcrumbs={breadcrumbs}
      />
      <Grid gap="lg" align="flex-start">
        <Grid.Col span={{ base: 12, sm: 4 }}>
          <AspectRatio ratio={2 / 3} maw={320} mx={{ base: 'auto', sm: 0 }} style={{ background: 'var(--rm-bg)' }}>
            {data.hasCover && data.coverUrl ? (
              <Image
                src={data.coverUrl}
                alt={data.title}
                fit="cover"
                style={outOfStock ? { opacity: 0.6 } : undefined}
              />
            ) : (
              <Text c="dimmed" size="sm" ta="center" p="md">{CATALOG.coverMissing}</Text>
            )}
          </AspectRatio>
        </Grid.Col>

        <Grid.Col span={{ base: 12, sm: 8 }}>
          <Stack gap="sm">
            <Group justify="space-between" align="flex-start" gap="sm" wrap="nowrap">
              <Title order={2} size="h3" style={{ flex: 1, minWidth: 0 }}>{data.title}</Title>
              {isManager && (
                <Group gap="xs" wrap="nowrap">
                  <Button component={Link} to={`/manager/books/${data.id}/edit`} variant="default" visibleFrom="sm">{MANAGER_BOOK.editAction}</Button>
                  <ActionIcon component={Link} to={`/manager/books/${data.id}/edit`} variant="default" size="lg" hiddenFrom="sm" aria-label={MANAGER_BOOK.editAction}>
                    <IconPencil />
                  </ActionIcon>
                  <DeleteBookButton book={data} />
                </Group>
              )}
            </Group>
            <Text size="lg">{authors}</Text>

            {data.genre?.name && (
              <Group gap={4}>
                <Badge variant="light" color="brand" radius="sm" styles={{ root: { textTransform: 'none' } }}>
                  {data.genre.name}
                </Badge>
              </Group>
            )}

            <Group gap={6} wrap="nowrap" align="center">
              {hasRating ? (
                <>
                  <Rating value={data.averageRating} fractions={4} readOnly size="sm" aria-hidden="true" />
                  <Text size="sm">
                    <VisuallyHidden>{CATALOG.ratingOf}: </VisuallyHidden>
                    {Number(data.averageRating).toFixed(1)} ({data.ratingsCount})
                  </Text>
                </>
              ) : (
                <Text size="sm" c="dimmed">{CATALOG.noRatings}</Text>
              )}
            </Group>

            <Money value={data.price} fw={700} size="xl" />

            <Group justify="space-between" align="flex-end" gap="sm" wrap="wrap">
              <AddToCartButton book={data} fullWidth={false} />
              {outOfStock ? (
                <Badge color="red" variant="light" radius="sm" styles={{ root: { textTransform: 'none' } }}>
                  {CATALOG.outOfStock}
                </Badge>
              ) : (
                <Text size="sm" c="dimmed">{CATALOG.inStockCount} {formatNumber(data.stock)} {CATALOG.pcs}</Text>
              )}
            </Group>

            {data.description && (
              <Section title={CATALOG.annotationTitle}>
                <Text style={{ whiteSpace: 'pre-wrap' }}>{data.description}</Text>
              </Section>
            )}

            <Section title={CATALOG.detailsTitle}>
              <Stack gap={6}>
                <Group justify="space-between" gap="sm">
                  <Text c="dimmed">{CATALOG.publisher}</Text>
                  <Text>{data.publisher || COMMON.emptyDash}</Text>
                </Group>
                <Group justify="space-between" gap="sm">
                  <Text c="dimmed">{CATALOG.publicationYear}</Text>
                  <Text>{data.publicationYear ?? COMMON.emptyDash}</Text>
                </Group>
                <Group justify="space-between" gap="sm">
                  <Text c="dimmed">{CATALOG.isbn}</Text>
                  <Text>{data.isbn || COMMON.emptyDash}</Text>
                </Group>
              </Stack>
            </Section>
          </Stack>
        </Grid.Col>
      </Grid>
    </>
  );
}
