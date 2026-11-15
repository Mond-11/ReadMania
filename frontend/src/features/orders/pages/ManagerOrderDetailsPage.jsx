import { Anchor, Button, Grid, Group, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, useLocation, useParams } from 'react-router';
import { formatDateTime } from '../../../shared/format.js';
import { labelOf, MANAGER_ORDERS, ORDER_STATUS, ORDER_TYPE, ORDERS, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageLoader, StatusBadge } from '../../../shared/ui/index.js';
import { useManagerOrder } from '../api.js';
import {
  OrderDeliverySection,
  OrderHistorySection,
  OrderItemsSection,
  OrderSummarySection,
} from '../components/OrderSections.jsx';
import { StatusChangeSection } from '../components/StatusChangeSection.jsx';

const LIST_OF_STATUS = { AWAITING_PAYMENT: '', PAID: '?list=paid' };

// M3 step 7.2.1.3: everything needed to change the status on one page (SRS 7.3), the status block next to it.
export default function ManagerOrderDetailsPage() {
  const raw = Number(useParams().id);
  const id = Number.isInteger(raw) && raw > 0 ? raw : null;
  const location = useLocation();
  const order = useManagerOrder(id);

  useDocumentTitle(`${ORDERS.orderTitle}${id ?? ''} — ${TERMS.appName}`);

  // Back to the list the order was opened from; otherwise to the list of its status.
  const backTo =
    typeof location.state?.from === 'string' && location.state.from.startsWith('/manager/orders')
      ? location.state.from
      : `/manager/orders${LIST_OF_STATUS[order.data?.status] ?? ''}`;

  if (id == null || order.error?.status === 404) {
    return (
      <EmptyState
        title={ORDERS.notFoundTitle}
        hint={ORDERS.notFoundHint}
        action={<Button component={Link} to="/manager/orders" variant="default">{MANAGER_ORDERS.backToList}</Button>}
      />
    );
  }
  if (order.isPending) return <PageLoader />;
  if (order.isError) return <ErrorState error={order.error} onRetry={order.refetch} />;

  const data = order.data;

  return (
    <Stack gap="md">
      <Anchor component={Link} to={backTo} size="sm">
        ← {MANAGER_ORDERS.backToList}
      </Anchor>
      <Stack gap={4}>
        <Group gap="sm" wrap="wrap">
          <Title order={2} size="h3">
            {ORDERS.orderTitle}{data.id}
          </Title>
          <StatusBadge map={ORDER_STATUS} value={data.status} />
        </Group>
        <Text size="sm" c="dimmed">
          {labelOf(ORDER_TYPE, data.type)} · {formatDateTime(data.createdAt)}
          {data.statusChangedAt && ` · ${ORDERS.statusChangedAt}: ${formatDateTime(data.statusChangedAt)}`}
        </Text>
      </Stack>

      <Grid gap="md" align="flex-start">
        {/* On narrow screens the status block comes first: it is the main action of M3. */}
        <Grid.Col span={{ base: 12, lg: 7 }} order={{ base: 2, lg: 1 }}>
          <Stack gap="md">
            <OrderItemsSection order={data} />
            <OrderSummarySection order={data} />
            <OrderDeliverySection order={data} />
          </Stack>
        </Grid.Col>
        <Grid.Col span={{ base: 12, lg: 5 }} order={{ base: 1, lg: 2 }}>
          <Stack gap="md">
            <StatusChangeSection order={data} onConflict={order.refetch} />
            <OrderHistorySection order={data} />
          </Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
