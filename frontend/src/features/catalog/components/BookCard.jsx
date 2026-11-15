import { AspectRatio, Badge, Card, Group, Image, Rating, Stack, Text, VisuallyHidden } from '@mantine/core';
import { Link } from 'react-router';
import { CATALOG } from '../../../shared/labels.js';
import { Money } from '../../../shared/ui/index.js';
import { bookHref } from '../bookSlug.js';
import { AddToCartButton } from './AddToCartButton.jsx';

export function BookCard({ book }) {
  const outOfStock = book.stock <= 0;
  const authors = book.authors?.length ? book.authors.map((a) => a.name).join(', ') : CATALOG.authorUnknown;
  const hasRating = book.ratingsCount > 0 && book.averageRating != null;
  const href = bookHref(book);

  return (
    <Card component="article" padding="sm" h="100%" style={{ display: 'flex', flexDirection: 'column' }}>
      <Card.Section component={Link} to={href} tabIndex={-1} aria-hidden="true">
        <AspectRatio ratio={2 / 3} style={{ background: 'var(--rm-bg)' }}>
          {book.hasCover && book.coverUrl ? (
            <Image src={book.coverUrl} alt="" loading="lazy" fit="cover" style={outOfStock ? { opacity: 0.6 } : undefined} />
          ) : (
            <Text c="dimmed" size="sm" ta="center" p="md">{CATALOG.coverMissing}</Text>
          )}
        </AspectRatio>
      </Card.Section>

      <Stack gap={6} mt="sm" style={{ flex: 1 }}>
        <Text component={Link} to={href} fw={600} lh={1.3} lineClamp={2} c="inherit" td="none" title={book.title}>
          {book.title}
        </Text>
        <Text size="sm" c="dimmed" lineClamp={2}>{authors}</Text>
        {book.genre?.name && (
          <Group gap={4}>
            <Badge variant="light" color="brand" radius="sm" styles={{ root: { textTransform: 'none' } }}>
              {book.genre.name}
            </Badge>
          </Group>
        )}
        <Group gap={6} wrap="nowrap" align="center">
          {hasRating ? (
            <>
              <Rating value={book.averageRating} fractions={4} readOnly size="sm" aria-hidden="true" />
              <Text size="sm">
                <VisuallyHidden>{CATALOG.ratingOf}: </VisuallyHidden>
                {Number(book.averageRating).toFixed(1)} ({book.ratingsCount})
              </Text>
            </>
          ) : (
            <Text size="sm" c="dimmed">{CATALOG.noRatings}</Text>
          )}
        </Group>
        <Group justify="space-between" align="center" mt="auto" gap="xs">
          <Money value={book.price} fw={700} size="lg" />
          {outOfStock ? (
            <Badge color="red" variant="light" radius="sm" styles={{ root: { textTransform: 'none' } }}>
              {CATALOG.outOfStock}
            </Badge>
          ) : (
            <Text size="sm" c="dimmed">{CATALOG.inStockCount} {book.stock} {CATALOG.pcs}</Text>
          )}
        </Group>
      </Stack>

      <div style={{ marginTop: 'var(--mantine-spacing-sm)' }}>
        <AddToCartButton book={book} />
      </div>
    </Card>
  );
}
