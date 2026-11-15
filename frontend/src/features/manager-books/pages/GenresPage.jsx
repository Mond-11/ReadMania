import { ActionIcon, Button, Group, Table, Tooltip } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { useState } from 'react';
import { formatNumber } from '../../../shared/format.js';
import { useGenres } from '../../../shared/hooks/useGenres.js';
import { CATALOG, COMMON, MANAGER_CATALOG as L, TERMS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, EmptyState, ErrorState, IconPencil, IconTrash, PageHeader, PageLoader, Section } from '../../../shared/ui/index.js';
import { useDeleteGenre } from '../api.js';
import { GenreFormModal } from '../components/GenreFormModal.jsx';

export default function GenresPage() {
  useDocumentTitle(`${L.genresTitle} — ${TERMS.appName}`);
  const genres = useGenres();
  const remove = useDeleteGenre();
  // modal: null | { genre: null | object } (form); toDelete: null | object
  const [modal, setModal] = useState(null);
  const [toDelete, setToDelete] = useState(null);

  const confirmDelete = () =>
    remove.mutate(toDelete.id, {
      onSuccess: () => notifySuccess(L.genreDeleted),
      onError: notifyError,
      onSettled: () => setToDelete(null),
    });

  const header = (
    <PageHeader
      title={L.genresTitle}
      description={L.genresDescription}
      breadcrumbs={[{ label: CATALOG.title, to: '/' }, { label: L.genresTitle }]}
      actions={<Button onClick={() => setModal({ genre: null })}>{L.addGenre}</Button>}
    />
  );

  let body;
  if (genres.isPending) body = <PageLoader />;
  else if (genres.isError) body = <ErrorState error={genres.error} onRetry={genres.refetch} />;
  else if (!genres.data.length) body = <EmptyState title={L.genresEmptyTitle} hint={L.genresEmptyHint} />;
  else {
    body = (
      <Section title={L.genresList}>
        <Table.ScrollContainer minWidth={320}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{L.genreName}</Table.Th>
                <Table.Th ta="right">{L.booksCount}</Table.Th>
                <Table.Th w={96} />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {genres.data.map((genre) => {
                const blocked = genre.booksCount > 0;
                return (
                  <Table.Tr key={genre.id}>
                    <Table.Td>{genre.name}</Table.Td>
                    <Table.Td ta="right">{formatNumber(genre.booksCount ?? 0)}</Table.Td>
                    <Table.Td>
                      <Group gap={4} justify="flex-end" wrap="nowrap">
                        <ActionIcon variant="subtle" aria-label={L.renameGenre(genre.name)} onClick={() => setModal({ genre })}>
                          <IconPencil />
                        </ActionIcon>
                        <Tooltip label={L.deleteGenreBlocked} disabled={!blocked}>
                          <span>
                            <ActionIcon
                              variant="subtle"
                              color="red"
                              aria-label={blocked ? `${L.deleteGenre(genre.name)}. ${L.deleteGenreBlocked}` : L.deleteGenre(genre.name)}
                              disabled={blocked}
                              onClick={() => setToDelete(genre)}
                            >
                              <IconTrash />
                            </ActionIcon>
                          </span>
                        </Tooltip>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                );
              })}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Section>
    );
  }

  return (
    <>
      {header}
      {body}
      {modal && (
        <GenreFormModal
          key={modal.genre?.id ?? 'new'}
          opened
          onClose={() => setModal(null)}
          genre={modal.genre}
        />
      )}
      <ConfirmModal
        opened={toDelete != null}
        onClose={() => setToDelete(null)}
        onConfirm={confirmDelete}
        title={L.deleteGenreTitle}
        message={toDelete && L.deleteGenreMessage(toDelete.name)}
        confirmLabel={COMMON.delete}
        danger
        loading={remove.isPending}
      />
    </>
  );
}
