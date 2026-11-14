import { Button, Group, Modal, Text } from '@mantine/core';
import { COMMON } from '../labels.js';

export function ConfirmModal({ opened, onClose, onConfirm, title, message, confirmLabel, danger = false, loading = false }) {
  return (
    <Modal opened={opened} onClose={onClose} title={title} closeOnClickOutside={!loading} closeOnEscape={!loading}>
      {message && <Text mb="lg">{message}</Text>}
      <Group justify="flex-end" gap="sm">
        <Button variant="default" onClick={onClose} disabled={loading}>
          {COMMON.cancel}
        </Button>
        <Button color={danger ? 'red' : undefined} onClick={onConfirm} loading={loading} data-autofocus>
          {confirmLabel}
        </Button>
      </Group>
    </Modal>
  );
}
