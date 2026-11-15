import { Alert, Anchor, Button, Grid, Group, Stack, Text, Title } from '@mantine/core';
import { useDisclosure, useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { formatDateTime } from '../../../shared/format.js';
import { ORDER_STATUS, ORDER_TYPE, ORDERS, TERMS, labelOf } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, PageLoader, StatusBadge } from '../../../shared/ui/index.js';
import {
  canUserCancel,
  OrderDeliverySection,
  OrderHistorySection,
  OrderItemsSection,
  OrderSummarySection,
  useCancelOrder,
  useMyOrder,
} from '../../orders/index.js';
import { TransferRequisites } from '../../cart/index.js';
import { RateBook } from '../components/RateBook.jsx';

const parseId = (param) => {
  const id = Number(param);
  return Number.isInteger(id) && id > 0 ? id : null;
};

// C4 order details; C3 cancel; C6 ratings in a delivered order.
export default function OrderDetailsPage() {
  const id = parseId(useParams().id);
  const order = useMyOrder(id);
  const cancel = useCancelOrder();
  const [confirmOpened, confirm] = useDisclosure(false);
  const [statusChanged, setStatusChanged] = useState(false);

  useDocumentTitle(`${ORDERS.orderTitle}${id ?? ''} — ${TERMS.appName}`);

  const backLink = (
    <Anchor component={Link} to="/account/orders" size="sm">
      ← {ORDERS.backToList}
    </Anchor>
  );

  if (id == null || order.error?.status === 404) {
    return (
      <EmptyState
        title={ORDERS.notFoundTitle}
        hint={ORDERS.notFoundHint}
        action={<Button component={Link} to="/account/orders" variant="default">{ORDERS.backToList}</Button>}
      />
    );
  }
  if (order.isPending) return <PageLoader />;
  if (order.isError) return <ErrorState error={order.error} onRetry={order.refetch} />;

  const data = order.data;
  const delivered = data.status === 'DELIVERED';

  const onCancel = () =>
    cancel.mutate(data.id, {
      onSuccess: () => {
        confirm.close();
        notifySuccess(ORDERS.cancelDone);
      },
      onError: (error) => {
        confirm.close();
        // C3 alternative flow: the Менеджер changed the status first — show the current state, the button disappears.
        if (error.status === 409 || error.status === 400) {
          setStatusChanged(true);
          order.refetch();
        } else {
          notifyError(error);
        }
      },
    });

  return (
    <Stack gap="md">
      {backLink}
      <Group justify="space-between" align="flex-start" wrap="wrap" gap="sm">
        <Stack gap={4}>
          <Group gap="sm" wrap="wrap">
            <Title order={2} size="h3">
              {ORDERS.orderTitle}{data.id}
            </Title>
            <StatusBadge map={ORDER_STATUS} value={data.status} />
          </Group>
          <Text size="sm" c="dimmed">
            {labelOf(ORDER_TYPE, data.type)} · {formatDateTime(data.createdAt)}
          </Text>
        </Stack>
        {canUserCancel(data) && (
          <Button color="red" variant="light" onClick={confirm.open}>
            {ORDERS.cancel}
          </Button>
        )}
      </Group>

      {statusChanged && (
        <Alert color="red" variant="light" role="alert" withCloseButton onClose={() => setStatusChanged(false)}>
          {ORDERS.statusChanged}
        </Alert>
      )}
      {data.status === 'AWAITING_PAYMENT' && !canUserCancel(data) && (
        <Text size="sm" c="dimmed">{ORDERS.cancelHint}</Text>
      )}
      {delivered && data.items.some((i) => !i.userRating) && (
        <Text size="sm" c="dimmed">{ORDERS.rateHint}</Text>
      )}

      <OrderItemsSection
        order={data}
        renderRating={delivered ? (item) => <RateBook item={item} orderId={data.id} canRate /> : undefined}
      />
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 6 }}>
          <OrderSummarySection order={data} />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 6 }}>
          <OrderDeliverySection order={data} />
        </Grid.Col>
      </Grid>
      {/* An unpaid transfer order: the user needs the bank details to pay it (C2), also after leaving the checkout. */}
      {data.status === 'AWAITING_PAYMENT' && data.paymentMethod === 'TRANSFER' && <TransferRequisites orderId={data.id} />}
      <OrderHistorySection order={data} />

      <ConfirmModal
        opened={confirmOpened}
        onClose={confirm.close}
        onConfirm={onCancel}
        title={ORDERS.cancelConfirmTitle}
        message={ORDERS.cancelConfirmMessage(data.id)}
        confirmLabel={ORDERS.cancel}
        danger
        loading={cancel.isPending}
      />
    </Stack>
  );
}
