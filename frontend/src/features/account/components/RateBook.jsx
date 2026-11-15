import { Button, Group, Rating, Text, VisuallyHidden } from '@mantine/core';
import { useState } from 'react';
import { ORDERS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { useRateBook } from '../../orders/index.js';

const symbolLabel = (i) => `${i} з 5`;

// C6: a rated book shows its rating; an unrated one in a delivered order shows the 1–5 scale.
// The rating is saved only after «Оцінити» (an unconfirmed rating is not stored).
export function RateBook({ item, orderId, canRate }) {
  const [value, setValue] = useState(0);
  const rate = useRateBook();

  if (item.userRating) {
    return (
      <Group gap={6} wrap="nowrap">
        <Rating value={item.userRating} readOnly size="sm" aria-hidden="true" />
        <Text size="sm">
          <VisuallyHidden>{ORDERS.rated}: </VisuallyHidden>
          {item.userRating} / 5
        </Text>
      </Group>
    );
  }
  if (!canRate) return <Text size="sm" c="dimmed">{ORDERS.notRated}</Text>;

  const submit = () =>
    rate.mutate(
      { bookId: item.bookId, orderId, value },
      { onSuccess: () => notifySuccess(ORDERS.rateDone), onError: (e) => notifyError(e) },
    );

  return (
    <Group gap="xs" wrap="nowrap">
      <Rating
        value={value}
        onChange={setValue}
        size="sm"
        getSymbolLabel={symbolLabel}
        aria-label={ORDERS.rateLabel(item.titleSnapshot)}
        readOnly={rate.isPending}
      />
      <Button size="compact-sm" variant="default" onClick={submit} disabled={value === 0} loading={rate.isPending}>
        {ORDERS.rateSubmit}
      </Button>
    </Group>
  );
}
