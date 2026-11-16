import { BarChart } from '@mantine/charts';
import { Box, Button, Collapse, Paper, SimpleGrid, Table, Text, Title, UnstyledButton } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useMemo, useState } from 'react';
import { formatNumber } from '../../../shared/format.js';
import { MANAGER_ANALYTICS as L } from '../../../shared/labels.js';
import { percentChange, signed } from '../summary.js';

const TOP = 10;
// Up to this many bars the names fit horizontally; more bars get slanted labels.
const FLAT_LABELS = 6;
const LABEL_CHARS = 18;

const short = (name) => (name.length > LABEL_CHARS ? `${name.slice(0, LABEL_CHARS - 1)}…` : name);

// One chart for all tabs and both groupings: vertical bars for the first TOP rows,
// text cards (insights) above it say the same in words and flag what needs attention.
// Below it a collapsible table lists every row, so nothing beyond the top 10 is hidden.
// valueKey: field with the main number; domain: fixed value axis (ratings);
// share: show each row's share of the total; prevRows: the previous period, for the «Зміна» column.
export default function AnalyticsChart({ title, rows, valueKey, valueLabel, domain, insights, share, prevRows }) {
  const top = rows.slice(0, TOP).map((r) => ({ name: r.name, [valueLabel]: Number(r[valueKey]) }));
  const first = top[0];
  const flat = top.length <= FLAT_LABELS;
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
          {/* A plain overflow box: inside ScrollArea the chart did not stretch to the full width. */}
          <Box style={{ overflowX: 'auto' }}>
            <Box miw={480} role="img" aria-label={`${L.chartSummary} ${first.name}, ${formatNumber(first[valueLabel])}`}>
              <BarChart
                h={320}
                data={top}
                dataKey="name"
                orientation="horizontal"
                series={[{ name: valueLabel, color: 'brand.5' }]}
                withLegend={false}
                withBarValueLabel
                valueFormatter={formatNumber}
                tickLine="none"
                xAxisProps={{
                  interval: 0,
                  tickFormatter: short,
                  fontSize: 13,
                  ...(flat ? { height: 40 } : { angle: -30, textAnchor: 'end', height: 100 }),
                }}
                yAxisProps={domain ? { domain } : undefined}
              />
            </Box>
          </Box>
          <RowsTable title={title} rows={rows} valueKey={valueKey} valueLabel={valueLabel} share={share} prevRows={prevRows} />
        </>
      )}
    </Box>
  );
}

function RowsTable({ title, rows, valueKey, valueLabel, share, prevRows }) {
  const [opened, { toggle }] = useDisclosure(false);
  const [sort, setSort] = useState({ key: 'value', dir: 'desc' });

  const total = share ? rows.reduce((acc, r) => acc + Number(r[valueKey]), 0) : 0;
  const prevById = useMemo(() => (prevRows ? new Map(prevRows.map((r) => [r.id, Number(r[valueKey])])) : null), [prevRows, valueKey]);
  const withRatings = rows.some((r) => r.ratingsCount != null);

  const columns = [
    { key: 'name', label: L.colName, get: (r) => r.name },
    { key: 'value', label: valueLabel, get: (r) => Number(r[valueKey]), right: true, show: formatNumber },
    withRatings && { key: 'count', label: L.colRatings, get: (r) => Number(r.ratingsCount), right: true, show: formatNumber },
    share && total > 0 && {
      key: 'share', label: L.colShare, right: true,
      get: (r) => Number(r[valueKey]) / total,
      show: (v) => `${formatNumber(Math.round(v * 100))}%`,
    },
    prevById && {
      key: 'change', label: L.colChange, right: true,
      // null: nothing in the previous period, so a percent makes no sense; such rows sort last.
      get: (r) => percentChange(Number(r[valueKey]), prevById.get(r.id) ?? 0),
      show: (v) => (v == null ? L.changeNew : `${signed(v)}${formatNumber(Math.abs(v))}%`),
      color: (v) => (v > 0 ? 'green.8' : v < 0 ? 'red.8' : undefined),
    },
  ].filter(Boolean);

  const column = columns.find((c) => c.key === sort.key) ?? columns[1];
  const sorted = [...rows].sort((a, b) => {
    const x = column.get(a);
    const y = column.get(b);
    // Rows without a value («—») go last in both directions.
    if (x == null || y == null) return x == null ? (y == null ? 0 : 1) : -1;
    const cmp = typeof x === 'string' ? x.localeCompare(y, 'uk') : x - y;
    return sort.dir === 'asc' ? cmp : -cmp;
  });
  const onSort = (key) =>
    setSort((s) => ({ key, dir: s.key === key ? (s.dir === 'asc' ? 'desc' : 'asc') : key === 'name' ? 'asc' : 'desc' }));

  return (
    <>
      <Button variant="subtle" size="compact-sm" mt="sm" onClick={toggle} aria-expanded={opened}>
        {opened ? L.hideTable : L.showTable}
      </Button>
      <Collapse expanded={opened}>
        <Table.ScrollContainer minWidth={420} mt="xs">
          <Table striped>
            <Table.Caption>{L.tableCaption(title)}</Table.Caption>
            <Table.Thead>
              <Table.Tr>
                {columns.map((c) => (
                  <Table.Th
                    key={c.key}
                    ta={c.right ? 'right' : undefined}
                    aria-sort={sort.key === c.key ? (sort.dir === 'asc' ? 'ascending' : 'descending') : undefined}
                  >
                    <UnstyledButton onClick={() => onSort(c.key)} fw={600} fz="sm">
                      {c.label}
                      {sort.key === c.key && <span aria-hidden>{sort.dir === 'asc' ? ' ↑' : ' ↓'}</span>}
                    </UnstyledButton>
                  </Table.Th>
                ))}
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {sorted.map((r) => (
                <Table.Tr key={r.id}>
                  {columns.map((c) => {
                    const v = c.get(r);
                    return (
                      <Table.Td key={c.key} ta={c.right ? 'right' : undefined} c={c.color?.(v)}>
                        {c.show ? c.show(v) : v}
                      </Table.Td>
                    );
                  })}
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Collapse>
    </>
  );
}
