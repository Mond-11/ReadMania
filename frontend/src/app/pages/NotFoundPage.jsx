import { Button, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link } from 'react-router';
import { PAGES, TERMS } from '../../shared/labels.js';

export function NotFoundPage() {
  useDocumentTitle(`${PAGES.notFoundTitle} — ${TERMS.appName}`);
  return (
    <Stack align="center" gap="sm" py={64} ta="center">
      <Text fz={64} fw={800} c="dimmed" lh={1} aria-hidden>404</Text>
      <Title order={1}>{PAGES.notFoundTitle}</Title>
      <Text c="dimmed">{PAGES.notFoundText}</Text>
      <Button component={Link} to="/" mt="sm">
        {PAGES.goHome}
      </Button>
    </Stack>
  );
}
