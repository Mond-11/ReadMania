import { ActionIcon, Badge, Button, Card, Group, Image, Stack, Text } from '@mantine/core';
import { Link } from 'react-router';
import { CATALOG, GIFT_SET } from '../../../shared/labels.js';
import { IconTrash, Money } from '../../../shared/ui/index.js';

export function GiftSetBookRow({ book, onReplace, onRemove, disabled }) {
  const authors = book.authors?.map((a) => a.name).join(', ') || CATALOG.authorUnknown;

  return (
    <Card component="li" padding="sm" pos="relative" style={{ listStyle: 'none' }}>
      <ActionIcon
        variant="subtle"
        color="red"
        aria-label={GIFT_SET.removeBook(book.title)}
        disabled={disabled}
        onClick={onRemove}
        style={{ position: 'absolute', top: 'var(--mantine-spacing-xs)', right: 'var(--mantine-spacing-xs)' }}
      >
        <IconTrash />
      </ActionIcon>
      <Group align="flex-start" wrap="nowrap" gap="sm">
        {/* Fixed-width cover: stretched to the card height it grew wide and squeezed the title. */}
        <div style={{ width: 72, aspectRatio: '2 / 3', flexShrink: 0, overflow: 'hidden', borderRadius: 4, background: 'var(--rm-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {book.coverUrl ? (
            <Image src={book.coverUrl} alt="" loading="lazy" fit="cover" h="100%" w="100%" />
          ) : (
            <Text c="dimmed" size="xs" ta="center" p={4}>{CATALOG.coverMissing}</Text>
          )}
        </div>

        <Stack justify="space-between" gap="sm" style={{ flex: 1, minWidth: 0 }}>
          <Stack gap={4}>
            <Text component={Link} to={`/books/${book.id}`} fw={600} td="none" c="inherit" style={{ paddingRight: 40 }}>{book.title}</Text>
            <Text size="sm" c="dimmed">{authors}</Text>
            {book.genre && (
              <Badge variant="light" radius="sm" styles={{ root: { textTransform: 'none', alignSelf: 'flex-start' } }}>
                {book.genre.name}
              </Badge>
            )}
          </Stack>
          <Group justify="space-between" align="center" wrap="wrap" gap="sm">
            <Button variant="default" size="xs" onClick={onReplace} disabled={disabled}>{GIFT_SET.replace}</Button>
            <Money value={book.price} fw={700} size="lg" />
          </Group>
        </Stack>
      </Group>
    </Card>
  );
}
