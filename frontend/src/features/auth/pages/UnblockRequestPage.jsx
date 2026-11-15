import { Alert, Anchor, PasswordInput, Stack, Textarea, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { Link, useLocation } from 'react-router';
import { applyFormErrors, getErrorMessage } from '../../../shared/api/index.js';
import { AUTH } from '../../../shared/labels.js';
import { FormActions } from '../../../shared/ui/index.js';
import { useUnblockRequest } from '../api.js';
import { AuthCard } from '../components/AuthCard.jsx';
import { AUTH_PATHS } from '../paths.js';
import { MAX_REASON_LENGTH, validateEmail, validatePassword, validateReason } from '../validation.js';

// M7, the blocked user's side. The password proves the account belongs to the sender.
// The justification is required here because the backend rejects an empty one (SRS M7 calls it optional; agreed with the team).
export default function UnblockRequestPage() {
  const location = useLocation();
  const request = useUnblockRequest();
  const [problem, setProblem] = useState(null);

  const form = useForm({
    mode: 'controlled',
    initialValues: { email: location.state?.email ?? '', password: '', reason: '' },
    validate: { email: validateEmail, password: validatePassword, reason: validateReason },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) => {
    setProblem(null);
    request.mutate(values, {
      onError: (error) => {
        if (!applyFormErrors(form, error)) setProblem(getErrorMessage(error));
      },
    });
  });

  const reasonLength = form.getValues().reason.trim().length;

  return (
    <AuthCard title={AUTH.unblockTitle} description={request.isSuccess ? undefined : AUTH.unblockDescription}>
      <Stack gap="md">
        {request.isSuccess ? (
          <Alert color="brand" variant="light" title={AUTH.unblockSentTitle} role="status">
            {AUTH.unblockSent}
          </Alert>
        ) : (
          <form onSubmit={submit} noValidate>
            <Stack gap="md">
              {problem && (
                <Alert color="red" variant="light" title={AUTH.unblockFailedTitle} role="alert">
                  {problem}
                </Alert>
              )}
              <TextInput
                label={AUTH.email}
                placeholder={AUTH.emailPlaceholder}
                type="email"
                autoComplete="email"
                required
                {...form.getInputProps('email')}
              />
              <PasswordInput
                label={AUTH.password}
                autoComplete="current-password"
                required
                {...form.getInputProps('password')}
              />
              <Textarea
                label={AUTH.reason}
                placeholder={AUTH.reasonPlaceholder}
                description={`${reasonLength} / ${MAX_REASON_LENGTH}`}
                autosize
                minRows={3}
                maxRows={8}
                required
                {...form.getInputProps('reason')}
              />
              <FormActions submitLabel={AUTH.unblockSubmit} loading={request.isPending} />
            </Stack>
          </form>
        )}
        <Anchor component={Link} to={AUTH_PATHS.login} size="sm">
          {AUTH.backToLogin}
        </Anchor>
      </Stack>
    </AuthCard>
  );
}
