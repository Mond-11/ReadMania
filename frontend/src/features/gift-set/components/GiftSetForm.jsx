import { MultiSelect, NumberInput, Stack } from '@mantine/core';
import { useForm } from '@mantine/form';
import { applyFormErrors } from '../../../shared/api/errors.js';
import { useGenres } from '../../../shared/hooks/useGenres.js';
import { GIFT_SET } from '../../../shared/labels.js';
import { notifyError } from '../../../shared/notify.js';
import { ErrorState, FormActions, PageLoader } from '../../../shared/ui/index.js';

export function GiftSetForm({ onSubmit, loading }) {
  const genres = useGenres();
  const form = useForm({
    initialValues: { genreIds: [], budget: 300, bookCount: '' },
    validate: {
      genreIds: (v) => (v.length === 0 ? GIFT_SET.genresRequired : null),
      budget: (v) => (Number(v) >= 50 && Number(v) % 50 === 0 ? null : GIFT_SET.budgetInvalid),
      bookCount: (v) => (v === '' || (Number.isInteger(Number(v)) && Number(v) >= 1) ? null : GIFT_SET.bookCountInvalid),
    },
  });

  if (genres.isPending) return <PageLoader minHeight={120} />;
  if (genres.isError) return <ErrorState error={genres.error} onRetry={genres.refetch} />;

  const options = genres.data.map((g) => ({ value: String(g.id), label: g.name }));

  const submit = form.onSubmit(async (values) => {
    try {
      await onSubmit({
        genreIds: values.genreIds.map(Number),
        budget: Number(values.budget),
        bookCount: values.bookCount === '' ? undefined : Number(values.bookCount),
      });
    } catch (error) {
      if (!applyFormErrors(form, error)) notifyError(error);
    }
  });

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="sm">
        <MultiSelect
          label={GIFT_SET.genres}
          placeholder={GIFT_SET.genresPlaceholder}
          data={options}
          searchable
          clearable
          withAsterisk
          {...form.getInputProps('genreIds')}
        />
        <NumberInput
          label={GIFT_SET.budget}
          description={GIFT_SET.budgetHint}
          min={50}
          step={50}
          allowDecimal={false}
          allowNegative={false}
          withAsterisk
          {...form.getInputProps('budget')}
        />
        <NumberInput
          label={GIFT_SET.bookCount}
          description={GIFT_SET.bookCountHint}
          min={1}
          allowDecimal={false}
          allowNegative={false}
          {...form.getInputProps('bookCount')}
        />
        <FormActions submitLabel={GIFT_SET.generate} loading={loading} />
      </Stack>
    </form>
  );
}
