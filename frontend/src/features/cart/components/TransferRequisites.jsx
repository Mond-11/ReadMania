import { Alert, Stack, Text } from '@mantine/core';
import { CHECKOUT } from '../../../shared/labels.js';
import { Section } from '../../../shared/ui/index.js';

// TODO(requisites): placeholder values; the real bank details are not in the API or docs. Ask what to show.
const REQUISITES = {
  recipient: 'ReadMania',
  iban: 'UA00 0000 0000 0000 0000 0000 0000 0',
};

export function TransferRequisites() {
  return (
    <Section title={CHECKOUT.requisitesTitle}>
      <Alert variant="light" color="brand">{CHECKOUT.requisitesHint}</Alert>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesRecipient}</Text>
        <Text fw={600}>{REQUISITES.recipient}</Text>
      </Stack>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesIban}</Text>
        <Text fw={600}>{REQUISITES.iban}</Text>
      </Stack>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesPurpose}</Text>
        <Text fw={600}>{CHECKOUT.requisitesPurposeValue}</Text>
      </Stack>
    </Section>
  );
}
