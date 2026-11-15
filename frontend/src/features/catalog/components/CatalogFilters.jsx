import { Button, Checkbox, Flex, Group, NumberInput, Select, SimpleGrid, Stack, TextInput } from '@mantine/core';
import { useDebouncedCallback, useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { BOOK_SORT, CATALOG, toOptions } from '../../../shared/labels.js';
import { useGenres } from '../../../shared/hooks/useGenres.js';
import { useAuthors } from '../api.js';
import { hasActiveFilters, isRangeInvalid } from '../filters.js';

const DEBOUNCE_MS = 400;
const SORT_OPTIONS = toOptions(BOOK_SORT);

// Local text while typing, committed to the URL after a pause. Follows the URL on Back/Forward.
function useDraft(value, commit) {
  const [draft, setDraft] = useState(value);
  const [seen, setSeen] = useState(value);
  if (seen !== value) {
    setSeen(value);
    setDraft(value);
  }
  const debounced = useDebouncedCallback(commit, DEBOUNCE_MS);
  return [
    draft,
    (next) => {
      setDraft(next);
      debounced(next);
    },
    debounced.flush,
  ];
}

function DraftNumber({ label, value, onCommit, error, ...props }) {
  const [draft, setDraft, flush] = useDraft(value, onCommit);
  return (
    <NumberInput
      label={label}
      value={draft}
      onChange={(v) => setDraft(v === '' ? '' : String(v))}
      onBlur={flush}
      error={error}
      min={0}
      hideControls
      allowNegative={false}
      thousandSeparator=" "
      {...props}
    />
  );
}

// vertical: sidebar layout, one narrow column, everything visible.
// Otherwise (above the list): search/sort row plus one row of main fields; stock filters sit under a toggle.
export function CatalogFilters({ filters, onChange, onReset, vertical = false }) {
  const genres = useGenres();
  const authors = useAuthors();
  const isWide = useMediaQuery('(min-width: 62em)');
  const stockActive = filters.stockMin !== '' || filters.stockMax !== '' || filters.inStock;
  const [open, setOpen] = useState(stockActive);
  const [searchValue, setSearchValue] = useDraft(filters.q, (q) => onChange({ q }));

  const priceError = isRangeInvalid(filters.priceMin, filters.priceMax) ? CATALOG.priceRangeError : null;
  const stockError = isRangeInvalid(filters.stockMin, filters.stockMax) ? CATALOG.stockRangeError : null;

  const genreOptions = (genres.data ?? []).map((g) => ({ value: String(g.id), label: g.name }));
  const authorOptions = (authors.data ?? []).map((a) => ({ value: String(a.id), label: a.name }));
  const showMain = vertical || isWide || open;
  const showMore = vertical || open;
  const full = vertical ? { gridColumn: '1 / -1' } : undefined;
  const cols = vertical ? 2 : { base: 1, sm: 2, md: 4 };
  const resetButton = (
    <Button variant="default" onClick={onReset} disabled={!hasActiveFilters(filters)}>
      {CATALOG.resetFilters}
    </Button>
  );

  return (
    <Stack gap="sm">
      <Flex direction={vertical ? 'column' : 'row'} align={vertical ? 'stretch' : 'flex-end'} wrap="wrap" gap="sm">
        <TextInput
          label={CATALOG.search}
          placeholder={CATALOG.searchPlaceholder}
          value={searchValue}
          onChange={(e) => setSearchValue(e.currentTarget.value)}
          type="search"
          style={vertical ? undefined : { flex: '1 1 260px' }}
        />
        <Select
          label={CATALOG.sort}
          data={SORT_OPTIONS}
          value={filters.sort}
          onChange={(sort) => sort && onChange({ sort })}
          allowDeselect={false}
          style={vertical ? undefined : { flex: '1 1 240px' }}
        />
      </Flex>

      <SimpleGrid cols={cols} spacing="sm" style={showMain ? undefined : { display: 'none' }}>
        <Select
          label={CATALOG.genre}
          placeholder={CATALOG.genrePlaceholder}
          data={genreOptions}
          style={full}
          value={filters.genreId || null}
          onChange={(v) => onChange({ genreId: v ?? '' })}
          clearable
          searchable
        />
        <Select
          label={CATALOG.author}
          placeholder={CATALOG.authorPlaceholder}
          data={authorOptions}
          style={full}
          value={filters.authorId || null}
          onChange={(v) => onChange({ authorId: v ?? '' })}
          clearable
          searchable
        />
        <DraftNumber
          label={CATALOG.priceMin}
          value={filters.priceMin}
          onCommit={(v) => onChange({ priceMin: v })}
          decimalScale={2}
          error={priceError ? ' ' : null}
        />
        <DraftNumber
          label={CATALOG.priceMax}
          value={filters.priceMax}
          onCommit={(v) => onChange({ priceMax: v })}
          decimalScale={2}
          error={priceError}
        />
      </SimpleGrid>

      <SimpleGrid id="catalog-filter-more" cols={cols} spacing="sm" style={showMore ? undefined : { display: 'none' }}>
        <DraftNumber
          label={CATALOG.stockMin}
          value={filters.stockMin}
          onCommit={(v) => onChange({ stockMin: v })}
          allowDecimal={false}
          error={stockError ? ' ' : null}
        />
        <DraftNumber
          label={CATALOG.stockMax}
          value={filters.stockMax}
          onCommit={(v) => onChange({ stockMax: v })}
          allowDecimal={false}
          error={stockError}
        />
        <Checkbox
          label={CATALOG.inStock}
          checked={filters.inStock}
          onChange={(e) => onChange({ inStock: e.currentTarget.checked })}
          style={{ ...full, alignSelf: 'end', paddingBottom: 8 }}
        />
      </SimpleGrid>
      {vertical ? (
        resetButton
      ) : (
        <Group gap="sm">
          <Button
            variant="default"
            onClick={() => setOpen((v) => !v)}
            aria-expanded={open}
            aria-controls="catalog-filter-more"
          >
            {open ? CATALOG.hideFilters : CATALOG.showFilters}
          </Button>
          {resetButton}
        </Group>
      )}
    </Stack>
  );
}
