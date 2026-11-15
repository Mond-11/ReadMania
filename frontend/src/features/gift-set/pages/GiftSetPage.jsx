import { Alert, Button, Divider, Group, SimpleGrid, Stack, Text } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { Link } from 'react-router';
import { GIFT_SET, TERMS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { Money, PageHeader, Section } from '../../../shared/ui/index.js';
import { useAddGiftSetToCart, useGenerateGiftSet } from '../api.js';
import { GiftSetBookRow } from '../components/GiftSetBookRow.jsx';
import { GiftSetForm } from '../components/GiftSetForm.jsx';
import { ReplaceModal } from '../components/ReplaceModal.jsx';

export default function GiftSetPage() {
  useDocumentTitle(`${GIFT_SET.title} — ${TERMS.appName}`);
  const generate = useGenerateGiftSet();
  const addToCart = useAddGiftSetToCart();

  const [params, setParams] = useState(null);
  const [giftSet, setGiftSet] = useState(null);
  const [failure, setFailure] = useState(null);
  const [replacing, setReplacing] = useState(null);
  const [addedToCart, setAddedToCart] = useState(false);

  const run = async (nextParams) => {
    setFailure(null);
    setAddedToCart(false);
    try {
      const result = await generate.mutateAsync(nextParams);
      setParams(nextParams);
      setGiftSet(result);
    } catch (error) {
      // Field errors go back to the form; the two business reasons (budget / genres) are shown inline.
      if (error.status !== 400 || Object.keys(error.fieldErrors).length > 0) throw error;
      setGiftSet(null);
      setFailure(error);
    }
  };

  const items = giftSet?.items ?? [];
  const busy = addToCart.isPending;

  const replaceRequest = replacing && {
    bookIdToReplace: replacing.id,
    budget: giftSet.budget,
    currentBookIds: items.map((b) => b.id),
    genreIds: params.genreIds,
  };

  const pickCandidate = (candidate) => {
    const nextItems = items.map((b) => (b.id === replacing.id ? candidate : b));
    const totalPrice = nextItems.reduce((sum, b) => sum + Number(b.price), 0);
    setGiftSet({ ...giftSet, items: nextItems, totalPrice, remainingBudget: giftSet.budget - totalPrice });
    setReplacing(null);
    setAddedToCart(false);
    notifySuccess(GIFT_SET.replaced);
  };

  // The set lives only on the client until it is added to the cart, so removal needs no request.
  const removeBook = (id) => {
    const nextItems = items.filter((b) => b.id !== id);
    setAddedToCart(false);
    if (nextItems.length === 0) {
      setGiftSet(null);
      return;
    }
    const totalPrice = nextItems.reduce((sum, b) => sum + Number(b.price), 0);
    setGiftSet({ ...giftSet, items: nextItems, totalPrice, remainingBudget: giftSet.budget - totalPrice });
    notifySuccess(GIFT_SET.removed);
  };

  const replaced = (response) => {
    setGiftSet(response);
    setReplacing(null);
    setAddedToCart(false);
    notifySuccess(GIFT_SET.replaced);
  };

  const handleAddToCart = () => {
    addToCart.mutate(items.map((b) => b.id), {
      onSuccess: () => {
        setAddedToCart(true);
        notifySuccess(GIFT_SET.addedToCart);
      },
      onError: (error) => {
        notifyError(error);
        if (error.added > 0) notifyError(GIFT_SET.partialAdd(error.added, items.length), { raw: true });
      },
    });
  };

  return (
    <>
      <PageHeader title={GIFT_SET.title} description={GIFT_SET.description} />

      <Stack gap="md">
        <Section title={GIFT_SET.paramsSection}>
          <GiftSetForm onSubmit={run} loading={generate.isPending} />
        </Section>

        {failure && (
          <Alert color="red" title={GIFT_SET.failTitle} role="alert">
            {failure.detail}
          </Alert>
        )}

        {giftSet && (
          <Section title={GIFT_SET.resultSection}>
            <SimpleGrid cols={{ base: 1, md: 2, xl: 3 }} spacing="sm" component="ul" style={{ padding: 0, margin: 0 }}>
              {items.map((book) => (
                <GiftSetBookRow
                  key={book.id}
                  book={book}
                  disabled={busy}
                  onReplace={() => setReplacing(book)}
                  onRemove={() => removeBook(book.id)}
                />
              ))}
            </SimpleGrid>

            <Divider my="md" />

            <Group justify="space-between" align="flex-end" gap="xl">
              <Group gap="sm">
                <Button onClick={handleAddToCart} loading={addToCart.isPending} disabled={addedToCart}>
                  {GIFT_SET.addToCart}
                </Button>
                <Button variant="default" onClick={() => run(params)} loading={generate.isPending} disabled={busy}>
                  {GIFT_SET.retry}
                </Button>
                {addedToCart && <Button component={Link} to="/cart" variant="subtle">{GIFT_SET.goToCart}</Button>}
              </Group>
              <Stack gap={0} align="flex-end">
                <Group gap={6}><Text c="dimmed" size="sm">{GIFT_SET.budgetLabel}:</Text><Money value={giftSet.budget} size="sm" c="dimmed" /></Group>
                <Group gap={6}><Text c="dimmed" size="sm">{GIFT_SET.remaining}:</Text><Money value={giftSet.remainingBudget} size="sm" c="dimmed" /></Group>
                <Group gap={6}><Text fw={600}>{GIFT_SET.total}:</Text><Money value={giftSet.totalPrice} fw={700} size="lg" /></Group>
              </Stack>
            </Group>
          </Section>
        )}
      </Stack>

      <ReplaceModal
        book={replacing}
        request={replaceRequest}
        onClose={() => setReplacing(null)}
        onPickCandidate={pickCandidate}
        onReplaced={replaced}
      />
    </>
  );
}
