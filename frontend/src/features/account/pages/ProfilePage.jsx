import { Grid, PasswordInput, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useDocumentTitle } from '@mantine/hooks';
import { applyFormErrors } from '../../../shared/api/index.js';
import { formatDateTime } from '../../../shared/format.js';
import { ACCOUNT, AUTH, PROFILE, TERMS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { FormActions, InfoList, Section } from '../../../shared/ui/index.js';
import {
  useCurrentUser,
  validateConfirmation,
  validateEmail,
  validateNewPassword,
  validatePassword,
} from '../../auth/index.js';
import { useChangeEmail, useChangePassword } from '../api.js';

// The backend reports a wrong current password as a general 400 without a field; show it next to the field.
function onSaveError(form, error) {
  if (applyFormErrors(form, error)) return;
  if (error.status === 400 && /поточн/i.test(error.detail ?? '')) form.setFieldError('currentPassword', error.detail);
  else if (error.status === 409) form.setFieldError('email', AUTH.emailTaken);
  else notifyError(error);
}

// C5: change e-mail and/or password; any change is confirmed with the current password.
export default function ProfilePage() {
  useDocumentTitle(`${ACCOUNT.profileTab} — ${TERMS.appName}`);
  const { user } = useCurrentUser();

  return (
    <Grid gap="md" align="flex-start">
      <Grid.Col span={{ base: 12, md: 6 }}>
        <Stack gap="md">
          <Section title={PROFILE.dataTitle}>
            <InfoList
              rows={[
                { label: AUTH.email, value: user?.email },
                { label: PROFILE.registeredAt, value: formatDateTime(user?.createdAt) },
                { label: PROFILE.lastLoginAt, value: formatDateTime(user?.lastLoginAt) },
              ]}
            />
          </Section>
          <EmailForm currentEmail={user?.email ?? ''} />
        </Stack>
      </Grid.Col>
      <Grid.Col span={{ base: 12, md: 6 }}>
        <PasswordForm />
      </Grid.Col>
    </Grid>
  );
}

function EmailForm({ currentEmail }) {
  const save = useChangeEmail();
  const form = useForm({
    mode: 'controlled',
    initialValues: { email: '', currentPassword: '' },
    validate: {
      email: (v) =>
        validateEmail(v) ?? (v.trim().toLowerCase() === currentEmail.toLowerCase() ? PROFILE.emailSame : null),
      currentPassword: validatePassword,
    },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) =>
    save.mutate(values, {
      onSuccess: () => {
        form.reset();
        notifySuccess(PROFILE.emailSaved);
      },
      onError: (error) => onSaveError(form, error),
    }),
  );

  return (
    <Section title={PROFILE.emailTitle}>
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          <TextInput
            label={PROFILE.newEmail}
            placeholder={AUTH.emailPlaceholder}
            type="email"
            autoComplete="email"
            required
            {...form.getInputProps('email')}
          />
          <PasswordInput
            label={PROFILE.currentPassword}
            description={PROFILE.currentPasswordHint}
            autoComplete="current-password"
            required
            {...form.getInputProps('currentPassword')}
          />
        </Stack>
        <FormActions submitLabel={PROFILE.saveEmail} loading={save.isPending} />
      </form>
    </Section>
  );
}

function PasswordForm() {
  const save = useChangePassword();
  const form = useForm({
    mode: 'controlled',
    initialValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
    validate: {
      currentPassword: validatePassword,
      newPassword: validateNewPassword,
      confirmPassword: validateConfirmation('newPassword'),
    },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) =>
    save.mutate(values, {
      onSuccess: () => {
        form.reset();
        notifySuccess(PROFILE.passwordSaved);
      },
      onError: (error) => onSaveError(form, error),
    }),
  );

  return (
    <Section title={PROFILE.passwordTitle}>
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          <PasswordInput
            label={PROFILE.currentPassword}
            autoComplete="current-password"
            required
            {...form.getInputProps('currentPassword')}
          />
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
        </Stack>
        <FormActions submitLabel={PROFILE.savePassword} loading={save.isPending} />
      </form>
    </Section>
  );
}
