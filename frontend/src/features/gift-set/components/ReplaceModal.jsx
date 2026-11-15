import { Button, Group, Modal, Stack, Text, UnstyledButton } from '@mantine/core';
import { useEffect, useState } from 'react';
import { CATALOG, COMMON, GIFT_SET } from '../../../shared/labels.js';
import { formatMoney } from '../../../shared/format.js';
import { notifyError } from '../../../shared/notify.js';
import { ErrorState, EmptyState, Money, PageLoader } from '../../../shared/ui/index.js';
import { useReplaceCandidates, useReplaceGiftSetBook } from '../api.js';

const PAGE_SIZE = 5;

// `request` = { bookIdToReplace, budget, currentBookIds, genreIds }; `book` is the item being replaced.
export function ReplaceModal({ book, request, onClose, onPickCandidate, onReplaced }) {
  const candidates = useReplaceCandidates();
  const replace = useReplaceGiftSetBook();

  const [visible, setVisible] = useState(PAGE_SIZE);

  useEffect(() => {
    setVisible(PAGE_SIZE);
    if (book) candidates.mutate(request);
    // Reload only when another book is opened for replacement.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [book?.id]);

  const randomReplace = () =>
    replace.mutate(request, {
      onSuccess: onReplaced,
      onError: notifyError,
    });

  const list = candidates.data?.candidates ?? [];

  return (
    <Modal opened={!!book} onClose={onClose} title={GIFT_SET.replaceTitle} size="lg">
      {book && (
        <Stack gap="sm">
          {candidates.isPending && <PageLoader minHeight={120} />}
          {candidates.isError && <ErrorState error={candidates.error} onRetry={() => candidates.mutate(request)} />}
          {candidates.isSuccess && (
            <>
              <Text size="sm" c="dimmed">
                {GIFT_SET.replaceHint(book.title, formatMoney(candidates.data.maxPriceAllowed))}
              </Text>
              {list.length === 0 ? (
                <EmptyState title={GIFT_SET.replaceNone} hint={GIFT_SET.replaceNoneHint} />
              ) : (
                <Stack gap="xs" component="ul" style={{ padding: 0, margin: 0, listStyle: 'none' }}>
                  {list.slice(0, visible).map((c) => (
                    <li key={c.id}>
                      <UnstyledButton
                        onClick={() => onPickCandidate(c)}
                        style={{ width: '100%', padding: 'var(--mantine-spacing-xs)', border: '1px solid var(--rm-border, var(--mantine-color-gray-3))', borderRadius: 'var(--mantine-radius-sm)' }}
                      >
                        <Group justify="space-between" wrap="nowrap" gap="sm">
                          <Stack gap={0} style={{ minWidth: 0 }}>
                            <Text fw={600}>{c.title}</Text>
                            <Text size="sm" c="dimmed">
                              {c.authors?.map((a) => a.name).join(', ') || CATALOG.authorUnknown}
                              {' · '}{GIFT_SET.inStock} {c.stock} {CATALOG.pcs}
                            </Text>
                          </Stack>
                          <Money value={c.price} fw={700} />
                        </Group>
                      </UnstyledButton>
                    </li>
                  ))}
                </Stack>
              )}
              {list.length > visible && (
                <Button variant="default" onClick={() => setVisible((v) => v + PAGE_SIZE)}>
                  {GIFT_SET.showMore} ({list.length - visible})
                </Button>
              )}
              <Group justify="flex-end" gap="sm" mt="sm">
                <Button variant="default" onClick={onClose}>{COMMON.cancel}</Button>
                <Button onClick={randomReplace} loading={replace.isPending} disabled={list.length === 0}>
                  {GIFT_SET.replaceRandom}
                </Button>
              </Group>
            </>
          )}
        </Stack>
      )}
    </Modal>
  );
}
