import { Alert, Button, FileInput, Group, Stack, Table, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { Link } from 'react-router';
import { formatNumber } from '../../../shared/format.js';
import { CATALOG, MANAGER_CATALOG as L, TERMS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { FormActions, InfoList, PageHeader, Section } from '../../../shared/ui/index.js';
import { useImportBooksCsv } from '../api.js';
import { normalizeCsvEncoding } from '../csvEncoding.js';

// The backend names the missing columns in English; show the Ukrainian names instead (matched by the message prefix).
const MISSING_COLUMNS_PREFIX = 'Файл не містить обов\'язкових колонок';
const reasonOf = (row) => (row.reason?.startsWith(MISSING_COLUMNS_PREFIX) ? L.importMissingColumns : row.reason);

export default function ImportCsvPage() {
  useDocumentTitle(`${L.importTitle} — ${TERMS.appName}`);
  const importCsv = useImportBooksCsv();
  const [result, setResult] = useState(null);

  const form = useForm({
    initialValues: { file: null },
    validate: {
      file: (file) => {
        if (!file) return L.importFileRequired;
        return /\.csv$/i.test(file.name) ? null : L.importFileType;
      },
    },
  });

  const submit = async ({ file }) => {
    let prepared;
    try {
      prepared = await normalizeCsvEncoding(file);
    } catch (error) {
      notifyError(error);
      return;
    }
    importCsv.mutate(prepared, {
      onSuccess: (data) => {
        setResult(data);
        notifySuccess(L.importDone(data?.addedCount ?? 0));
      },
      onError: notifyError,
    });
  };

  const reset = () => {
    setResult(null);
    form.reset();
  };

  const rejected = result?.rejectedRows ?? [];

  return (
    <>
      <PageHeader
        title={L.importTitle}
        description={L.importDescription}
        breadcrumbs={[{ label: CATALOG.title, to: '/' }, { label: L.importTitle }]}
      />
      <Stack gap="md">
        {!result && (
          <Section title={L.importUploadTitle}>
            <form onSubmit={form.onSubmit(submit)} noValidate>
              <Stack gap="md">
                <FileInput
                  label={L.importFile}
                  description={L.importFormatHint}
                  placeholder={L.importFilePlaceholder}
                  accept=".csv,text/csv"
                  clearable
                  {...form.getInputProps('file')}
                />
                <Text size="sm" c="dimmed" mt={-8}>{L.importColumnsHint}</Text>
                <FormActions submitLabel={L.importSubmit} loading={importCsv.isPending} />
              </Stack>
            </form>
          </Section>
        )}

        {result && (
          <>
            <Section title={L.importResult}>
              <Stack gap="md">
                <InfoList
                  rows={[
                    { label: L.importTotal, value: formatNumber(result.totalRows ?? 0) },
                    { label: L.importAdded, value: formatNumber(result.addedCount ?? 0) },
                    { label: L.importRejected, value: formatNumber(result.rejectedCount ?? rejected.length) },
                  ]}
                />
                {!rejected.length && <Alert color="green">{L.importAllAdded}</Alert>}
                {!!rejected.length && !result.addedCount && <Alert color="red">{L.importNoneAdded}</Alert>}
                <Group gap="sm">
                  <Button onClick={reset}>{L.importAgain}</Button>
                  <Button component={Link} to="/" variant="default">{L.toCatalog}</Button>
                </Group>
              </Stack>
            </Section>

            {!!rejected.length && (
              <Section title={L.importRejectedTitle}>
                <Table.ScrollContainer minWidth={320}>
                  <Table>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th ta="right">{L.importRowNumber}</Table.Th>
                        <Table.Th>{L.importIsbn}</Table.Th>
                        <Table.Th>{L.importBookTitle}</Table.Th>
                        <Table.Th>{L.importReason}</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {rejected.map((row) => (
                        <Table.Tr key={`${row.rowNumber}-${row.isbn}`}>
                          <Table.Td ta="right">{row.rowNumber}</Table.Td>
                          <Table.Td>{row.isbn || '—'}</Table.Td>
                          <Table.Td>{row.title || '—'}</Table.Td>
                          <Table.Td><Text size="sm">{reasonOf(row)}</Text></Table.Td>
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                </Table.ScrollContainer>
              </Section>
            )}
          </>
        )}
      </Stack>
    </>
  );
}
