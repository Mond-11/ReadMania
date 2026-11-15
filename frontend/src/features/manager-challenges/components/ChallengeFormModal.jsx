import { Alert, Button, Group, Modal, NumberInput, Select, SimpleGrid, Stack, Textarea, TextInput } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import dayjs from 'dayjs';
import { applyFormErrors } from '../../../shared/api/index.js';
import { useGenres } from '../../../shared/hooks/useGenres.js';
import { COMMON, MANAGER_CHALLENGES as L } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { useSaveChallenge } from '../api.js';

const DATE = 'YYYY-MM-DD';
const isWholeFrom = (min) => (v) => Number.isInteger(v) && v >= min;

// The deadline is a calendar date; the challenge lasts until the end of that day in local time.
const toDeadline = (date) => dayjs(date).endOf('day').toISOString();

// Backend field names: `title` in the request, `name` in the response.
const toFieldErrors = (fields) => (fields.title ? { ...fields, name: fields.title } : fields);

// challenge === null: M4.1 create; otherwise M4.2 edit of an active challenge.
// The parent remounts the modal per open (key), so the form starts clean.
export function ChallengeFormModal({ opened, onClose, challenge }) {
  const save = useSaveChallenge();
  const genres = useGenres();
  const genreOptions = (genres.data ?? []).map((g) => ({ value: String(g.id), label: g.name }));

  const form = useForm({
    mode: 'controlled',
    initialValues: {
      name: challenge?.name ?? '',
      description: challenge?.description ?? '',
      requiredBooks: challenge?.requiredBooks ?? 3,
      genreId: challenge?.genreId ? String(challenge.genreId) : null,
      deadline: challenge ? dayjs(challenge.deadline).format(DATE) : null,
      rewardPoints: challenge?.rewardPoints ?? 50,
    },
    validate: {
      name: (v) => (v.trim() ? null : L.nameRequired),
      requiredBooks: (v) => (isWholeFrom(1)(v) ? null : L.requiredBooksError),
      rewardPoints: (v) => (isWholeFrom(0)(v) ? null : L.rewardError),
      deadline: (v) => {
        if (!v) return L.deadlineRequired;
        return dayjs(v).endOf('day').isAfter(dayjs()) ? null : L.deadlinePast;
      },
    },
    validateInputOnBlur: true,
  });

  const submit = form.onSubmit((values) =>
    save.mutate(
      {
        id: challenge?.id,
        title: values.name.trim(),
        description: values.description.trim() || null,
        requiredBooks: values.requiredBooks,
        genreId: values.genreId ? Number(values.genreId) : null,
        deadline: toDeadline(values.deadline),
        rewardPoints: values.rewardPoints,
      },
      {
        onSuccess: () => {
          notifySuccess(challenge ? L.updated : L.created);
          onClose();
        },
        onError: (error) => {
          if (error.fieldErrors && Object.keys(error.fieldErrors).length) {
            form.setErrors(toFieldErrors(error.fieldErrors));
          } else if (!applyFormErrors(form, error)) {
            notifyError(error);
          }
        },
      },
    ),
  );

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={challenge ? L.editTitle : L.createTitle}
      size="lg"
      closeOnClickOutside={!save.isPending}
    >
      <form onSubmit={submit} noValidate>
        <Stack gap="sm">
          {challenge && (
            <Alert color="gray" variant="light">
              {L.editWarning}
            </Alert>
          )}
          <TextInput label={L.name} placeholder={L.namePlaceholder} required data-autofocus {...form.getInputProps('name')} />
          <Textarea
            label={L.descriptionLabel}
            placeholder={L.descriptionPlaceholder}
            autosize
            minRows={2}
            maxRows={5}
            {...form.getInputProps('description')}
          />
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
            <NumberInput
              label={L.requiredBooks}
              description={L.requiredBooksHint}
              min={1}
              allowDecimal={false}
              allowNegative={false}
              required
              {...form.getInputProps('requiredBooks')}
            />
            <Select
              label={L.genre}
              placeholder={L.genreAny}
              data={genreOptions}
              clearable
              searchable
              disabled={genres.isPending}
              {...form.getInputProps('genreId')}
            />
            <DateInput
              label={L.deadline}
              description={L.deadlineHint}
              valueFormat="DD.MM.YYYY"
              minDate={dayjs().format(DATE)}
              required
              {...form.getInputProps('deadline')}
            />
            <NumberInput
              label={L.reward}
              min={0}
              allowDecimal={false}
              allowNegative={false}
              required
              {...form.getInputProps('rewardPoints')}
            />
          </SimpleGrid>
        </Stack>
        <Group justify="flex-end" gap="sm" mt="md">
          <Button variant="default" onClick={onClose} disabled={save.isPending}>
            {COMMON.cancel}
          </Button>
          <Button type="submit" loading={save.isPending}>
            {challenge ? COMMON.save : L.create}
          </Button>
        </Group>
      </form>
    </Modal>
  );
}
