import { Divider, Group, Stack, Text } from '@mantine/core';
import { POINT_FORMS, formatCount } from '../../../shared/format.js';
import { CHECKOUT } from '../../../shared/labels.js';
import { Money, Section } from '../../../shared/ui/index.js';

function Row({ label, children }) {
  return (
    <Group justify="space-between" gap="sm" wrap="nowrap" align="baseline">
      <Text size="sm" c="dimmed">{label}</Text>
      {children}
    </Group>
  );
}

export function OrderSummary({ preview }) {
  return (
    <Section title={CHECKOUT.summarySection}>
      <Stack gap="xs" component="ul" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {preview.items.map((item) => (
          <Group key={item.bookId} component="li" justify="space-between" gap="sm" wrap="nowrap" align="baseline">
            <Text size="sm" style={{ minWidth: 0 }}>
              {item.title} × {item.quantity}
            </Text>
            <Money value={item.subtotal} size="sm" />
          </Group>
        ))}
      </Stack>
      <Divider />
      <Row label={CHECKOUT.itemsTotal}><Money value={preview.itemsTotal} /></Row>
      <Row label={`${CHECKOUT.discount} (${formatCount(preview.pointsUsed, POINT_FORMS)})`}>
        <Money value={-preview.discountAmount} />
      </Row>
      <Divider />
      <Group justify="space-between" gap="sm" wrap="nowrap" align="baseline">
        <Text fw={600}>{CHECKOUT.totalToPay}</Text>
        <Money value={preview.totalToPay} fw={700} size="xl" />
      </Group>
    </Section>
  );
}
