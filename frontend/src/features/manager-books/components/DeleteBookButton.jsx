import { ActionIcon, Box, Button, Tooltip } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useNavigate } from 'react-router';
import { MANAGER_CATALOG as L } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ConfirmModal, IconTrash } from '../../../shared/ui/index.js';
import { useDeleteBook } from '../api.js';

// Text button from `sm` up, icon-only button below it (same action, same handler).
export function DeleteBookButton({ book }) {
  const [opened, { open, close }] = useDisclosure(false);
  const navigate = useNavigate();
  const remove = useDeleteBook();
  const blocked = book.canDelete === false;

  const confirm = () =>
    remove.mutate(book.id, {
      onSuccess: () => {
        close();
        notifySuccess(L.bookDeleted);
        navigate('/', { replace: true });
      },
      onError: (error) => {
        close();
        notifyError(error);
      },
    });

  // A disabled control swallows hover/focus, so the tooltip hangs on a focusable wrapper.
  const withReason = (node) =>
    blocked ? (
      <Tooltip label={L.deleteBookBlocked} multiline w={280}>
        <span tabIndex={0}>{node}</span>
      </Tooltip>
    ) : (
      node
    );

  return (
    <>
      <Box component="span" visibleFrom="sm">
        {withReason(
          <Button color="red" variant="outline" onClick={open} disabled={blocked}>
            {L.deleteBook}
          </Button>,
        )}
      </Box>
      <Box component="span" hiddenFrom="sm">
        {withReason(
          <ActionIcon color="red" variant="outline" size="lg" onClick={open} disabled={blocked} aria-label={L.deleteBook}>
            <IconTrash />
          </ActionIcon>,
        )}
      </Box>
      <ConfirmModal
        opened={opened}
        onClose={close}
        onConfirm={confirm}
        title={L.deleteBookTitle}
        message={L.deleteBookMessage(book.title)}
        confirmLabel={L.deleteBook}
        danger
        loading={remove.isPending}
      />
    </>
  );
}
