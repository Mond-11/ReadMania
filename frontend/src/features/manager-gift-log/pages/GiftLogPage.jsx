import { Button, Flex, Paper, Select, Table, Text } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useDocumentTitle } from '@mantine/hooks';
import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { formatCount, formatDate, formatNumber, toApiDate, BOOK_FORMS } from '../../../shared/format.js';
import { useInstitutions } from '../../../shared/hooks/useInstitutions.js';
import { COMMON, MANAGER_GIFT_LOG as L, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageLoader } from '../../../shared/ui/index.js';
import { managerOrderPath } from '../../orders/index.js';
import { useGiftLog } from '../api.js';

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const validDate = (value) => (value && DATE_RE.test(value) ? value : null);

export default function GiftLogPage() {
  useDocumentTitle(`${L.title} — ${TERMS.appName}`);
  const [searchParams, setSearchParams] = useSearchParams();
  const institutionId = searchParams.get('institutionId');
  const dateFrom = validDate(searchParams.get('dateFrom'));
  const dateTo = validDate(searchParams.get('dateTo'));
  // Books are delivered in the past, so a future date can only match nothing: the pickers cap at today.
  // Today is read once per mount (a lazy initializer): calling Date during every render is impure.
  const [today] = useState(() => toApiDate(new Date()));
  const fromFuture = Boolean(dateFrom && dateFrom > today);
  const toFuture = Boolean(dateTo && dateTo > today);
  // The backend answers 400 when dateTo < dateFrom; the pickers prevent it, a hand-edited URL is caught here.
  const rangeInvalid = Boolean(dateFrom && dateTo && dateTo < dateFrom);
  const invalid = rangeInvalid || fromFuture || toFuture;

  const institutions = useInstitutions();
  const institutionOptions = useMemo(
    () => (institutions.data ?? []).map((i) => ({ value: String(i.id), label: i.name })),
    [institutions.data],
  );

  const log = useGiftLog({
    institutionId: institutionId ?? undefined,
    dateFrom: dateFrom ?? undefined,
    dateTo: dateTo ?? undefined,
    enabled: !invalid,
  });
  const hasFilters = Boolean(institutionId || dateFrom || dateTo);

  const setFilter = (patch) => {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(patch)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setSearchParams(next, { replace: true });
  };
  const resetFilters = () => setSearchParams(new URLSearchParams(), { replace: true });

  const items = log.data?.items ?? [];
  let body;
  if (invalid) body = null;
  else if (log.isPending) body = <PageLoader />;
  else if (log.isError) body = <ErrorState error={log.error} onRetry={log.refetch} />;
  else if (!items.length) {
    body = hasFilters ? (
      <EmptyState
        title={L.emptyFilteredTitle}
        hint={L.emptyFilteredHint}
        action={<Button variant="default" onClick={resetFilters}>{L.resetFilters}</Button>}
      />
    ) : (
      <EmptyState title={L.emptyTitle} hint={L.emptyHint} />
    );
  } else {
    body = (
      <Table.ScrollContainer minWidth={320}>
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{L.deliveredAt}</Table.Th>
              <Table.Th>{L.institutionName}</Table.Th>
              <Table.Th>{L.bookTitle}</Table.Th>
              <Table.Th ta="right">{L.quantity}</Table.Th>
              <Table.Th>{L.order}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((row) => (
              <Table.Tr key={row.id}>
                <Table.Td>{formatDate(row.deliveredAt)}</Table.Td>
                <Table.Td>{row.institutionName}</Table.Td>
                <Table.Td>{row.bookTitle}</Table.Td>
                <Table.Td ta="right">{formatNumber(row.quantity)}</Table.Td>
                <Table.Td>
                  <Link to={managerOrderPath(row.orderId)}>№{row.orderId}</Link>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    );
  }

  return (
    <>
      <Flex direction={{ base: 'column', sm: 'row' }} gap="sm" mb="md" align={{ sm: 'flex-end' }}>
        <Select
          label={L.institution}
          placeholder={L.institutionPlaceholder}
          data={institutionOptions}
          value={institutionId}
          onChange={(value) => setFilter({ institutionId: value })}
          searchable
          clearable
          nothingFoundMessage={COMMON.emptyDash}
          flex={{ base: '0 0 auto', sm: '1 1 260px' }}
        />
        <DatePickerInput
          label={L.dateFrom}
          placeholder={L.datePlaceholder}
          valueFormat="DD.MM.YYYY"
          value={dateFrom}
          maxDate={dateTo && dateTo < today ? dateTo : today}
          onChange={(value) => setFilter({ dateFrom: value })}
          error={fromFuture ? L.futureError : undefined}
          clearable
          flex={{ base: '0 0 auto', sm: '1 1 160px' }}
        />
        <DatePickerInput
          label={L.dateTo}
          placeholder={L.datePlaceholder}
          valueFormat="DD.MM.YYYY"
          value={dateTo}
          minDate={dateFrom ?? undefined}
          maxDate={today}
          onChange={(value) => setFilter({ dateTo: value })}
          error={toFuture ? L.futureError : rangeInvalid ? L.rangeError : undefined}
          clearable
          flex={{ base: '0 0 auto', sm: '1 1 160px' }}
        />
        <Button variant="default" disabled={!hasFilters} onClick={resetFilters}>
          {COMMON.reset}
        </Button>
      </Flex>
      {!invalid && log.data && (
        <Paper withBorder p="md" mb="md" aria-live="polite">
          <Text size="sm">{L.totalDonated}</Text>
          <Text fw={700} fz="xl">
            {formatCount(log.data.totalDonatedBooks ?? 0, BOOK_FORMS)}
          </Text>
        </Paper>
      )}
      {body}
    </>
  );
}
