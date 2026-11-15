import { Tabs } from '@mantine/core';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { ACCOUNT, CATALOG } from '../../../shared/labels.js';
import { PageHeader } from '../../../shared/ui/index.js';

// «Особистий кабінет» (UX lab 3): three tabs, each with its own URL so it can be bookmarked (D1).
const ACCOUNT_TABS = [
  { value: 'orders', path: '/account/orders', label: ACCOUNT.ordersTab },
  { value: 'profile', path: '/account/profile', label: ACCOUNT.profileTab },
  { value: 'challenges', path: '/account/challenges', label: ACCOUNT.challengesTab },
];

export default function AccountLayout() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const active = ACCOUNT_TABS.find((t) => pathname === t.path || pathname.startsWith(`${t.path}/`)) ?? ACCOUNT_TABS[0];

  return (
    <>
      <PageHeader
        title={ACCOUNT.title}
        breadcrumbs={[
          { label: CATALOG.title, to: '/' },
          { label: ACCOUNT.title, to: ACCOUNT_TABS[0].path },
          { label: active.label },
        ]}
      />
      <Tabs value={active.value} onChange={(v) => navigate(ACCOUNT_TABS.find((t) => t.value === v).path)} mb="md">
        <Tabs.List aria-label={ACCOUNT.tabs}>
          {ACCOUNT_TABS.map((t) => (
            <Tabs.Tab key={t.value} value={t.value}>
              {t.label}
            </Tabs.Tab>
          ))}
        </Tabs.List>
      </Tabs>
      <Outlet />
    </>
  );
}
