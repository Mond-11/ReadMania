import { Anchor, Button, Checkbox, Group, Pagination, Stack, Table, Tabs, Text } from '@mantine/core';
import { useDisclosure, useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router';
import { formatDateTime } from '../../../shared/format.js';
import {
  labelOf,
  MANAGER,
  MANAGER_ORDERS,
  ORDER_TYPE,
  ORDERS,
  PAGINATION_CONTROLS,
  PAYMENT_METHOD,
  TERMS,
} from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, Money, PageLoader, Section } from '../../../shared/ui/index.js';
import {
  MANAGER_ORDER_LISTS,
  managerOrderPath,
  useCancelOverdueOrders,
  useDeleteArchivedOrders,
  useManagerOrders,
} from '../api.js';

const LISTS = [
  { value: 'awaiting', label: MANAGER_ORDERS.awaiting, hint: MANAGER_ORDERS.awaitingHint },
  { value: 'paid', label: MANAGER_ORDERS.paid, hint: MANAGER_ORDERS.paidHint },
  { value: 'overdue', label: MANAGER_ORDERS.overdue, hint: MANAGER_ORDERS.overdueHint },
  { value: 'archive', label: MANAGER_ORDERS.archive, hint: MANAGER_ORDERS.archiveHint },
];

// M3, step 7.2.1.1: the lists «Очікує оплати» / «Оплачено», plus «Не оплачено понад 7 днів» (7.2.2.6) and «Архів» (7.2.2.7).
// The list and page live in the URL (?list=paid&page=2), so «back» from an order returns to the same place.
export default function ManagerOrdersPage() {
  useDocumentTitle(`${MANAGER.orders} — ${TERMS.appName}`);
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const list = MANAGER_ORDER_LISTS[searchParams.get('list')] ? searchParams.get('list') : 'awaiting';
  const page = Math.max(1, Number.parseInt(searchParams.get('page') ?? '1', 10) || 1);
  const current = LISTS.find((l) => l.value === list);
  const batch = MANAGER_ORDER_LISTS[list].batch;

  const orders = useManagerOrders(list, page);
  const [selected, setSelected] = useState(() => new Set());
  const [confirmOpened, confirm] = useDisclosure(false);
  const cancelBatch = useCancelOverdueOrders();
  const deleteBatch = useDeleteArchivedOrders();
  const action = batch === 'cancel' ? cancelBatch : deleteBatch;

  // Selection belongs to one page of one list.
  const go = (next) => {
    setSelected(new Set());
    const params = {};
    if (next.list !== 'awaiting') params.list = next.list;
    if (next.page > 1) params.page = String(next.page);
    setSearchParams(params);
  };

  const rows = orders.data?.content ?? [];
  const totalPages = orders.data?.page?.totalPages ?? 0;
  const pageIds = rows.map((o) => o.id);
  const allChecked = pageIds.length > 0 && pageIds.every((id) => selected.has(id));
  const someChecked = pageIds.some((id) => selected.has(id));

  const toggle = (id) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  const toggleAll = () => setSelected(allChecked ? new Set() : new Set(pageIds));

  const runBatch = () =>
    action.mutate([...selected], {
      onSuccess: (res) => {
        confirm.close();
        setSelected(new Set());
        notifySuccess(
          batch === 'cancel'
            ? MANAGER_ORDERS.cancelledCount(res?.cancelledCount ?? selected.size)
            : MANAGER_ORDERS.deletedCount(res?.deletedCount ?? selected.size),
        );
      },
      onError: (error) => {
        // 409: some order no longer qualifies (e.g. it was just paid); nothing was changed, the list is reloaded.
        confirm.close();
        setSelected(new Set());
        notifyError(error);
      },
    });

  return (
    <Stack gap="md">
      <Tabs value={list} onChange={(v) => v && go({ list: v, page: 1 })}>
        <Tabs.List aria-label={MANAGER_ORDERS.lists}>
          {LISTS.map((l) => (
            <Tabs.Tab key={l.value} value={l.value}>
              {l.label}
            </Tabs.Tab>
          ))}
        </Tabs.List>
      </Tabs>

      <Section
        title={current.label}
        actions={
          batch && selected.size > 0 ? (
            <Group gap="sm">
              <Text size="sm">
                {MANAGER_ORDERS.selected} {selected.size}
              </Text>
              <Button color="red" variant="light" onClick={confirm.open}>
                {batch === 'cancel' ? MANAGER_ORDERS.cancelSelected : MANAGER_ORDERS.deleteSelected}
              </Button>
            </Group>
          ) : null
        }
      >
        <Text size="sm" c="dimmed">{current.hint}</Text>

        {orders.isPending ? (
          <PageLoader />
        ) : orders.isError ? (
          <ErrorState error={orders.error} onRetry={orders.refetch} />
        ) : rows.length === 0 ? (
          <EmptyState title={MANAGER_ORDERS.emptyTitle} />
        ) : (
          <Stack gap="md" style={{ opacity: orders.isPlaceholderData ? 0.6 : 1 }}>
            <Table.ScrollContainer minWidth={batch ? 640 : 600}>
              <Table>
                <Table.Thead>
                  <Table.Tr>
                    {batch && (
                      <Table.Th w={40}>
                        <Checkbox
                          checked={allChecked}
                          indeterminate={someChecked && !allChecked}
                          onChange={toggleAll}
                          aria-label={MANAGER_ORDERS.selectAll}
                        />
                      </Table.Th>
                    )}
                    <Table.Th>{ORDERS.number}</Table.Th>
                    <Table.Th>{ORDERS.date}</Table.Th>
                    <Table.Th>{ORDERS.type}</Table.Th>
                    <Table.Th>{ORDERS.paymentMethod}</Table.Th>
                    <Table.Th ta="right">{ORDERS.totalToPay}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {rows.map((o) => (
                    <Table.Tr key={o.id} bg={selected.has(o.id) ? 'var(--mantine-color-brand-0)' : undefined}>
                      {batch && (
                        <Table.Td>
                          <Checkbox
                            checked={selected.has(o.id)}
                            onChange={() => toggle(o.id)}
                            aria-label={`${MANAGER_ORDERS.selectOrder}${o.id}`}
                          />
                        </Table.Td>
                      )}
                      <Table.Td>
                        <Anchor
                          component={Link}
                          to={managerOrderPath(o.id)}
                          state={{ from: location.pathname + location.search }}
                          fw={600}
                          aria-label={`${ORDERS.openOrder}${o.id}`}
                        >
                          {ORDERS.numberShort}{o.id}
                        </Anchor>
                      </Table.Td>
                      <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(o.createdAt)}</Table.Td>
                      <Table.Td>
                        {labelOf(ORDER_TYPE, o.type)}
                        {o.institutionName && <Text size="xs" c="dimmed">{o.institutionName}</Text>}
                      </Table.Td>
                      <Table.Td>{labelOf(PAYMENT_METHOD, o.paymentMethod)}</Table.Td>
                      <Table.Td ta="right"><Money value={o.totalToPay} /></Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            {totalPages > 1 && (
              <Group justify="center">
                <Pagination
                  total={totalPages}
                  value={Math.min(page, totalPages)}
                  onChange={(p) => go({ list, page: p })}
                  getControlProps={(control) => ({ 'aria-label': PAGINATION_CONTROLS[control] })}
                  aria-label={ORDERS.pagination}
                  component="nav"
                />
              </Group>
            )}
          </Stack>
        )}
      </Section>

      <ConfirmModal
        opened={confirmOpened}
        onClose={confirm.close}
        onConfirm={runBatch}
        title={batch === 'cancel' ? MANAGER_ORDERS.cancelConfirmTitle : MANAGER_ORDERS.deleteConfirmTitle}
        message={
          batch === 'cancel'
            ? MANAGER_ORDERS.cancelConfirmMessage(selected.size)
            : MANAGER_ORDERS.deleteConfirmMessage(selected.size)
        }
        confirmLabel={batch === 'cancel' ? MANAGER_ORDERS.cancelSelected : MANAGER_ORDERS.deleteSelected}
        danger
        loading={action.isPending}
      />
    </Stack>
  );
}
