import { Alert, Anchor, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { Link } from 'react-router';
import { applyFormErrors } from '../../../shared/api/index.js';
import { AUTH } from '../../../shared/labels.js';
import { notifyError } from '../../../shared/notify.js';
import { FormActions } from '../../../shared/ui/index.js';
import { useRequestPasswordReset } from '../api.js';
import { AuthCard } from '../components/AuthCard.jsx';
import { AUTH_PATHS } from '../paths.js';
import { validateEmail } from '../validation.js';

// G3 «Забули пароль?». The answer is the same whether the account exists or not, so registered addresses are not revealed.
export default function ForgotPasswordPage() {
  const request = useRequestPasswordReset();
  const form = useForm({
    mode: 'controlled',
    initialValues: { email: '' },
    validate: { email: validateEmail },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) =>
    request.mutate(values, {
      onError: (error) => {
        if (!applyFormErrors(form, error)) notifyError(error);
      },
    }),
  );

  return (
    <AuthCard title={AUTH.forgotTitle} description={AUTH.forgotDescription}>
      <Stack gap="md">
        {request.isSuccess ? (
          <Alert color="brand" variant="light" role="status">
            {AUTH.forgotSent}
          </Alert>
        ) : (
          <form onSubmit={submit} noValidate>
            <TextInput
              label={AUTH.email}
              placeholder={AUTH.emailPlaceholder}
              type="email"
              autoComplete="email"
              required
              {...form.getInputProps('email')}
            />
            <FormActions submitLabel={AUTH.forgotSubmit} loading={request.isPending} />
          </form>
        )}
        <Anchor component={Link} to={AUTH_PATHS.login} size="sm">
          {AUTH.backToLogin}
        </Anchor>
      </Stack>
    </AuthCard>
  );
}
