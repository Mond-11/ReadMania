import { Anchor, Badge, Card, Group, Progress, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link } from 'react-router';
import { BOOK_FORMS, formatCount, formatDate, formatDateTime, formatNumber, POINT_FORMS } from '../../../shared/format.js';
import { ACCOUNT, BONUS_TRANSACTION_TYPE, labelOf, LOYALTY, TERMS } from '../../../shared/labels.js';
import { EmptyState, ErrorState, InfoList, PageLoader, Section } from '../../../shared/ui/index.js';
import { myOrderPath } from '../../orders/index.js';
import { useChallenges, useLoyalty } from '../api.js';

// C7: active challenges with own progress, completed challenges and the bonus balance with its log.
export default function ChallengesPage() {
  useDocumentTitle(`${ACCOUNT.challengesTab} — ${TERMS.appName}`);
  const loyalty = useLoyalty();
  const challenges = useChallenges();

  // GET /api/challenges returns the active challenges plus the ones this user completed (also already ended ones).
  const all = challenges.data ?? [];
  const active = all.filter((c) => c.status === 'ACTIVE');
  const completed = all
    .filter((c) => c.completed)
    .sort((a, b) => new Date(b.completedAt ?? 0) - new Date(a.completedAt ?? 0));

  return (
    <Stack gap="md">
      <Section title={LOYALTY.balanceTitle}>
        {loyalty.isPending ? (
          <PageLoader minHeight={80} />
        ) : loyalty.isError ? (
          <ErrorState error={loyalty.error} onRetry={loyalty.refetch} />
        ) : (
          <Stack gap={4}>
            <Text fz={32} fw={800} lh={1.2}>
              {formatCount(loyalty.data.bonusBalance, POINT_FORMS)}
            </Text>
            <Text size="sm" c="dimmed">{LOYALTY.balanceHint}</Text>
          </Stack>
        )}
      </Section>

      <Section title={LOYALTY.activeTitle}>
        {challenges.isPending ? (
          <PageLoader minHeight={120} />
        ) : challenges.isError ? (
          <ErrorState error={challenges.error} onRetry={challenges.refetch} />
        ) : active.length === 0 ? (
          <EmptyState title={LOYALTY.activeEmpty} hint={LOYALTY.activeEmptyHint} />
        ) : (
          <Stack gap="sm">
            <Text size="sm" c="dimmed">{LOYALTY.progressHint}</Text>
            <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
              {active.map((c) => (
                <ChallengeCard key={c.id} challenge={c} />
              ))}
            </SimpleGrid>
          </Stack>
        )}
      </Section>

      <Section title={LOYALTY.completedTitle}>
        {challenges.isPending ? (
          <PageLoader minHeight={80} />
        ) : challenges.isError ? null : completed.length === 0 ? (
          <EmptyState title={LOYALTY.completedEmpty} />
        ) : (
          <Stack gap={6} component="ul" p={0} m={0} style={{ listStyle: 'none' }}>
            {completed.map((c) => (
              <Group key={c.id} component="li" justify="space-between" gap="sm" wrap="wrap">
                <Text fw={600}>{c.name}</Text>
                <Text size="sm" c="dimmed">
                  {formatDate(c.completedAt)} · +{formatCount(c.rewardPoints, POINT_FORMS)}
                </Text>
              </Group>
            ))}
          </Stack>
        )}
      </Section>

      <Section title={LOYALTY.historyTitle}>
        {loyalty.isPending ? (
          <PageLoader minHeight={80} />
        ) : loyalty.isError ? null : loyalty.data.transactions.length === 0 ? (
          <EmptyState title={LOYALTY.historyEmpty} />
        ) : (
          <Table.ScrollContainer minWidth={520}>
            <Table>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>{LOYALTY.operation}</Table.Th>
                  <Table.Th>{LOYALTY.details}</Table.Th>
                  <Table.Th>{LOYALTY.date}</Table.Th>
                  <Table.Th ta="right">{LOYALTY.points}</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {loyalty.data.transactions.map((t) => (
                  <Table.Tr key={t.id}>
                    <Table.Td>{labelOf(BONUS_TRANSACTION_TYPE, t.type)}</Table.Td>
                    <Table.Td>
                      {t.orderId ? (
                        <Anchor component={Link} to={myOrderPath(t.orderId)} size="sm">
                          {LOYALTY.orderRef}{t.orderId}
                        </Anchor>
                      ) : (
                        <Text size="sm">{t.challengeName ?? ''}</Text>
                      )}
                    </Table.Td>
                    <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(t.createdAt)}</Table.Td>
                    {/* Sign is shown as text, not only by color. */}
                    <Table.Td ta="right" fw={600} c={t.points < 0 ? 'red' : undefined}>
                      {t.points > 0 ? '+' : ''}{formatNumber(t.points)}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Section>
    </Stack>
  );
}

function ChallengeCard({ challenge: c }) {
  const done = Math.min(c.userProgress, c.requiredBooks);
  const percent = c.requiredBooks > 0 ? Math.round((done / c.requiredBooks) * 100) : 0;
  const progressText = LOYALTY.progress(done, c.requiredBooks);

  return (
    <Card padding="md">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="xs" wrap="nowrap">
          <Title order={3} size="h5">{c.name}</Title>
          {c.completed && (
            <Badge color="brand" variant="light" radius="sm" styles={{ root: { textTransform: 'none', flexShrink: 0 } }}>
              {LOYALTY.completedAt}
            </Badge>
          )}
        </Group>
        {c.description && <Text size="sm">{c.description}</Text>}
        <Stack gap={4}>
          <Progress value={percent} color="brand" aria-label={progressText} />
          <Text size="sm" fw={600}>{progressText}</Text>
        </Stack>
        <InfoList
          rows={[
            { label: LOYALTY.condition, value: LOYALTY.conditionText(formatCount(c.requiredBooks, BOOK_FORMS), c.genreName) },
            { label: LOYALTY.deadline, value: formatDate(c.deadline) },
            { label: LOYALTY.reward, value: formatCount(c.rewardPoints, POINT_FORMS) },
            c.completed && { label: LOYALTY.completedAt, value: formatDate(c.completedAt) },
          ]}
        />
      </Stack>
    </Card>
  );
}
