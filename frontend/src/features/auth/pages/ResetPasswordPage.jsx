import { Alert, Anchor, Button, PasswordInput, Stack } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { applyFormErrors, getErrorMessage } from '../../../shared/api/index.js';
import { AUTH } from '../../../shared/labels.js';
import { notifySuccess } from '../../../shared/notify.js';
import { EmptyState, FormActions } from '../../../shared/ui/index.js';
import { useConfirmPasswordReset } from '../api.js';
import { AuthCard } from '../components/AuthCard.jsx';
import { AUTH_PATHS } from '../paths.js';
import { validateConfirmation, validateNewPassword } from '../validation.js';

// G3: the link from the e-mail is /reset-password?token=…; it works for 1 hour and only once.
export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const confirm = useConfirmPasswordReset();
  const [problem, setProblem] = useState(null);

  const form = useForm({
    mode: 'controlled',
    initialValues: { newPassword: '', confirmPassword: '' },
    validate: { newPassword: validateNewPassword, confirmPassword: validateConfirmation('newPassword') },
    validateInputOnBlur: true,
  });

  const requestNewLink = (
    <Button component={Link} to={AUTH_PATHS.forgotPassword} variant="default">
      {AUTH.requestNewLink}
    </Button>
  );

  if (!token) {
    return (
      <AuthCard title={AUTH.resetTitle}>
        <EmptyState title={AUTH.resetNoToken} hint={AUTH.resetNoTokenHint} action={requestNewLink} />
      </AuthCard>
    );
  }

  const submit = form.onSubmit((values) => {
    setProblem(null);
    confirm.mutate(
      { token, ...values },
      {
        onSuccess: () => {
          notifySuccess(AUTH.resetDone);
          navigate(AUTH_PATHS.login, { replace: true });
        },
        onError: (error) => {
          if (!applyFormErrors(form, error)) setProblem(getErrorMessage(error));
        },
      },
    );
  });

  return (
    <AuthCard title={AUTH.resetTitle} description={AUTH.resetDescription}>
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          {problem && (
            <Alert color="red" variant="light" title={AUTH.resetFailedTitle} role="alert">
              <Stack gap="xs" align="flex-start">
                {problem}
                <Anchor component={Link} to={AUTH_PATHS.forgotPassword} size="sm">
                  {AUTH.requestNewLink}
                </Anchor>
              </Stack>
            </Alert>
          )}
          <PasswordInput
            label={AUTH.newPassword}
            description={AUTH.passwordHint}
            autoComplete="new-password"
            required
            {...form.getInputProps('newPassword')}
          />
          <PasswordInput
            label={AUTH.confirmPassword}
            autoComplete="new-password"
            required
            {...form.getInputProps('confirmPassword')}
          />
          <FormActions submitLabel={AUTH.resetSubmit} loading={confirm.isPending} />
        </Stack>
      </form>
    </AuthCard>
  );
}
