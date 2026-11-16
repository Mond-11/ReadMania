import { Alert, Stack, Text } from '@mantine/core';
import { CHECKOUT } from '../../../shared/labels.js';
import { Section } from '../../../shared/ui/index.js';

// Training project: deliberately fictional details (agreed with the team), no real company or bank data.
const REQUISITES = {
  recipient: 'ТОВ «ReadMania»',
  edrpou: '00000000',
  iban: 'UA00 0000 0000 0000 0000 0000 0000 0',
};

// orderId: when the order already exists (its page in the personal account), the payment purpose names it.
export function TransferRequisites({ orderId }) {
  return (
    <Section title={CHECKOUT.requisitesTitle}>
      <Alert variant="light" color="brand">{CHECKOUT.requisitesHint}</Alert>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesRecipient}</Text>
        <Text fw={600}>{REQUISITES.recipient}</Text>
      </Stack>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesEdrpou}</Text>
        <Text fw={600}>{REQUISITES.edrpou}</Text>
      </Stack>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesIban}</Text>
        <Text fw={600}>{REQUISITES.iban}</Text>
      </Stack>
      <Stack gap={4}>
        <Text size="sm" c="dimmed">{CHECKOUT.requisitesPurpose}</Text>
        <Text fw={600}>{orderId ? CHECKOUT.requisitesPurposeOrder(orderId) : CHECKOUT.requisitesPurposeValue}</Text>
      </Stack>
    </Section>
  );
}
