import { Button, Group, Tooltip } from '@mantine/core';
import { COMMON } from '../labels.js';

export function FormActions({ submitLabel = COMMON.save, onCancel, loading = false, disabled = false, disabledReason }) {
  const submit = (
    <Button type="submit" loading={loading} disabled={disabled}>
      {submitLabel}
    </Button>
  );
  return (
    <Group justify="flex-end" gap="sm" mt="md">
      {onCancel && (
        <Button variant="default" onClick={onCancel} disabled={loading}>
          {COMMON.cancel}
        </Button>
      )}
      {disabled && disabledReason ? (
        // The span lets the tooltip receive hover/focus events that a disabled button swallows.
        <Tooltip label={disabledReason}>
          <span>{submit}</span>
        </Tooltip>
      ) : (
        submit
      )}
    </Group>
  );
}
