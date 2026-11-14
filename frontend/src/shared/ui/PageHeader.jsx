import { Anchor, Breadcrumbs, Group, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';
import { COMMON } from '../labels.js';

// breadcrumbs: [{ label, to }]; the last item is the current page and has no link.
export function PageHeader({ title, description, breadcrumbs, actions }) {
  return (
    <Stack gap="xs" mb="md">
      {breadcrumbs?.length > 0 && (
        <Breadcrumbs aria-label={COMMON.breadcrumbs}>
          {breadcrumbs.map((item, i) =>
            item.to && i < breadcrumbs.length - 1 ? (
              <Anchor key={item.label} component={Link} to={item.to} size="sm">
                {item.label}
              </Anchor>
            ) : (
              <Text key={item.label} size="sm" aria-current="page">
                {item.label}
              </Text>
            ),
          )}
        </Breadcrumbs>
      )}
      <Group justify="space-between" align="flex-start" wrap="wrap" gap="sm">
        <Stack gap={4}>
          <Title order={1} size="h2">
            {title}
          </Title>
          {description && (
            <Text c="dimmed" size="sm">
              {description}
            </Text>
          )}
        </Stack>
        {actions && <Group gap="sm">{actions}</Group>}
      </Group>
    </Stack>
  );
}
