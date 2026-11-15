import { Anchor, Table, Text } from '@mantine/core';
import { Link } from 'react-router';
import { formatDateTime, formatNumber } from '../../../shared/format.js';
import { DELIVERY_METHOD, labelOf, ORDER_STATUS, ORDERS, PAYMENT_METHOD } from '../../../shared/labels.js';
import { InfoList, Money, Section, StatusBadge } from '../../../shared/ui/index.js';

// Blocks of the order details page. Shared by Користувач (C4) and Менеджер (M3): all order data on one page.

// `renderRating(item)` adds the «Оцінка» column (C6); without it the column is not shown.
export function OrderItemsSection({ order, renderRating }) {
  return (
    <Section title={ORDERS.itemsTitle}>
      <Table.ScrollContainer minWidth={renderRating ? 640 : 480}>
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{ORDERS.book}</Table.Th>
              <Table.Th ta="right">{ORDERS.price}</Table.Th>
              <Table.Th ta="right">{ORDERS.quantity}</Table.Th>
              <Table.Th ta="right">{ORDERS.subtotal}</Table.Th>
              {renderRating && <Table.Th>{ORDERS.rating}</Table.Th>}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {order.items.map((item) => (
              <Table.Tr key={item.id}>
                <Table.Td>
                  {/* Title and price are snapshots: they stay even if the book was edited or removed. */}
                  <Anchor component={Link} to={`/books/${item.bookId}`}>
                    {item.titleSnapshot}
                  </Anchor>
                </Table.Td>
                <Table.Td ta="right"><Money value={item.priceSnapshot} /></Table.Td>
                <Table.Td ta="right">{formatNumber(item.quantity)}</Table.Td>
                <Table.Td ta="right"><Money value={item.subtotal} /></Table.Td>
                {renderRating && <Table.Td>{renderRating(item)}</Table.Td>}
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Section>
  );
}

// The SRS wants item cost, discount and the amount to pay as separate values.
export function OrderSummarySection({ order }) {
  return (
    <Section title={ORDERS.summaryTitle}>
      <InfoList
        rows={[
          { label: ORDERS.itemsTotal, value: <Money value={order.itemsTotal} /> },
          { label: ORDERS.discount, value: <Money value={order.discountAmount} /> },
          { label: ORDERS.pointsUsed, value: formatNumber(order.pointsUsed) },
          { label: ORDERS.totalToPay, value: <Money value={order.totalToPay} fw={700} /> },
        ]}
      />
    </Section>
  );
}

export function OrderDeliverySection({ order }) {
  const gift = order.type === 'GIFT';
  return (
    <Section title={ORDERS.deliveryTitle}>
      <InfoList
        rows={[
          { label: ORDERS.createdAt, value: formatDateTime(order.createdAt) },
          {
            label: ORDERS.deliveryMethod,
            value: gift ? ORDERS.giftDelivery : labelOf(DELIVERY_METHOD, order.deliveryMethod),
          },
          gift && { label: ORDERS.institution, value: order.institutionName },
          gift && { label: ORDERS.contactPerson, value: order.institutionContactPerson },
          { label: ORDERS.address, value: order.address },
          !gift && { label: ORDERS.recipient, value: order.recipientName },
          !gift && { label: ORDERS.phone, value: order.recipientPhone },
          { label: ORDERS.paymentMethod, value: labelOf(PAYMENT_METHOD, order.paymentMethod) },
          order.deliveredAt && { label: ORDERS.deliveredAt, value: formatDateTime(order.deliveredAt) },
        ]}
      />
    </Section>
  );
}

export function OrderHistorySection({ order }) {
  if (!order.statusHistory?.length) return null;
  return (
    <Section title={ORDERS.historyTitle}>
      <Table.ScrollContainer minWidth={480}>
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{ORDERS.date}</Table.Th>
              <Table.Th>{ORDERS.status}</Table.Th>
              <Table.Th>{ORDERS.historyComment}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {order.statusHistory.map((h) => (
              <Table.Tr key={h.id}>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(h.changedAt)}</Table.Td>
                <Table.Td>
                  <StatusBadge map={ORDER_STATUS} value={h.toStatus} />
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{h.comment || ''}</Text>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Section>
  );
}
