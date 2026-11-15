import { Anchor, Button, Grid, Group, Stack, Table, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';
import { formatCount, formatDateTime, formatNumber, POINT_FORMS } from '../../../shared/format.js';
import { MANAGER_USERS as L, ORDER_STATUS, TERMS, USER_STATUS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, InfoList, PageLoader, Section, StatusBadge } from '../../../shared/ui/index.js';
import { useManagerUser } from '../api.js';
import { BlockUserModal } from '../components/BlockUserModal.jsx';

// M11 account card: orders by status, bonus balance, block reason and date; M6 «Заблокувати».
export default function UserDetailsPage() {
  const raw = Number(useParams().id);
  const id = Number.isInteger(raw) && raw > 0 ? raw : null;
  const location = useLocation();
  const user = useManagerUser(id);
  const [blockOpened, setBlockOpened] = useState(false);

  useDocumentTitle(`${user.data?.email ?? L.title} — ${TERMS.appName}`);

  const from = location.state?.from;
  const backTo = typeof from === 'string' && from.startsWith('/manager/users') ? from : '/manager/users';
  const back = (
    <Anchor component={Link} to={backTo} size="sm">
      ← {L.backToList}
    </Anchor>
  );

  if (id == null || user.error?.status === 404) {
    return (
      <Stack gap="md">
        {back}
        <EmptyState title={L.notFoundTitle} hint={L.notFoundHint} />
      </Stack>
    );
  }
  if (user.isPending) return <PageLoader />;
  if (user.isError) return <ErrorState error={user.error} onRetry={user.refetch} />;

  const data = user.data;
  const blocked = data.status === 'BLOCKED';

  return (
    <Stack gap="md">
      {back}
      <Group justify="space-between" align="flex-start" wrap="wrap" gap="sm">
        <Group gap="sm" wrap="wrap">
          <Title order={2} size="h3" style={{ overflowWrap: 'anywhere' }}>
            {data.email}
          </Title>
          <StatusBadge map={USER_STATUS} value={data.status} />
        </Group>
        {!blocked && (
          <Button color="red" variant="light" onClick={() => setBlockOpened(true)}>
            {L.block}
          </Button>
        )}
      </Group>
      {blocked && <Text size="sm" c="dimmed">{L.unblockHint}</Text>}

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Section title={L.accountTitle}>
            <InfoList
              rows={[
                { label: L.registeredAt, value: formatDateTime(data.createdAt) },
                { label: L.lastLoginAt, value: data.lastLoginAt ? formatDateTime(data.lastLoginAt) : L.never },
                { label: L.bonusBalance, value: formatCount(data.bonusBalance, POINT_FORMS) },
                blocked && { label: L.blockedAt, value: formatDateTime(data.blockedAt) },
                blocked && { label: L.blockReason, value: data.blockReason },
              ]}
            />
          </Section>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Section title={L.ordersTitle}>
            <Table>
              <Table.Tbody>
                {Object.keys(ORDER_STATUS).map((s) => (
                  <Table.Tr key={s}>
                    <Table.Td>
                      <StatusBadge map={ORDER_STATUS} value={s} />
                    </Table.Td>
                    <Table.Td ta="right">{formatNumber(data.ordersByStatus?.[s] ?? 0)}</Table.Td>
                  </Table.Tr>
                ))}
                <Table.Tr>
                  <Table.Td fw={600}>{L.totalOrders}</Table.Td>
                  <Table.Td ta="right" fw={600}>{formatNumber(data.totalOrders)}</Table.Td>
                </Table.Tr>
              </Table.Tbody>
            </Table>
          </Section>
        </Grid.Col>
      </Grid>

      {blockOpened && <BlockUserModal opened onClose={() => setBlockOpened(false)} user={data} />}
    </Stack>
  );
}
