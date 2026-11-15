# shared/ui: the UI-kit

Use these instead of rebuilding the same block. Anything not listed: use Mantine directly; the theme (`../theme.js`) already sets the palette, radius, bordered `Paper`/`Card`, and defaults for modals, tooltips, action icons and tables. Do not set hex colors in components. All texts come from `../labels.js`.

| Component | Use | Props |
|---|---|---|
| `PageLoader` | page is loading | `minHeight` |
| `ErrorState` | query failed | `error`, `onRetry` |
| `EmptyState` | nothing to show | `title`, `hint`, `action` |
| `PageHeader` | top of every page (the only `<h1>`) | `title`, `description`, `breadcrumbs=[{label,to}]`, `actions` |
| `Section` | content block (white bordered card, `<h2>`) | `title`, `actions`, any `Paper` prop |
| `ConfirmModal` | destructive or irreversible action | `opened`, `onClose`, `onConfirm`, `title`, `message`, `confirmLabel`, `danger`, `loading` |
| `StatusBadge` | status, role, order type | `map` (from labels), `value`, `tone` (`success`/`danger`/`info`/`neutral`) |
| `Money` | price | `value` + any `Text` prop |
| `FormActions` | bottom of a form | `submitLabel`, `onCancel`, `loading`, `disabled`, `disabledReason` |
| `IconTrash` | icon for a delete/remove `ActionIcon` | any `svg` prop |

## Page skeleton
```jsx
import { PageHeader, Section, PageLoader, ErrorState, EmptyState } from '../../../shared/ui/index.js';

<>
  <PageHeader title="Кошик" breadcrumbs={[{ label: 'Каталог', to: '/' }, { label: 'Кошик' }]} />
  {isLoading ? <PageLoader /> : error ? <ErrorState error={error} onRetry={refetch} /> :
   !data.length ? <EmptyState title="…" hint="…" /> :
   <Section title="Позиції">…</Section>}
</>
```

## Rules
- Button text names the action («Додати до кошика»); the same action has the same name everywhere. Primary action: default `Button`; secondary: `variant="default"`; destructive: `color="red"`.
- Icon-only buttons need `aria-label`. Status is never shown by color alone: `StatusBadge` always has text.
- Forms: `@mantine/form`, every field has `label`, buttons via `FormActions`.
- Missing something? Add it here and tell the other developer instead of making a local copy.
