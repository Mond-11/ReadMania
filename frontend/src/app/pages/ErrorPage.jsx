import { Button, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { isRouteErrorResponse, useRouteError } from 'react-router';
import { getErrorMessage } from '../../shared/api/errorMessage.js';
import { ApiError } from '../../shared/api/index.js';
import { PAGES, TERMS } from '../../shared/labels.js';
import { NotFoundPage } from './NotFoundPage.jsx';

export function ErrorPage({ standalone = false }) {
  const error = useRouteError();
  useDocumentTitle(`${PAGES.errorTitle} — ${TERMS.appName}`);

  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />;

  const message = error instanceof ApiError ? getErrorMessage(error) : PAGES.errorText;
  if (!(error instanceof ApiError)) console.error(error);

  return (
    <Stack align="center" gap="sm" py={64} px="md" ta="center" role="alert">
      <Text fz={64} fw={800} c="dimmed" lh={1} aria-hidden>500</Text>
      <Title order={1}>{PAGES.errorTitle}</Title>
      <Text c="dimmed">{message}</Text>
      <Button mt="sm" onClick={() => (standalone ? window.location.assign('/') : window.location.reload())}>
        {standalone ? PAGES.goHome : 'Оновити сторінку'}
      </Button>
    </Stack>
  );
}
