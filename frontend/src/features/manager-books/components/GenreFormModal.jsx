import { Button, Group, Modal, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { applyFormErrors } from '../../../shared/api/index.js';
import { COMMON, MANAGER_CATALOG as L } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { useSaveGenre } from '../api.js';

// genre === null: create; otherwise rename. The parent remounts it per open (key) so the form starts clean.
export function GenreFormModal({ opened, onClose, genre }) {
  const save = useSaveGenre();
  const form = useForm({
    initialValues: { name: genre?.name ?? '' },
    validate: {
      name: (v) => (v.trim().length === 0 || v.trim().length > 100 ? L.genreNameError : null),
    },
  });

  const submit = form.onSubmit((values) =>
    save.mutate(
      { id: genre?.id, name: values.name.trim() },
      {
        onSuccess: () => {
          notifySuccess(genre ? L.genreRenamed : L.genreCreated);
          onClose();
        },
        onError: (error) => {
          if (error?.status === 409) form.setFieldError('name', L.genreDuplicate);
          else if (!applyFormErrors(form, error)) notifyError(error);
        },
      },
    ),
  );

  return (
    <Modal opened={opened} onClose={onClose} title={genre ? L.renameGenreTitle : L.createGenreTitle} closeOnClickOutside={!save.isPending}>
      <form onSubmit={submit} noValidate>
        <TextInput
          label={L.genreName}
          placeholder={L.genreNamePlaceholder}
          required
          data-autofocus
          {...form.getInputProps('name')}
        />
        <Group justify="flex-end" gap="sm" mt="md">
          <Button variant="default" onClick={onClose} disabled={save.isPending}>{COMMON.cancel}</Button>
          <Button type="submit" loading={save.isPending}>{COMMON.save}</Button>
        </Group>
      </form>
    </Modal>
  );
}
