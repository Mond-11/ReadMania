import { BarChart } from '@mantine/charts';
import { Box, Paper, ScrollArea, SimpleGrid, Text, Title } from '@mantine/core';
import { formatNumber } from '../../../shared/format.js';
import { MANAGER_ANALYTICS as L } from '../../../shared/labels.js';

const TOP = 10;

// One chart for all tabs and both groupings: vertical bars for the first TOP rows,
// text cards (insights) above it say the same in words and flag what needs attention.
// valueKey: field with the main number; domain: fixed value axis (ratings).
export default function AnalyticsChart({ title, rows, valueKey, valueLabel, domain, insights }) {
  const top = rows.slice(0, TOP).map((r) => ({ name: r.name, [valueLabel]: Number(r[valueKey]) }));
  const first = top[0];
  return (
    <Box component="section" mb="xl" aria-label={title}>
      <Title order={3} fz="h4" mb="xs">{title}</Title>
      {insights.length > 0 && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="sm" mb="md">
          {insights.map((c) => (
            <Paper key={c.key} withBorder p="sm" style={c.alert ? { borderLeft: '4px solid var(--mantine-color-red-7)' } : undefined}>
              <Text size="sm">{c.label}</Text>
              <Text fw={600} c={c.alert ? 'red.8' : undefined}>{c.value}</Text>
            </Paper>
          ))}
        </SimpleGrid>
      )}
      {top.length > 0 && (
        <>
          {rows.length > TOP && <Text size="sm" mb="xs">{L.topNote}</Text>}
          <ScrollArea type="auto">
            <Box miw={480} role="img" aria-label={`${L.chartSummary} ${first.name}, ${formatNumber(first[valueLabel])}`}>
              <BarChart
                h={340}
                data={top}
                dataKey="name"
                orientation="horizontal"
                series={[{ name: valueLabel, color: 'brand.5' }]}
                withLegend={false}
                withBarValueLabel
                valueFormatter={formatNumber}
                xAxisProps={{ angle: -30, textAnchor: 'end', interval: 0, height: 90 }}
                yAxisProps={domain ? { domain } : undefined}
              />
            </Box>
          </ScrollArea>
        </>
      )}
    </Box>
  );
}
