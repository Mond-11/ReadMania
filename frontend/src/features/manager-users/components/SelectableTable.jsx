import { Checkbox, Table } from '@mantine/core';

// Table with a checkbox per row and «select all» in the header (M5 and M7 work on marked rows).
// columns: [{ key, header, render(row), ...TdProps }]
export function SelectableTable({ rows, columns, selected, onChange, selectAllLabel, selectRowLabel, minWidth = 640 }) {
  const ids = rows.map((r) => r.id);
  const all = ids.length > 0 && ids.every((id) => selected.includes(id));
  const some = ids.some((id) => selected.includes(id));

  const toggle = (id, checked) => onChange(checked ? [...selected, id] : selected.filter((x) => x !== id));

  return (
    <Table.ScrollContainer minWidth={minWidth}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th w={40}>
              <Checkbox
                aria-label={selectAllLabel}
                checked={all}
                indeterminate={some && !all}
                onChange={(e) => onChange(e.currentTarget.checked ? ids : [])}
              />
            </Table.Th>
            {columns.map(({ key, header, render: _render, ...props }) => (
              <Table.Th key={key} {...props}>
                {header}
              </Table.Th>
            ))}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {rows.map((row) => (
            <Table.Tr key={row.id} bg={selected.includes(row.id) ? 'var(--mantine-color-brand-0)' : undefined}>
              <Table.Td>
                <Checkbox
                  aria-label={selectRowLabel(row)}
                  checked={selected.includes(row.id)}
                  onChange={(e) => toggle(row.id, e.currentTarget.checked)}
                />
              </Table.Td>
              {columns.map(({ key, header: _header, render, ...props }) => (
                <Table.Td key={key} {...props}>
                  {render(row)}
                </Table.Td>
              ))}
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
