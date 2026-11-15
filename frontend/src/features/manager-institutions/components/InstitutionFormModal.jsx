import { Button, Group, Modal, Select, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { applyFormErrors } from '../../../shared/api/index.js';
import { COMMON, INSTITUTION_TYPE, MANAGER_INSTITUTION as L, toOptions } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { useSaveInstitution } from '../api.js';

const TYPE_OPTIONS = toOptions(INSTITUTION_TYPE);
const required = (message) => (v) => (String(v ?? '').trim() ? null : message);

// institution === null: create; otherwise edit. The parent remounts it per open (key) so the form starts clean.
export function InstitutionFormModal({ opened, onClose, institution }) {
  const save = useSaveInstitution();
  const form = useForm({
    initialValues: {
      name: institution?.name ?? '',
      type: institution?.type ?? null,
      address: institution?.address ?? '',
      contactPerson: institution?.contactPerson ?? '',
    },
    validate: {
      name: required(L.nameError),
      type: required(L.typeError),
      address: required(L.addressError),
      contactPerson: required(L.contactPersonError),
    },
  });

  const submit = form.onSubmit((values) =>
    save.mutate(
      {
        id: institution?.id,
        name: values.name.trim(),
        type: values.type,
        address: values.address.trim(),
        contactPerson: values.contactPerson.trim(),
      },
      {
        onSuccess: () => {
          notifySuccess(institution ? L.updated : L.created);
          onClose();
        },
        onError: (error) => {
          if (!applyFormErrors(form, error)) notifyError(error);
        },
      },
    ),
  );

  return (
    <Modal opened={opened} onClose={onClose} title={institution ? L.editTitle : L.createTitle} closeOnClickOutside={!save.isPending}>
      <form onSubmit={submit} noValidate>
        <Stack gap="sm">
          <TextInput label={L.name} placeholder={L.namePlaceholder} required data-autofocus {...form.getInputProps('name')} />
          <Select
            label={L.type}
            data={TYPE_OPTIONS}
            allowDeselect={false}
            required
            {...form.getInputProps('type')}
          />
          <TextInput label={L.address} placeholder={L.addressPlaceholder} required {...form.getInputProps('address')} />
          <TextInput
            label={L.contactPerson}
            placeholder={L.contactPersonPlaceholder}
            required
            {...form.getInputProps('contactPerson')}
          />
        </Stack>
        <Group justify="flex-end" gap="sm" mt="md">
          <Button variant="default" onClick={onClose} disabled={save.isPending}>{COMMON.cancel}</Button>
          <Button type="submit" loading={save.isPending}>{COMMON.save}</Button>
        </Group>
      </form>
    </Modal>
  );
}
