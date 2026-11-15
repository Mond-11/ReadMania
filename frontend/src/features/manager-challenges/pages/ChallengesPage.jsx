import { ActionIcon, Button, Group, SegmentedControl, Stack, Table, Text, Tooltip, VisuallyHidden } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { BOOK_FORMS, formatCount, formatDate, formatNumber, POINT_FORMS } from '../../../shared/format.js';
import { CHALLENGE_STATUS, MANAGER_CHALLENGES as L, TERMS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, IconPencil, PageLoader, Section, StatusBadge } from '../../../shared/ui/index.js';
import { useFinishChallenge, useManagerChallenges } from '../api.js';
import { ChallengeFormModal } from '../components/ChallengeFormModal.jsx';

const STATUS_FILTER = [
  { value: '', label: L.allStatuses },
  ...Object.entries(CHALLENGE_STATUS).map(([value, label]) => ({ value, label })),
];

// M4: list of reading challenges; create (M4.1), edit an active one (M4.2), finish early (M4.3).
export default function ChallengesPage() {
  useDocumentTitle(`${L.title} — ${TERMS.appName}`);
  const [searchParams, setSearchParams] = useSearchParams();
  const raw = searchParams.get('status');
  const status = raw && raw in CHALLENGE_STATUS ? raw : '';
  const challenges = useManagerChallenges(status || undefined);
  const finish = useFinishChallenge();

  // modal: null | { challenge: null | object }; `key` remounts the form for every opening.
  const [modal, setModal] = useState(null);
  const [toFinish, setToFinish] = useState(null);

  const setStatus = (value) => setSearchParams(value ? { status: value } : {}, { replace: true });

  const confirmFinish = () =>
    finish.mutate(toFinish.id, {
      onSuccess: (res) => notifySuccess(L.finished(res?.notifiedUsersCount ?? 0)),
      onError: notifyError,
      onSettled: () => setToFinish(null),
    });

  const rows = challenges.data ?? [];

  let body;
  if (challenges.isPending) body = <PageLoader />;
  else if (challenges.isError) body = <ErrorState error={challenges.error} onRetry={challenges.refetch} />;
  else if (!rows.length) {
    body = status ? (
      <EmptyState title={L.emptyFiltered} />
    ) : (
      <EmptyState title={L.emptyTitle} hint={L.emptyHint} action={<Button onClick={() => setModal({ challenge: null })}>{L.create}</Button>} />
    );
  } else {
    body = (
      <Table.ScrollContainer minWidth={880}>
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{L.name}</Table.Th>
              <Table.Th>{L.condition}</Table.Th>
              <Table.Th>{L.deadline}</Table.Th>
              <Table.Th ta="right">{L.reward}</Table.Th>
              <Table.Th miw={120}>{L.status}</Table.Th>
              <Table.Th w={180}>
                <VisuallyHidden>{L.actions}</VisuallyHidden>
              </Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((c) => {
              const active = c.status === 'ACTIVE';
              return (
                <Table.Tr key={c.id}>
                  <Table.Td>
                    <Text fw={600}>{c.name}</Text>
                    {c.description && (
                      <Text size="xs" c="dimmed" lineClamp={2}>
                        {c.description}
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td>
                    {formatCount(c.requiredBooks, BOOK_FORMS)}
                    {c.genreName && <Text size="xs" c="dimmed">{c.genreName}</Text>}
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDate(c.deadline)}</Table.Td>
                  <Table.Td ta="right" title={formatCount(c.rewardPoints, POINT_FORMS)}>
                    {formatNumber(c.rewardPoints)}
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>
                    <StatusBadge map={CHALLENGE_STATUS} value={c.status} />
                  </Table.Td>
                  <Table.Td>
                    {active && (
                      <Group gap={4} justify="flex-end" wrap="nowrap">
                        <Tooltip label={L.edit(c.name)}>
                          <ActionIcon variant="subtle" aria-label={L.edit(c.name)} onClick={() => setModal({ challenge: c })}>
                            <IconPencil />
                          </ActionIcon>
                        </Tooltip>
                        <Button size="compact-sm" variant="light" color="red" onClick={() => setToFinish(c)} aria-label={L.finishOne(c.name)}>
                          {L.finish}
                        </Button>
                      </Group>
                    )}
                  </Table.Td>
                </Table.Tr>
              );
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    );
  }

  return (
    <Stack gap="md">
      <Section title={L.title} actions={<Button onClick={() => setModal({ challenge: null })}>{L.create}</Button>}>
        <Text size="sm" c="dimmed">{L.description}</Text>
        <SegmentedControl
          value={status}
          onChange={setStatus}
          data={STATUS_FILTER}
          aria-label={L.statusFilter}
          fullWidth
          styles={{ root: { overflowX: 'auto' } }}
        />
        {body}
      </Section>

      {modal && (
        <ChallengeFormModal
          key={modal.challenge?.id ?? 'new'}
          opened
          onClose={() => setModal(null)}
          challenge={modal.challenge}
        />
      )}
      <ConfirmModal
        opened={Boolean(toFinish)}
        onClose={() => setToFinish(null)}
        onConfirm={confirmFinish}
        title={L.finishTitle}
        message={toFinish ? L.finishMessage(toFinish.name) : ''}
        confirmLabel={L.finish}
        danger
        loading={finish.isPending}
      />
    </Stack>
  );
}
