import { Anchor, Group, Pagination, Select, SimpleGrid, Stack, Table, Text, TextInput } from '@mantine/core';
import { useDebouncedCallback } from '@mantine/hooks';
import { useState } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router';
import { formatDateTime } from '../../../shared/format.js';
import { MANAGER_USERS as L, PAGINATION_CONTROLS, toOptions, USER_STATUS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, PageLoader, StatusBadge } from '../../../shared/ui/index.js';
import { DEFAULT_USER_SORT, managerUserPath, useManagerUsers } from '../api.js';

const STATUS_OPTIONS = [{ value: '', label: L.allStatuses }, ...toOptions(USER_STATUS)];
const SORT_OPTIONS = toOptions(L.sortOptions);
const DEBOUNCE_MS = 400;

// M11: list of Користувач accounts. Filters and page live in the URL, so «back» from an account keeps them.
export function SearchTab() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') ?? '';
  const rawStatus = searchParams.get('status');
  const status = rawStatus && rawStatus in USER_STATUS ? rawStatus : '';
  const rawSort = searchParams.get('sort');
  const sort = rawSort && rawSort in L.sortOptions ? rawSort : DEFAULT_USER_SORT;
  const page = Math.max(1, Number.parseInt(searchParams.get('page') ?? '1', 10) || 1);

  const users = useManagerUsers({ email: q.trim() || undefined, status: status || undefined, sort, page });

  // Any filter change goes back to page 1.
  const update = (patch) => {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries({ page: '', ...patch })) {
      if (value && !(key === 'sort' && value === DEFAULT_USER_SORT)) next.set(key, value);
      else next.delete(key);
    }
    setSearchParams(next, { replace: true });
  };

  // The input shows what is typed at once; the URL (and the request) follow after a pause.
  const [draft, setDraft] = useState(q);
  const [seenQ, setSeenQ] = useState(q);
  if (seenQ !== q) {
    setSeenQ(q);
    setDraft(q);
  }
  const commitQuery = useDebouncedCallback((value) => update({ q: value.trim() }), DEBOUNCE_MS);

  const rows = users.data?.content ?? [];
  const totalPages = users.data?.page?.totalPages ?? 0;

  return (
    <Stack gap="md">
      <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="sm">
        <TextInput
          label={L.emailSearch}
          placeholder={L.emailSearchPlaceholder}
          type="search"
          value={draft}
          onChange={(e) => {
            setDraft(e.currentTarget.value);
            commitQuery(e.currentTarget.value);
          }}
        />
        <Select label={L.status} data={STATUS_OPTIONS} value={status} onChange={(v) => update({ status: v })} allowDeselect={false} />
        <Select label={L.sort} data={SORT_OPTIONS} value={sort} onChange={(v) => update({ sort: v })} allowDeselect={false} />
      </SimpleGrid>

      {users.isPending ? (
        <PageLoader />
      ) : users.isError ? (
        <ErrorState error={users.error} onRetry={users.refetch} />
      ) : rows.length === 0 ? (
        <EmptyState title={L.emptyTitle} hint={q || status ? L.emptyHint : undefined} />
      ) : (
        <Stack gap="md" style={{ opacity: users.isPlaceholderData ? 0.6 : 1 }}>
          <Table.ScrollContainer minWidth={560}>
            <Table>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>{L.email}</Table.Th>
                  <Table.Th>{L.status}</Table.Th>
                  <Table.Th>{L.registeredAt}</Table.Th>
                  <Table.Th>{L.lastLoginAt}</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {rows.map((u) => (
                  <Table.Tr key={u.id}>
                    <Table.Td>
                      <Anchor
                        component={Link}
                        to={managerUserPath(u.id)}
                        state={{ from: location.pathname + location.search }}
                        aria-label={L.open(u.email)}
                        style={{ overflowWrap: 'anywhere' }}
                      >
                        {u.email}
                      </Anchor>
                    </Table.Td>
                    <Table.Td>
                      <StatusBadge map={USER_STATUS} value={u.status} />
                    </Table.Td>
                    <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(u.createdAt)}</Table.Td>
                    <Table.Td style={{ whiteSpace: 'nowrap' }}>
                      {u.lastLoginAt ? formatDateTime(u.lastLoginAt) : <Text span c="dimmed" size="sm">{L.never}</Text>}
                    </Table.Td>
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
                onChange={(p) => update({ page: p > 1 ? String(p) : '' })}
                getControlProps={(control) => ({ 'aria-label': PAGINATION_CONTROLS[control] })}
                aria-label={L.pagination}
                component="nav"
              />
            </Group>
          )}
        </Stack>
      )}
    </Stack>
  );
}
