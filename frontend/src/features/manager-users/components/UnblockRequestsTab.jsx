import { Anchor, Button, Group, Stack, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useState } from 'react';
import { Link } from 'react-router';
import { formatDateTime } from '../../../shared/format.js';
import { MANAGER_USERS as L } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, PageLoader } from '../../../shared/ui/index.js';
import { managerUserPath, useUnblockRequests, useUnblockUsers } from '../api.js';
import { SelectableTable } from './SelectableTable.jsx';

// M7: the Менеджер marks requests to grant and confirms; unmarked requests stay in the list.
export function UnblockRequestsTab() {
  const requests = useUnblockRequests();
  const unblock = useUnblockUsers();
  const [selected, setSelected] = useState([]);
  const [confirmOpened, confirm] = useDisclosure(false);

  const rows = (requests.data ?? []).map((r) => ({ ...r, id: r.userId }));

  const run = () =>
    unblock.mutate(selected, {
      onSuccess: () => notifySuccess(L.unblocked),
      onError: notifyError,
      onSettled: () => {
        confirm.close();
        setSelected([]);
      },
    });

  if (requests.isPending) return <PageLoader />;
  if (requests.isError) return <ErrorState error={requests.error} onRetry={requests.refetch} />;

  return (
    <Stack gap="md">
      <Text size="sm" c="dimmed">{L.requestsHint}</Text>
      {rows.length === 0 ? (
        <EmptyState title={L.requestsEmpty} />
      ) : (
        <>
          <Group justify="flex-end" gap="sm" mih={36}>
            {selected.length > 0 && (
              <>
                <Text size="sm">
                  {L.selected} {selected.length}
                </Text>
                <Button onClick={confirm.open}>{L.unblockSelected}</Button>
              </>
            )}
          </Group>
          <SelectableTable
            rows={rows}
            selected={selected}
            onChange={setSelected}
            selectAllLabel={L.selectAllRequests}
            selectRowLabel={(r) => L.selectRequest(r.email)}
            minWidth={900}
            columns={[
              {
                key: 'email',
                header: L.email,
                miw: 200,
                render: (r) => (
                  <Anchor component={Link} to={managerUserPath(r.userId)} style={{ overflowWrap: 'anywhere' }}>
                    {r.email}
                  </Anchor>
                ),
              },
              { key: 'blockedAt', header: L.blockedAt, style: { whiteSpace: 'nowrap' }, render: (r) => formatDateTime(r.blockedAt) },
              { key: 'reason', header: L.blockReason, render: (r) => r.blockReason },
              {
                key: 'justification',
                header: L.justification,
                render: (r) => <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>{r.justification}</Text>,
              },
              { key: 'createdAt', header: L.requestedAt, style: { whiteSpace: 'nowrap' }, render: (r) => formatDateTime(r.createdAt) },
            ]}
          />
        </>
      )}
      <ConfirmModal
        opened={confirmOpened}
        onClose={confirm.close}
        onConfirm={run}
        title={L.unblockTitle}
        message={L.unblockMessage(selected.length)}
        confirmLabel={L.unblockSelected}
        loading={unblock.isPending}
      />
    </Stack>
  );
}
