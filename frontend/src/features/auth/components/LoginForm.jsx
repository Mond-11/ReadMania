import { Alert, Anchor, Group, PasswordInput, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { Link } from 'react-router';
import { applyFormErrors, getErrorMessage } from '../../../shared/api/index.js';
import { formatDateTime } from '../../../shared/format.js';
import { AUTH, ERRORS } from '../../../shared/labels.js';
import { notifySuccess } from '../../../shared/notify.js';
import { FormActions } from '../../../shared/ui/index.js';
import { useLogin } from '../api.js';
import { AUTH_PATHS } from '../paths.js';
import { validateEmail, validatePassword } from '../validation.js';
import { BlockedNotice } from './BlockedNotice.jsx';

// G3. On success the user is cached; ProtectedRoute (guestOnly) then sends them back to `from`.
// `blocked` comes from navigation state when the account was blocked during an open session.
export function LoginForm({ blocked }) {
  const login = useLogin();
  const [problem, setProblem] = useState(blocked ? { kind: 'blocked', ...blocked } : null);

  const form = useForm({
    mode: 'controlled',
    initialValues: { email: blocked?.email ?? '', password: '' },
    validate: { email: validateEmail, password: validatePassword },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) => {
    setProblem(null);
    login.mutate(values, {
      onSuccess: () => notifySuccess(AUTH.welcome),
      onError: (error) => {
        if (error.is('account-blocked')) {
          setProblem({ kind: 'blocked', reason: error.blockReason ?? error.detail, blockedAt: error.blockedAt });
        } else if (error.is('login-rate-limited')) {
          setProblem({ kind: 'rateLimited', retryAfter: error.retryAfter });
        } else if (error.status === 401) {
          setProblem({ kind: 'invalid' });
        } else if (!applyFormErrors(form, error)) {
          setProblem({ kind: 'other', message: getErrorMessage(error) });
        }
      },
    });
  });

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        {problem?.kind === 'blocked' && (
          <BlockedNotice reason={problem.reason} blockedAt={problem.blockedAt} email={form.getValues().email} />
        )}
        {problem?.kind === 'rateLimited' && (
          <Alert color="red" variant="light" title={AUTH.rateLimitedTitle} role="alert">
            <Text size="sm">{AUTH.rateLimited}</Text>
            {problem.retryAfter && (
              <Text size="sm" mt={4}>
                {AUTH.retryAt} <b>{formatDateTime(problem.retryAfter)}</b>.
              </Text>
            )}
          </Alert>
        )}
        {problem?.kind === 'invalid' && (
          <Alert color="red" variant="light" title={AUTH.invalidCredentialsTitle} role="alert">
            {AUTH.invalidCredentials}
          </Alert>
        )}
        {problem?.kind === 'other' && (
          <Alert color="red" variant="light" title={ERRORS.title} role="alert">
            {problem.message}
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
        <Group justify="flex-end">
          <Anchor component={Link} to={AUTH_PATHS.forgotPassword} size="sm">
            {AUTH.forgotLink}
          </Anchor>
        </Group>
        <FormActions submitLabel={AUTH.loginSubmit} loading={login.isPending} />
      </Stack>
    </form>
  );
}
