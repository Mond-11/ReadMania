import { Group, Paper, Stack, Title } from '@mantine/core';
import { useId } from 'react';

export function Section({ title, actions, children, ...props }) {
  const titleId = useId();
  return (
    <Paper component="section" p="md" aria-labelledby={title ? titleId : undefined} {...props}>
      <Stack gap="md">
        {(title || actions) && (
          <Group justify="space-between" align="center" wrap="wrap" gap="sm">
            {title && (
              <Title order={2} size="h4" id={titleId}>
                {title}
              </Title>
            )}
            {actions && <Group gap="sm">{actions}</Group>}
          </Group>
        )}
        {children}
      </Stack>
    </Paper>
  );
}
