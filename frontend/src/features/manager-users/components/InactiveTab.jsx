import { Anchor, Button, Group, Stack, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useState } from 'react';
import { Link } from 'react-router';
import { formatDate } from '../../../shared/format.js';
import { MANAGER_USERS as L, USER_STATUS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, PageLoader, StatusBadge } from '../../../shared/ui/index.js';
import { managerUserPath, useDeleteInactiveUsers, useInactiveUsers } from '../api.js';
import { SelectableTable } from './SelectableTable.jsx';

// M5: for each account the state and the date it is judged by (last login/registration, or the block date).
export function InactiveTab() {
  const inactive = useInactiveUsers();
  const remove = useDeleteInactiveUsers();
  const [selected, setSelected] = useState([]);
  const [confirmOpened, confirm] = useDisclosure(false);

  const rows = inactive.data ?? [];

  const run = () =>
    remove.mutate(selected, {
      onSuccess: () => notifySuccess(L.deleted(selected.length)),
      onError: notifyError,
      onSettled: () => {
        confirm.close();
        setSelected([]);
      },
    });

  if (inactive.isPending) return <PageLoader />;
  if (inactive.isError) return <ErrorState error={inactive.error} onRetry={inactive.refetch} />;

  return (
    <Stack gap="md">
      <Text size="sm" c="dimmed">{L.inactiveHint}</Text>
      {rows.length === 0 ? (
        <EmptyState title={L.inactiveEmpty} />
      ) : (
        <>
          <Group justify="flex-end" gap="sm" mih={36}>
            {selected.length > 0 && (
              <>
                <Text size="sm">
                  {L.selected} {selected.length}
                </Text>
                <Button color="red" variant="light" onClick={confirm.open}>
                  {L.deleteSelected}
                </Button>
              </>
            )}
          </Group>
          <SelectableTable
            rows={rows}
            selected={selected}
            onChange={setSelected}
            selectAllLabel={L.selectAllInactive}
            selectRowLabel={(r) => L.selectInactive(r.email)}
            minWidth={560}
            columns={[
              {
                key: 'email',
                header: L.email,
                miw: 200,
                render: (r) => (
                  <Anchor component={Link} to={managerUserPath(r.id)} style={{ overflowWrap: 'anywhere' }}>
                    {r.email}
                  </Anchor>
                ),
              },
              { key: 'status', header: L.status, render: (r) => <StatusBadge map={USER_STATUS} value={r.status} /> },
              {
                key: 'date',
                header: L.referenceDate,
                render: (r) => (
                  <>
                    {formatDate(r.referenceDate)}
                    <Text size="xs" c="dimmed">
                      {r.status === 'BLOCKED' ? L.referenceBlocked : L.referenceLogin}
                    </Text>
                  </>
                ),
              },
            ]}
          />
        </>
      )}
      <ConfirmModal
        opened={confirmOpened}
        onClose={confirm.close}
        onConfirm={run}
        title={L.deleteTitle}
        message={L.deleteMessage(selected.length)}
        confirmLabel={L.deleteSelected}
        danger
        loading={remove.isPending}
      />
    </Stack>
  );
}
