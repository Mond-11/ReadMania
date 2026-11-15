import { Stack, Text } from '@mantine/core';

export function EmptyState({ title, hint, action }) {
  return (
    <Stack align="center" gap="xs" py="xl" ta="center">
      <Text fw={600}>{title}</Text>
      {hint && <Text c="dimmed" size="sm">{hint}</Text>}
      {action}
    </Stack>
  );
}
