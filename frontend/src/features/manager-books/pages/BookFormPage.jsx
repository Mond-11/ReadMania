import { Button } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, useNavigate, useParams } from 'react-router';
import { CATALOG, MANAGER_BOOK as L, TERMS } from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { EmptyState, ErrorState, PageHeader, PageLoader } from '../../../shared/ui/index.js';
import { bookHref, useBook } from '../../catalog/index.js';
import { useSaveBook } from '../api.js';
import { BookForm } from '../components/BookForm.jsx';

const parseId = (param) => {
  const id = Number(param);
  return Number.isInteger(id) && id > 0 ? id : null;
};

export default function BookFormPage() {
  const { id: idParam } = useParams();
  const isEdit = idParam != null;
  const id = isEdit ? parseId(idParam) : null;
  const navigate = useNavigate();
  const book = useBook(id);
  const save = useSaveBook(id);

  const title = isEdit ? L.editTitle : L.createTitle;
  useDocumentTitle(`${title} — ${TERMS.appName}`);

  const breadcrumbs = [
    { label: CATALOG.title, to: '/' },
    ...(isEdit && book.data ? [{ label: book.data.title, to: bookHref(book.data) }] : []),
    { label: title },
  ];
  const header = <PageHeader title={title} description={isEdit ? undefined : L.createDescription} breadcrumbs={breadcrumbs} />;

  if (isEdit && (id == null || book.error?.status === 404)) {
    return (
      <>
        {header}
        <EmptyState
          title={CATALOG.notFoundTitle}
          hint={CATALOG.notFoundHint}
          action={<Button component={Link} to="/" variant="default">{CATALOG.backToCatalog}</Button>}
        />
      </>
    );
  }
  if (isEdit && book.isPending) return <>{header}<PageLoader /></>;
  if (isEdit && book.isError) return <>{header}<ErrorState error={book.error} onRetry={book.refetch} /></>;

  const onSubmit = async (payload) => {
    const { book: saved, coverFailed } = await save.mutateAsync(payload);
    if (coverFailed) {
      notifyError(L.coverFailed, { raw: true });
      // A new book already exists: continue in the edit form so the cover can be retried without a duplicate.
      navigate(`/manager/books/${saved.id}/edit`, { replace: true });
      return;
    }
    notifySuccess(isEdit ? L.updated : L.created);
    navigate(bookHref(saved));
  };

  return (
    <>
      {header}
      <BookForm
        key={book.data?.version ?? 'new'}
        book={book.data}
        onSubmit={onSubmit}
        onCancel={() => navigate(-1)}
        loading={save.isPending}
      />
    </>
  );
}
