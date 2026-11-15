// Readable book URLs: "/books/{id}-{slug}". The id is the real key (parsed back
// from the leading digits); the slug is cosmetic, built from title+publisher+year
// so two editions of the same title still read differently.
function slugify(value) {
  return String(value ?? '')
    .toLowerCase()
    .trim()
    .replace(/['"«»]/g, '')
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '');
}

export function bookHref(book) {
  const slug = slugify([book.title, book.publisher, book.publicationYear].filter(Boolean).join('-'));
  return `/books/${book.id}${slug ? `-${slug}` : ''}`;
}

export function parseBookId(param) {
  const id = Number(String(param ?? '').split('-')[0]);
  return Number.isInteger(id) && id > 0 ? id : null;
}
