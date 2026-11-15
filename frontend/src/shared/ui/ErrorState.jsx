import { Alert, Button, Stack } from '@mantine/core';
import { getErrorMessage } from '../api/errorMessage.js';
import { COMMON, ERRORS } from '../labels.js';

export function ErrorState({ error, onRetry }) {
  return (
    <Alert color="red" variant="light" title={ERRORS.title} role="alert">
      <Stack gap="sm" align="flex-start">
        {getErrorMessage(error)}
        {onRetry && (
          <Button variant="default" size="xs" onClick={() => onRetry()}>
            {COMMON.retry}
          </Button>
        )}
      </Stack>
    </Alert>
  );
}
