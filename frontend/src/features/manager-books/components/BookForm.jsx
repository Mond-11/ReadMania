import { Autocomplete, FileInput, Image, NumberInput, Stack, TagsInput, Text, Textarea, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { applyFormErrors } from '../../../shared/api/index.js';
import { useGenres } from '../../../shared/hooks/useGenres.js';
import { MANAGER_BOOK as L } from '../../../shared/labels.js';
import { notifyError } from '../../../shared/notify.js';
import { ErrorState, FormActions, PageLoader, Section } from '../../../shared/ui/index.js';
import { useAuthors } from '../../catalog/index.js';

const COVER_TYPES = ['image/jpeg', 'image/png'];
const isBlank = (v) => v == null || String(v).trim() === '';
const isInt = (v) => v !== '' && Number.isInteger(Number(v));
const sameName = (a, b) => a.trim().toLocaleLowerCase('uk') === b.trim().toLocaleLowerCase('uk');

function toValues(book) {
  return {
    title: book?.title ?? '',
    authors: book?.authors?.map((a) => a.name) ?? [],
    genre: book?.genre?.name ?? '',
    publisher: book?.publisher ?? '',
    publicationYear: book?.publicationYear ?? '',
    isbn: book?.isbn ?? '',
    price: book?.price ?? '',
    stock: book?.stock ?? '',
    description: book?.description ?? '',
    cover: null,
  };
}

export function BookForm({ book, onSubmit, onCancel, loading }) {
  const genres = useGenres();
  const authors = useAuthors();
  const form = useForm({
    initialValues: toValues(book),
    validate: {
      title: (v) => (isBlank(v) || v.trim().length > 255 ? L.errors.title : null),
      authors: (v) => (v.length === 0 ? L.errors.authors : v.some((n) => n.trim().length > 255) ? L.errors.authorLength : null),
      genre: (v) => (isBlank(v) || v.trim().length > 100 ? L.errors.genre : null),
      publisher: (v) => (isBlank(v) || v.trim().length > 255 ? L.errors.publisher : null),
      publicationYear: (v) => (isInt(v) && Number(v) >= 1450 && Number(v) <= 2100 ? null : L.errors.publicationYear),
      isbn: (v) => (isBlank(v) || v.trim().length > 20 ? L.errors.isbn : null),
      price: (v) => (v !== '' && Number(v) > 0 ? null : L.errors.price),
      stock: (v) => (isInt(v) && Number(v) >= 0 ? null : L.errors.stock),
      description: (v) => (isBlank(v) ? L.errors.description : null),
      // SRS M1.1: all fields are required, the cover too; a book that already has a cover keeps it when no new file is chosen.
      cover: (v) => {
        if (!v) return book?.hasCover ? null : L.errors.coverRequired;
        return COVER_TYPES.includes(v.type) ? null : L.errors.cover;
      },
    },
  });

  if (genres.isPending || authors.isPending) return <PageLoader minHeight={160} />;
  if (genres.isError) return <ErrorState error={genres.error} onRetry={genres.refetch} />;
  if (authors.isError) return <ErrorState error={authors.error} onRetry={authors.refetch} />;

  const genreNames = genres.data.map((g) => g.name);
  const authorNames = authors.data.map((a) => a.name);
  const typedGenre = form.values.genre.trim();
  const newGenre = typedGenre && !genreNames.some((n) => sameName(n, typedGenre)) ? typedGenre : null;
  const newAuthors = form.values.authors.filter((n) => !authorNames.some((x) => sameName(x, n)));

  const submit = form.onSubmit(async (values) => {
    try {
      await onSubmit({ values, coverFile: values.cover });
    } catch (error) {
      if (error?.status === 409) form.setFieldError('isbn', L.duplicateIsbn);
      else if (!applyFormErrors(form, error)) notifyError(error);
    }
  });

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        <Section title={L.mainSection}>
          <Stack gap="sm">
            <TextInput label={L.title} withAsterisk maxLength={255} {...form.getInputProps('title')} />
            <TagsInput
              label={L.authors}
              placeholder={L.authorsPlaceholder}
              description={L.authorsHint}
              data={authorNames}
              splitChars={[]}
              withAsterisk
              error={form.errors.authors}
              value={form.values.authors}
              onChange={(v) => form.setFieldValue('authors', v)}
            />
            {newAuthors.length > 0 && <Text size="sm" c="dimmed">{L.authorsNew(newAuthors)}</Text>}
            <Autocomplete
              label={L.genre}
              placeholder={L.genrePlaceholder}
              description={L.genreHint}
              data={genreNames}
              maxLength={100}
              withAsterisk
              {...form.getInputProps('genre')}
            />
            {newGenre && <Text size="sm" c="dimmed">{L.genreNew(newGenre)}</Text>}
            <TextInput label={L.publisher} withAsterisk maxLength={255} {...form.getInputProps('publisher')} />
            <NumberInput
              label={L.publicationYear}
              min={1450}
              max={2100}
              allowDecimal={false}
              allowNegative={false}
              withAsterisk
              {...form.getInputProps('publicationYear')}
            />
            <TextInput
              label={L.isbn}
              placeholder={L.isbnPlaceholder}
              maxLength={20}
              withAsterisk
              {...form.getInputProps('isbn')}
            />
            <Textarea label={L.description} autosize minRows={4} withAsterisk {...form.getInputProps('description')} />
          </Stack>
        </Section>

        <Section title={L.salesSection}>
          <Stack gap="sm">
            <NumberInput
              label={L.price}
              min={0}
              decimalScale={2}
              allowNegative={false}
              withAsterisk
              {...form.getInputProps('price')}
            />
            <NumberInput
              label={L.stock}
              min={0}
              allowDecimal={false}
              allowNegative={false}
              withAsterisk
              {...form.getInputProps('stock')}
            />
          </Stack>
        </Section>

        <Section title={L.coverSection}>
          <Stack gap="sm">
            {book?.hasCover && book.coverUrl && (
              <div>
                <Text size="sm" c="dimmed" mb={4}>{L.coverCurrent}</Text>
                <Image src={book.coverUrl} alt={book.title} w={120} fit="cover" radius="sm" />
              </div>
            )}
            <FileInput
              label={L.cover}
              description={book?.hasCover ? `${L.coverHint} ${L.coverReplaceHint}` : L.coverHint}
              accept={COVER_TYPES.join(',')}
              clearable
              required={!book?.hasCover}
              {...form.getInputProps('cover')}
            />
          </Stack>
        </Section>

        <FormActions submitLabel={book ? L.submitEdit : L.submitCreate} onCancel={onCancel} loading={loading} />
      </Stack>
    </form>
  );
}
