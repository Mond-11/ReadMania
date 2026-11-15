import { Alert, Button, Group, Radio, Stack, Text, Textarea } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useState } from 'react';
import { getErrorMessage } from '../../../shared/api/index.js';
import { labelOf, MANAGER_ORDERS, ORDER_STATUS } from '../../../shared/labels.js';
import { notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, Section, StatusBadge } from '../../../shared/ui/index.js';
import { allowedTransitions, useChangeOrderStatus } from '../api.js';

// Final statuses cannot be undone, so they need a confirmation; «Оплачено» is saved right away (step 7.2.1.4–5).
const NEEDS_CONFIRM = new Set(['DELIVERED', 'CANCELLED']);

// M3 steps 7.2.1.3–7.2.1.7: only the transitions allowed by Table 3 are offered.
// After saving, the block shows the transitions from the new status (e.g. «Оплачено» → «Доставлено»).
export function StatusChangeSection({ order, onConflict }) {
  const change = useChangeOrderStatus();
  const [status, setStatus] = useState(null);
  const [comment, setComment] = useState('');
  const [problem, setProblem] = useState(null);
  const [confirmOpened, confirm] = useDisclosure(false);
  const allowed = allowedTransitions(order);

  // A choice made for the old status is not valid after the status changed.
  const chosen = allowed.includes(status) ? status : null;

  const save = () => {
    setProblem(null);
    change.mutate(
      { id: order.id, status: chosen, expectedStatus: order.status, comment },
      {
        onSuccess: (updated) => {
          confirm.close();
          setStatus(null);
          setComment('');
          notifySuccess(MANAGER_ORDERS.statusSaved(labelOf(ORDER_STATUS, updated.status)));
        },
        onError: (error) => {
          confirm.close();
          setStatus(null);
          if (error.is('order-status-conflict')) {
            // 7.2.2.8: someone else changed the status — show the current one and start from step 3 again.
            setProblem({
              title: MANAGER_ORDERS.conflictTitle,
              text: MANAGER_ORDERS.conflict(labelOf(ORDER_STATUS, error.currentStatus)),
            });
          } else {
            // 7.2.2.9: the backend rolled everything back; the order keeps its previous status.
            setProblem({ title: MANAGER_ORDERS.saveFailedTitle, text: `${getErrorMessage(error)} ${MANAGER_ORDERS.saveFailed}` });
          }
          onConflict?.();
        },
      },
    );
  };

  const submit = (e) => {
    e.preventDefault();
    if (!chosen) return;
    if (NEEDS_CONFIRM.has(chosen)) confirm.open();
    else save();
  };

  return (
    <Section title={MANAGER_ORDERS.statusTitle}>
      <Group gap="xs">
        <Text size="sm" c="dimmed">{MANAGER_ORDERS.currentStatus}:</Text>
        <StatusBadge map={ORDER_STATUS} value={order.status} />
      </Group>

      {problem && (
        <Alert color="red" variant="light" title={problem.title} role="alert" withCloseButton onClose={() => setProblem(null)}>
          {problem.text}
        </Alert>
      )}

      {allowed.length === 0 ? (
        <Text size="sm">{MANAGER_ORDERS.finalStatus}</Text>
      ) : (
        <form onSubmit={submit} noValidate>
          <Stack gap="md">
            <Radio.Group label={MANAGER_ORDERS.nextStatus} value={chosen} onChange={setStatus} required>
              <Stack gap="sm" mt="xs">
                {allowed.map((s) => (
                  <Radio
                    key={s}
                    value={s}
                    label={labelOf(ORDER_STATUS, s)}
                    description={MANAGER_ORDERS.transitions[s]}
                  />
                ))}
              </Stack>
            </Radio.Group>
            <Textarea
              label={MANAGER_ORDERS.comment}
              description={MANAGER_ORDERS.commentHint}
              placeholder={MANAGER_ORDERS.commentPlaceholder}
              value={comment}
              onChange={(e) => setComment(e.currentTarget.value)}
              autosize
              minRows={2}
              maxLength={500}
            />
            <Text size="xs" c="dimmed">{MANAGER_ORDERS.customerHint}</Text>
            <Group justify="flex-end">
              <Button type="submit" disabled={!chosen} loading={change.isPending && !confirmOpened}>
                {MANAGER_ORDERS.saveStatus}
              </Button>
            </Group>
          </Stack>
        </form>
      )}

      <ConfirmModal
        opened={confirmOpened}
        onClose={confirm.close}
        onConfirm={save}
        title={chosen ? MANAGER_ORDERS.confirmTitle(labelOf(ORDER_STATUS, chosen)) : ''}
        message={chosen ? `${MANAGER_ORDERS.transitions[chosen]} ${MANAGER_ORDERS.confirmMessage}` : ''}
        confirmLabel={MANAGER_ORDERS.saveStatus}
        danger={chosen === 'CANCELLED'}
        loading={change.isPending}
      />
    </Section>
  );
}
