import { ActionIcon, Button, Checkbox, Flex, Group, Select, Table, Text, TextInput, Tooltip } from '@mantine/core';
import { useDebouncedCallback, useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { formatNumber } from '../../../shared/format.js';
import { COMMON, INSTITUTION_TYPE, MANAGER_INSTITUTION as L, TERMS, labelOf, toOptions } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, IconPencil, IconTrash, PageLoader } from '../../../shared/ui/index.js';
import { useDeleteInstitutions, useManagerInstitutions } from '../api.js';
import { InstitutionFormModal } from '../components/InstitutionFormModal.jsx';

const TYPE_OPTIONS = toOptions(INSTITUTION_TYPE);
const DEBOUNCE_MS = 400;

export default function InstitutionsPage() {
  useDocumentTitle(`${L.title} — ${TERMS.appName}`);
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') ?? '';
  const rawType = searchParams.get('type');
  const type = rawType && rawType in INSTITUTION_TYPE ? rawType : null;

  const institutions = useManagerInstitutions({ type, query: q.trim() || undefined });
  const remove = useDeleteInstitutions();
  const [selected, setSelected] = useState([]);
  // rows waiting for delete confirmation (one row from its trash icon, or the checked ones)
  const [toDelete, setToDelete] = useState(null);
  // modal: null | { institution: null | object }
  const [modal, setModal] = useState(null);

  // Filters live in the URL (like the catalog); a filter change drops the selection so it never hides rows.
  const setFilter = (patch) => {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(patch)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setSearchParams(next, { replace: true });
    setSelected([]);
  };
  const resetFilters = () => {
    setSearchParams(new URLSearchParams(), { replace: true });
    setSelected([]);
  };

  const [draft, setDraft] = useState(q);
  const [seenQ, setSeenQ] = useState(q);
  if (seenQ !== q) {
    setSeenQ(q);
    setDraft(q);
  }
  const commitQuery = useDebouncedCallback((value) => setFilter({ q: value.trim() }), DEBOUNCE_MS);

  const rows = institutions.data ?? [];
  const deletable = rows.filter((r) => r.canDelete);
  const selectedRows = rows.filter((r) => selected.includes(r.id));
  const allSelected = deletable.length > 0 && selectedRows.length === deletable.length;
  const hasFilters = Boolean(q || type);

  const toggle = (id, checked) => setSelected((prev) => (checked ? [...prev, id] : prev.filter((x) => x !== id)));

  const confirmDelete = () =>
    remove.mutate(toDelete.map((r) => r.id), {
      onSuccess: () => notifySuccess(toDelete.length === 1 ? L.deleted : L.deletedMany(toDelete.length)),
      onError: notifyError,
      onSettled: () => {
        setToDelete(null);
        setSelected([]);
      },
    });

  let body;
  if (institutions.isPending) body = <PageLoader />;
  else if (institutions.isError) body = <ErrorState error={institutions.error} onRetry={institutions.refetch} />;
  else if (!rows.length) {
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
              <Table.Th w={40}>
                <Checkbox
                  aria-label={L.selectAll}
                  checked={allSelected}
                  indeterminate={selectedRows.length > 0 && !allSelected}
                  disabled={deletable.length === 0}
                  onChange={(e) => setSelected(e.currentTarget.checked ? deletable.map((r) => r.id) : [])}
                />
              </Table.Th>
              <Table.Th>{L.name}</Table.Th>
              <Table.Th>{L.type}</Table.Th>
              <Table.Th>{L.address}</Table.Th>
              <Table.Th>{L.contactPerson}</Table.Th>
              <Table.Th ta="right">{L.giftOrdersCount}</Table.Th>
              <Table.Th w={96} />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((row) => (
              <Table.Tr key={row.id}>
                <Table.Td>
                  <Tooltip label={L.deleteBlocked} disabled={row.canDelete} multiline maw={280}>
                    <span>
                      <Checkbox
                        aria-label={row.canDelete ? L.selectOne(row.name) : `${L.selectOne(row.name)}. ${L.deleteBlocked}`}
                        checked={selected.includes(row.id)}
                        disabled={!row.canDelete}
                        onChange={(e) => toggle(row.id, e.currentTarget.checked)}
                      />
                    </span>
                  </Tooltip>
                </Table.Td>
                <Table.Td>{row.name}</Table.Td>
                <Table.Td>{labelOf(INSTITUTION_TYPE, row.type)}</Table.Td>
                <Table.Td>{row.address}</Table.Td>
                <Table.Td>{row.contactPerson}</Table.Td>
                <Table.Td ta="right">{formatNumber(row.giftOrdersCount ?? 0)}</Table.Td>
                <Table.Td>
                  <Group gap={4} justify="flex-end" wrap="nowrap">
                    <ActionIcon variant="subtle" aria-label={L.editOne(row.name)} onClick={() => setModal({ institution: row })}>
                      <IconPencil />
                    </ActionIcon>
                    <Tooltip label={L.deleteBlocked} disabled={row.canDelete} multiline maw={280}>
                      <span>
                        <ActionIcon
                          variant="subtle"
                          color="red"
                          aria-label={row.canDelete ? L.deleteOne(row.name) : `${L.deleteOne(row.name)}. ${L.deleteBlocked}`}
                          disabled={!row.canDelete}
                          onClick={() => setToDelete([row])}
                        >
                          <IconTrash />
                        </ActionIcon>
                      </span>
                    </Tooltip>
                  </Group>
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
        <TextInput
          label={L.search}
          placeholder={L.searchPlaceholder}
          type="search"
          value={draft}
          onChange={(e) => {
            setDraft(e.currentTarget.value);
            commitQuery(e.currentTarget.value);
          }}
          flex={{ base: '0 0 auto', sm: '1 1 260px' }}
        />
        <Select
          label={L.typeFilter}
          placeholder={L.typeFilterPlaceholder}
          data={TYPE_OPTIONS}
          value={type}
          onChange={(value) => setFilter({ type: value })}
          clearable
          flex={{ base: '0 0 auto', sm: '1 1 220px' }}
        />
        <Button variant="default" disabled={!hasFilters} onClick={resetFilters}>
          {COMMON.reset}
        </Button>
        <Group gap="sm" wrap="nowrap" justify="flex-end" ml={{ sm: 'auto' }}>
          {selectedRows.length > 0 && <Text size="sm" aria-live="polite">{L.selected(selectedRows.length)}</Text>}
          <Button onClick={() => setModal({ institution: null })}>{L.add}</Button>
          <ActionIcon
            variant="filled"
            color="red"
            size="lg"
            aria-label={L.deleteSelected}
            disabled={selectedRows.length === 0}
            onClick={() => setToDelete(selectedRows)}
          >
            <IconTrash />
          </ActionIcon>
        </Group>
      </Flex>
      {body}
      {modal && (
        <InstitutionFormModal
          key={modal.institution?.id ?? 'new'}
          opened
          onClose={() => setModal(null)}
          institution={modal.institution}
        />
      )}
      <ConfirmModal
        opened={toDelete != null}
        onClose={() => setToDelete(null)}
        onConfirm={confirmDelete}
        title={toDelete?.length === 1 ? L.deleteTitle : L.deleteSelectedTitle}
        message={toDelete && (toDelete.length === 1 ? L.deleteMessage(toDelete[0].name) : L.deleteSelectedMessage(toDelete.length))}
        confirmLabel={COMMON.delete}
        danger
        loading={remove.isPending}
      />
    </>
  );
}
