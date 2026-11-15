import { ActionIcon, Badge, Card, Group, Stack, Text } from '@mantine/core';
import { Link } from 'react-router';
import { CART, CATALOG } from '../../../shared/labels.js';
import { IconTrash, Money } from '../../../shared/ui/index.js';

const COVER_W = 96;
const COVER_H = 144;

export function CartItemRow({ item, onChangeQuantity, onRemove, updating, removing }) {
  const href = `/books/${item.bookId}`;
  const atMin = item.quantity <= 1;
  const atMax = item.quantity >= item.stock;

  return (
    <Card component="li" padding="sm" pos="relative" style={{ listStyle: 'none' }}>
      <ActionIcon
        variant="subtle"
        color="red"
        aria-label={CART.remove}
        loading={removing}
        onClick={onRemove}
        style={{ position: 'absolute', top: 'var(--mantine-spacing-xs)', right: 'var(--mantine-spacing-xs)' }}
      >
        <IconTrash />
      </ActionIcon>

      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--mantine-spacing-sm)' }}>
        {/* Hard size (width and height) so a real cover can never stretch the box and squeeze the text out. */}
        <div style={{ width: COVER_W, height: COVER_H, flex: `0 0 ${COVER_W}px`, overflow: 'hidden', background: 'var(--rm-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {item.hasCover && item.coverUrl ? (
            <img src={item.coverUrl} alt="" loading="lazy" width={COVER_W} height={COVER_H} style={{ width: COVER_W, height: COVER_H, objectFit: 'cover', display: 'block' }} />
          ) : (
            <Text c="dimmed" size="xs" ta="center" p={4}>{CATALOG.coverMissing}</Text>
          )}
        </div>

        <Stack justify="space-between" gap="sm" style={{ flex: 1, minWidth: 0, alignSelf: 'stretch' }}>
          <Stack gap={4}>
            <Text component={Link} to={href} fw={600} td="none" c="inherit" style={{ paddingRight: 40 }}>
              {item.title}
            </Text>
            <Money value={item.price} c="dimmed" size="sm" />
            {!item.available && (
              <Badge color="red" variant="light" radius="sm" styles={{ root: { textTransform: 'none' } }}>
                {CART.unavailable}
              </Badge>
            )}
          </Stack>

          <Group justify="space-between" align="center" wrap="wrap" gap="sm">
            {item.available ? (
              <Group gap={6} align="center" wrap="nowrap">
                <ActionIcon
                  variant="default"
                  aria-label={CART.decreaseQuantity}
                  disabled={atMin || updating}
                  onClick={() => onChangeQuantity(item.quantity - 1)}
                >
                  −
                </ActionIcon>
                <Text w={28} ta="center" aria-label={CART.quantity}>{item.quantity}</Text>
                <ActionIcon
                  variant="default"
                  aria-label={CART.increaseQuantity}
                  disabled={atMax || updating}
                  onClick={() => onChangeQuantity(item.quantity + 1)}
                >
                  +
                </ActionIcon>
              </Group>
            ) : <span />}

            <Group gap={6} align="baseline">
              <Text size="sm" c="dimmed">{CART.subtotal}:</Text>
              <Money value={item.subtotal} fw={700} size="lg" />
            </Group>
          </Group>
        </Stack>
      </div>
    </Card>
  );
}
