import { Paper, Stack } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { CATALOG, TERMS } from '../../../shared/labels.js';
import { PageHeader } from '../../../shared/ui/index.js';

// Narrow centered layout shared by all auth pages.
export function AuthCard({ title, description, crumb, children }) {
  useDocumentTitle(`${crumb ?? title} — ${TERMS.appName}`);
  return (
    <Stack maw={480} mx="auto" w="100%" gap={0}>
      <PageHeader
        title={title}
        description={description}
        breadcrumbs={[{ label: CATALOG.title, to: '/' }, { label: crumb ?? title }]}
      />
      <Paper p={{ base: 'md', sm: 'lg' }}>{children}</Paper>
    </Stack>
  );
}
