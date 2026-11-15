import { Flex, Tabs, Text } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useDocumentTitle } from '@mantine/hooks';
import dayjs from 'dayjs';
import { useSearchParams } from 'react-router';
import { toApiDate } from '../../../shared/format.js';
import { MANAGER_ANALYTICS as L, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageLoader } from '../../../shared/ui/index.js';
import { useAnalytics } from '../api.js';
import AnalyticsChart from '../components/AnalyticsChart.jsx';
import { buildInsights } from '../insights.js';

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const validDate = (value) => (value && DATE_RE.test(value) ? value : null);

// Per tab: endpoint, main value field and its label, hint, empty state, optional extras.
const TABS = {
  demand: { label: L.tabDemand, hint: L.demandHint, valueKey: 'orderedQuantity', valueLabel: L.demandValue },
  stock: { label: L.tabStock, hint: L.stockHint, valueKey: 'stock', valueLabel: L.stockValue, noPeriod: true },
  ratings: {
    label: L.tabRatings,
    hint: L.ratingsHint,
    valueKey: 'averageRating',
    valueLabel: L.ratingValue,
    domain: [0, 5],
  },
};

function TabPanelBody({ kind, period, invalid }) {
  const tab = TABS[kind];
  const query = useAnalytics(kind, { ...period, enabled: !invalid });
  // The stock lists are all active genres and books: they reveal items the demand endpoint omits (never ordered).
  // A failed or slow catalog only hides that one card.
  const catalogQuery = useAnalytics('stock', { enabled: !invalid && kind === 'demand' });
  if (invalid) return null;
  if (query.isPending) return <PageLoader />;
  if (query.isError) return <ErrorState error={query.error} onRetry={query.refetch} />;
  const { byGenres = [], byBooks = [] } = query.data ?? {};
  if (!byGenres.length && !byBooks.length && !catalogQuery.data?.byBooks?.length) {
    return tab.noPeriod
      ? <EmptyState title={L.emptyStockTitle} hint={L.emptyStockHint} />
      : <EmptyState title={L.emptyTitle} hint={L.emptyHint} />;
  }
  const common = { valueKey: tab.valueKey, valueLabel: tab.valueLabel, domain: tab.domain };
  const groups = [
    { entity: 'genres', title: L.byGenres, rows: byGenres, catalog: catalogQuery.data?.byGenres },
    { entity: 'books', title: L.byBooks, rows: byBooks, catalog: catalogQuery.data?.byBooks },
  ];
  return groups.map((g) => (
    <AnalyticsChart
      key={g.entity}
      title={g.title}
      rows={g.rows}
      insights={buildInsights(kind, g.entity, g.rows, g.catalog)}
      {...common}
    />
  ));
}

export default function AnalyticsPage() {
  useDocumentTitle(`${L.tabDemand} — ${TERMS.appName}`);
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const kind = TABS[requested] ? requested : 'demand';

  // Default period is the last 3 months, same as the backend; sent explicitly so the pickers match the data.
  const today = toApiDate(new Date());
  const to = validDate(searchParams.get('to')) ?? today;
  const from = validDate(searchParams.get('from')) ?? toApiDate(dayjs(to).subtract(3, 'month').toDate());
  const toFuture = to > today;
  // The backend answers 400 for to < from or to > today; the pickers prevent both, a hand-edited URL is caught here.
  const rangeInvalid = to < from;
  const invalid = toFuture || rangeInvalid;

  const setParams = (patch) => {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(patch)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setSearchParams(next, { replace: true });
  };

  return (
    <Tabs value={kind} onChange={(value) => setParams({ tab: value === 'demand' ? null : value })} keepMounted={false}>
      <Tabs.List aria-label={L.tabs} mb="md">
        {Object.entries(TABS).map(([value, t]) => (
          <Tabs.Tab key={value} value={value}>{t.label}</Tabs.Tab>
        ))}
      </Tabs.List>
      <Text size="sm" mb="sm">{TABS[kind].hint}</Text>
      {!TABS[kind].noPeriod && (
        <Flex direction={{ base: 'column', sm: 'row' }} gap="sm" mb="md">
          <DatePickerInput
            label={L.dateFrom}
            placeholder={L.datePlaceholder}
            valueFormat="DD.MM.YYYY"
            value={from}
            maxDate={to < today ? to : today}
            onChange={(value) => setParams({ from: value })}
            flex={{ base: '0 0 auto', sm: '0 1 200px' }}
          />
          <DatePickerInput
            label={L.dateTo}
            placeholder={L.datePlaceholder}
            valueFormat="DD.MM.YYYY"
            value={to}
            minDate={from}
            maxDate={today}
            onChange={(value) => setParams({ to: value })}
            error={toFuture ? L.futureError : rangeInvalid ? L.rangeError : undefined}
            flex={{ base: '0 0 auto', sm: '0 1 200px' }}
          />
        </Flex>
      )}
      <Tabs.Panel value={kind}>
        <TabPanelBody
          key={kind}
          kind={kind}
          period={{ from, to }}
          invalid={!TABS[kind].noPeriod && invalid}
        />
      </Tabs.Panel>
    </Tabs>
  );
}
