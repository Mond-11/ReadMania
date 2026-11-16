import { Button, Flex, Group, Tabs, Text } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useDocumentTitle } from '@mantine/hooks';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { formatDate, toApiDate } from '../../../shared/format.js';
import { MANAGER_ANALYTICS as L, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageLoader } from '../../../shared/ui/index.js';
import { useAnalytics } from '../api.js';
import AnalyticsChart from '../components/AnalyticsChart.jsx';
import AnalyticsSummary from '../components/AnalyticsSummary.jsx';
import AnalyticsTips from '../components/AnalyticsTips.jsx';
import { buildInsights } from '../insights.js';
import { buildSummary, buildTips, previousPeriod } from '../summary.js';

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const validDate = (value) => (value && DATE_RE.test(value) ? value : null);

// Per tab: endpoint, main value field and its label, hint, empty state, optional extras.
const TABS = {
  demand: { label: L.tabDemand, hint: L.demandHint, valueKey: 'orderedQuantity', valueLabel: L.demandValue, share: true },
  stock: { label: L.tabStock, hint: L.stockHint, valueKey: 'stock', valueLabel: L.stockValue, noPeriod: true, share: true },
  ratings: {
    label: L.tabRatings,
    hint: L.ratingsHint,
    valueKey: 'averageRating',
    valueLabel: L.ratingValue,
    domain: [0, 5],
  },
};

// Quick periods ending today; «3 місяці» is the default period.
const PRESETS = [
  { key: '30d', label: L.preset30, from: (to) => dayjs(to).subtract(30, 'day') },
  { key: '3m', label: L.preset90, from: (to) => dayjs(to).subtract(3, 'month') },
  { key: '1y', label: L.preset365, from: (to) => dayjs(to).subtract(1, 'year') },
];

// Summary and «Що варто зробити» above the charts. Tips always use the period's demand and ratings with the
// current stock, so they are the same on every tab; the summary cards depend on the tab.
// The queries share cache keys with the tab bodies, so nothing is fetched twice.
function Overview({ kind, period, invalid }) {
  const enabled = !invalid;
  const demand = useAnalytics('demand', { ...period, enabled });
  const prevDemand = useAnalytics('demand', { ...previousPeriod(period), enabled: enabled && kind === 'demand' });
  const stock = useAnalytics('stock', { enabled });
  const ratings = useAnalytics('ratings', { ...period, enabled });
  if (invalid) return null;
  const data = { demand: demand.data, prevDemand: prevDemand.data, stock: stock.data, ratings: ratings.data };
  const { tips, hidden } = buildTips(data);
  const caption = TABS[kind].noPeriod ? L.summaryNow : L.summaryPeriod(formatDate(period.from), formatDate(period.to));
  return (
    <>
      <AnalyticsSummary cards={buildSummary(kind, data)} caption={caption} />
      {demand.data && stock.data && <AnalyticsTips tips={tips} hidden={hidden} />}
    </>
  );
}

function TabPanelBody({ kind, period, invalid }) {
  const tab = TABS[kind];
  const query = useAnalytics(kind, { ...period, enabled: !invalid });
  // The previous period of the same length gives the «Зміна» column of the demand tables.
  const prevQuery = useAnalytics('demand', { ...previousPeriod(period), enabled: !invalid && kind === 'demand' });
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
  const common = { valueKey: tab.valueKey, valueLabel: tab.valueLabel, domain: tab.domain, share: tab.share };
  const prev = kind === 'demand' ? prevQuery.data : undefined;
  const groups = [
    { entity: 'genres', title: L.byGenres, rows: byGenres, catalog: catalogQuery.data?.byGenres, prevRows: prev?.byGenres },
    { entity: 'books', title: L.byBooks, rows: byBooks, catalog: catalogQuery.data?.byBooks, prevRows: prev?.byBooks },
  ];
  return groups.map((g) => (
    <AnalyticsChart
      key={g.entity}
      title={g.title}
      rows={g.rows}
      insights={buildInsights(kind, g.entity, g.rows, g.catalog)}
      prevRows={g.prevRows}
      {...common}
    />
  ));
}

export default function AnalyticsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const kind = TABS[requested] ? requested : 'demand';
  // The browser tab names the open analytics tab.
  useDocumentTitle(`${TABS[kind].label} — ${TERMS.appName}`);

  // Default period is the last 3 months, same as the backend; sent explicitly so the pickers match the data.
  // Today is read once per mount (a lazy initializer): calling Date during every render is impure.
  const [today] = useState(() => toApiDate(new Date()));
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
          <Group gap="xs" role="group" aria-label={L.presets} mt={{ sm: 'auto' }} mb={{ sm: invalid ? 22 : 0 }}>
            {PRESETS.map((p) => {
              const presetFrom = toApiDate(p.from(today).toDate());
              const active = to === today && from === presetFrom;
              return (
                <Button
                  key={p.key}
                  variant={active ? 'light' : 'default'}
                  aria-pressed={active}
                  onClick={() => setParams({ from: presetFrom, to: today })}
                >
                  {p.label}
                </Button>
              );
            })}
          </Group>
        </Flex>
      )}
      <Tabs.Panel value={kind}>
        <Overview kind={kind} period={{ from, to }} invalid={!TABS[kind].noPeriod && invalid} />
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
