import { Alert, Button, Stack, Text } from '@mantine/core';
import { Link } from 'react-router';
import { formatDateTime } from '../../../shared/format.js';
import { AUTH } from '../../../shared/labels.js';
import { AUTH_PATHS } from '../paths.js';

// G3 / M6: the account is blocked — show the reason and offer the unblock request (M7).
export function BlockedNotice({ reason, blockedAt, email }) {
  return (
    <Alert color="red" variant="light" title={AUTH.blockedTitle} role="alert">
      <Stack gap="xs" align="flex-start">
        {reason && (
          <Text size="sm">
            <b>{AUTH.blockedReason}</b> {reason}
          </Text>
        )}
        {blockedAt && (
          <Text size="sm">
            <b>{AUTH.blockedAt}</b> {formatDateTime(blockedAt)}
          </Text>
        )}
        <Text size="sm">{AUTH.blockedHint}</Text>
        <Button component={Link} to={AUTH_PATHS.unblockRequest} state={{ email }} variant="default" size="sm">
          {AUTH.unblockButton}
        </Button>
      </Stack>
    </Alert>
  );
}
