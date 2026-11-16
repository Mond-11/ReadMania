import { Box, Group, Paper, SimpleGrid, Text, Title } from '@mantine/core';
import { MANAGER_ANALYTICS as L } from '../../../shared/labels.js';

const TONE = { danger: 'red.8', warning: 'yellow.9', success: 'green.8' };

// Key numbers of the open tab (cards from buildSummary): the overall picture before the charts.
export default function AnalyticsSummary({ cards, caption }) {
  if (!cards.length) return null;
  return (
    <Box component="section" aria-label={L.summaryTitle} mb="lg">
      <Group gap="xs" align="baseline" mb="xs">
        <Title order={3} fz="h4">{L.summaryTitle}</Title>
        <Text size="sm" c="dimmed">{caption}</Text>
      </Group>
      <SimpleGrid cols={{ base: 1, xs: 2, md: cards.length }} spacing="sm">
        {cards.map((c) => (
          <Paper key={c.key} withBorder p="sm">
            <Text size="sm">{c.label}</Text>
            <Text fz={26} fw={700} lh={1.3} c={TONE[c.tone]}>{c.value}</Text>
            {c.hint && <Text size="sm" c={TONE[c.hintTone] ?? 'dimmed'}>{c.hint}</Text>}
          </Paper>
        ))}
      </SimpleGrid>
    </Box>
  );
}
