import { Group, Stack, Text } from '@mantine/core';

// Label/value rows, the same look as «Деталі видання» on the book page. Rows with an empty value are skipped.
export function InfoList({ rows }) {
  return (
    <Stack gap={6} component="dl" m={0}>
      {rows
        .filter((row) => row && row.value !== null && row.value !== undefined && row.value !== '')
        .map((row) => (
          <Group key={row.label} justify="space-between" gap="sm" wrap="nowrap" align="flex-start">
            <Text component="dt" c="dimmed">{row.label}</Text>
            <Text component="dd" m={0} ta="right" style={{ overflowWrap: 'anywhere' }}>
              {row.value}
            </Text>
          </Group>
        ))}
    </Stack>
  );
}
