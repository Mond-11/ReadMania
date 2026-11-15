import { Button, Group, Modal, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { applyFormErrors } from '../../../shared/api/index.js';
import { COMMON, MANAGER_USERS as L } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { useBlockUser } from '../api.js';

const MAX_REASON = 500;

// M6: the Менеджер enters the reason and confirms. The modal itself is the confirmation step.
export function BlockUserModal({ opened, onClose, user }) {
  const block = useBlockUser();
  const form = useForm({
    mode: 'controlled',
    initialValues: { reason: '' },
    validate: {
      reason: (v) => {
        const t = v.trim();
        if (!t) return L.reasonRequired;
        return t.length > MAX_REASON ? L.reasonTooLong : null;
      },
    },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit(({ reason }) =>
    block.mutate(
      { id: user.id, reason },
      {
        onSuccess: () => {
          notifySuccess(L.blocked);
          onClose();
        },
        onError: (error) => {
          if (!applyFormErrors(form, error)) notifyError(error);
        },
      },
    ),
  );

  const length = form.getValues().reason.trim().length;

  return (
    <Modal opened={opened} onClose={onClose} title={L.blockTitle} closeOnClickOutside={!block.isPending}>
      <form onSubmit={submit} noValidate>
        <Stack gap="sm">
          <Text size="sm" fw={600} style={{ overflowWrap: 'anywhere' }}>
            {user.email}
          </Text>
          <Text size="sm" c="dimmed">{L.blockHint}</Text>
          <Textarea
            label={L.reason}
            placeholder={L.reasonPlaceholder}
            description={`${length} / ${MAX_REASON}`}
            autosize
            minRows={3}
            required
            data-autofocus
            {...form.getInputProps('reason')}
          />
        </Stack>
        <Group justify="flex-end" gap="sm" mt="md">
          <Button variant="default" onClick={onClose} disabled={block.isPending}>
            {COMMON.cancel}
          </Button>
          <Button type="submit" color="red" loading={block.isPending}>
            {L.block}
          </Button>
        </Group>
      </form>
    </Modal>
  );
}
