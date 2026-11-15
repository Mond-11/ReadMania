import { Tabs } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useSearchParams } from 'react-router';
import { MANAGER_USERS as L, TERMS } from '../../../shared/labels.js';
import { Section } from '../../../shared/ui/index.js';
import { InactiveTab } from '../components/InactiveTab.jsx';
import { SearchTab } from '../components/SearchTab.jsx';
import { UnblockRequestsTab } from '../components/UnblockRequestsTab.jsx';

const TABS = [
  { value: 'search', label: L.searchTab, Panel: SearchTab },
  { value: 'requests', label: L.requestsTab, Panel: UnblockRequestsTab },
  { value: 'inactive', label: L.inactiveTab, Panel: InactiveTab },
];

// «Облікові записи»: search (M11), unblock requests (M7), inactive accounts (M5). The tab is part of the URL.
export default function UsersPage() {
  useDocumentTitle(`${L.title} — ${TERMS.appName}`);
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = TABS.find((t) => t.value === searchParams.get('tab')) ?? TABS[0];

  return (
    <Section title={L.title}>
      <Tabs value={tab.value} onChange={(v) => setSearchParams(v && v !== 'search' ? { tab: v } : {})} keepMounted={false}>
        <Tabs.List aria-label={L.tabs} mb="md">
          {TABS.map((t) => (
            <Tabs.Tab key={t.value} value={t.value}>
              {t.label}
            </Tabs.Tab>
          ))}
        </Tabs.List>
        {TABS.map(({ value, Panel }) => (
          <Tabs.Panel key={value} value={value}>
            <Panel />
          </Tabs.Panel>
        ))}
      </Tabs>
    </Section>
  );
}
