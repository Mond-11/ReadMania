import { Alert, PasswordInput, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { applyFormErrors, getErrorMessage } from '../../../shared/api/index.js';
import { AUTH, ERRORS } from '../../../shared/labels.js';
import { notifySuccess } from '../../../shared/notify.js';
import { FormActions } from '../../../shared/ui/index.js';
import { useRegister } from '../api.js';
import { validateConfirmation, validateEmail, validateNewPassword } from '../validation.js';

// G2: email, password and its confirmation. The new account is logged in right away.
export function RegisterForm() {
  const register = useRegister();
  const [problem, setProblem] = useState(null);

  const form = useForm({
    mode: 'controlled',
    initialValues: { email: '', password: '', confirmPassword: '' },
    validate: {
      email: validateEmail,
      password: validateNewPassword,
      confirmPassword: validateConfirmation('password'),
    },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) => {
    setProblem(null);
    register.mutate(values, {
      onSuccess: () => notifySuccess(AUTH.registered),
      onError: (error) => {
        if (error.status === 409) form.setFieldError('email', AUTH.emailTaken);
        else if (!applyFormErrors(form, error)) setProblem(getErrorMessage(error));
      },
    });
  });

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        {problem && (
          <Alert color="red" variant="light" title={ERRORS.title} role="alert">
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
          description={AUTH.passwordHint}
          autoComplete="new-password"
          required
          {...form.getInputProps('password')}
        />
        <PasswordInput
          label={AUTH.confirmPassword}
          autoComplete="new-password"
          required
          {...form.getInputProps('confirmPassword')}
        />
        <FormActions submitLabel={AUTH.registerSubmit} loading={register.isPending} />
      </Stack>
    </form>
  );
}
