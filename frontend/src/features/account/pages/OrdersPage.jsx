import { Anchor, Button, Group, Pagination, Rating, Stack, Table, Text } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, useSearchParams } from 'react-router';
import { formatDate, formatDateTime } from '../../../shared/format.js';
import {
  ACCOUNT,
  CATALOG,
  labelOf,
  ORDER_STATUS,
  ORDER_TYPE,
  ORDERS,
  PAGINATION_CONTROLS,
  TERMS,
} from '../../../shared/labels.js';
import { EmptyState, ErrorState, Money, PageLoader, Section, StatusBadge } from '../../../shared/ui/index.js';
import { myOrderPath, useMyOrders, useMyRatings } from '../../orders/index.js';

// C4: own orders (including gift ones) newest first, plus own ratings.
export default function OrdersPage() {
  useDocumentTitle(`${ACCOUNT.ordersTab} — ${TERMS.appName}`);
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(1, Number.parseInt(searchParams.get('page') ?? '1', 10) || 1);
  const orders = useMyOrders(page);

  const goTo = (p) => setSearchParams(p > 1 ? { page: String(p) } : {});
  const totalPages = orders.data?.page?.totalPages ?? 0;

  return (
    <Stack gap="md">
      <Section title={ORDERS.listTitle}>
        {orders.isPending ? (
          <PageLoader />
        ) : orders.isError ? (
          <ErrorState error={orders.error} onRetry={orders.refetch} />
        ) : orders.data.content.length === 0 ? (
          <EmptyState
            title={ORDERS.emptyTitle}
            hint={ORDERS.emptyHint}
            action={<Button component={Link} to="/" variant="default">{CATALOG.backToCatalog}</Button>}
          />
        ) : (
          <Stack gap="md" style={{ opacity: orders.isPlaceholderData ? 0.6 : 1 }}>
            <Table.ScrollContainer minWidth={560}>
              <Table>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{ORDERS.number}</Table.Th>
                    <Table.Th>{ORDERS.date}</Table.Th>
                    <Table.Th>{ORDERS.type}</Table.Th>
                    <Table.Th>{ORDERS.status}</Table.Th>
                    <Table.Th ta="right">{ORDERS.totalToPay}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {orders.data.content.map((o) => (
                    <Table.Tr key={o.id}>
                      <Table.Td>
                        <Anchor component={Link} to={myOrderPath(o.id)} fw={600} aria-label={`${ORDERS.openOrder}${o.id}`}>
                          {ORDERS.numberShort}{o.id}
                        </Anchor>
                      </Table.Td>
                      <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(o.createdAt)}</Table.Td>
                      <Table.Td>
                        {labelOf(ORDER_TYPE, o.type)}
                        {o.institutionName && (
                          <Text size="xs" c="dimmed">{o.institutionName}</Text>
                        )}
                      </Table.Td>
                      <Table.Td>
                        <StatusBadge map={ORDER_STATUS} value={o.status} />
                      </Table.Td>
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
                  onChange={goTo}
                  getControlProps={(control) => ({ 'aria-label': PAGINATION_CONTROLS[control] })}
                  aria-label={ORDERS.pagination}
                  component="nav"
                />
              </Group>
            )}
          </Stack>
        )}
      </Section>
      <MyRatingsSection />
    </Stack>
  );
}

function MyRatingsSection() {
  const ratings = useMyRatings();
  return (
    <Section title={ORDERS.myRatingsTitle}>
      {ratings.isPending ? (
        <PageLoader minHeight={120} />
      ) : ratings.isError ? (
        <ErrorState error={ratings.error} onRetry={ratings.refetch} />
      ) : ratings.data.length === 0 ? (
        <EmptyState title={ORDERS.myRatingsEmpty} hint={ORDERS.myRatingsHint} />
      ) : (
        <Table.ScrollContainer minWidth={420}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{ORDERS.book}</Table.Th>
                <Table.Th>{ORDERS.rating}</Table.Th>
                <Table.Th>{ORDERS.date}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {ratings.data.map((r) => (
                <Table.Tr key={r.id}>
                  <Table.Td>
                    <Anchor component={Link} to={`/books/${r.bookId}`}>{r.bookTitle}</Anchor>
                  </Table.Td>
                  <Table.Td>
                    <Group gap={6} wrap="nowrap">
                      <Rating value={r.value} readOnly size="sm" aria-hidden="true" />
                      <Text size="sm">{r.value} / 5</Text>
                    </Group>
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDate(r.createdAt)}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
    </Section>
  );
}
