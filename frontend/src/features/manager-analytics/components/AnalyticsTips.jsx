import { Badge, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { MANAGER_ANALYTICS as L } from '../../../shared/labels.js';

const KIND = {
  restock: { label: L.tipRestock, color: 'red' },
  review: { label: L.tipReview, color: 'yellow' },
  rating: { label: L.tipRating, color: 'gray' },
};

// «Що варто зробити»: the numbers turned into catalog decisions (SRS M10). Tips come from buildTips.
export default function AnalyticsTips({ tips, hidden }) {
  return (
    <Paper component="section" aria-label={L.tipsTitle} withBorder p="md" mb="xl">
      <Title order={3} fz="h4">{L.tipsTitle}</Title>
      <Text size="sm" c="dimmed" mb="sm">{L.tipsHint}</Text>
      {tips.length ? (
        <Stack component="ul" gap="xs" p={0} m={0} style={{ listStyle: 'none' }}>
          {tips.map((t) => (
            <Group component="li" key={t.key} gap="sm" wrap="nowrap" align="flex-start">
              <Badge color={KIND[t.kind].color} variant="light" radius="sm" w={110} miw={110} mt={2} styles={{ root: { textTransform: 'none' } }}>
                {KIND[t.kind].label}
              </Badge>
              <Text size="sm">{t.text}</Text>
            </Group>
          ))}
        </Stack>
      ) : (
        <Text size="sm">{L.tipsEmpty}</Text>
      )}
      {hidden > 0 && <Text size="sm" c="dimmed" mt="sm">{L.tipsMore(hidden)}</Text>}
    </Paper>
  );
}
