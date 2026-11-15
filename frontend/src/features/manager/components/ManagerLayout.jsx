import { Grid, NavLink, Paper, Select, Stack } from '@mantine/core';
import { Link, Outlet, useLocation, useNavigate } from 'react-router';
import { CATALOG, MANAGER } from '../../../shared/labels.js';
import { PageHeader } from '../../../shared/ui/index.js';
import { isSectionReady, MANAGER_SECTIONS } from '../sections.js';

const BASE = '/manager';

// Shell of «Панель менеджера»: menu of sections on the left (a select on narrow screens), the section on the right.
export default function ManagerLayout() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const active = MANAGER_SECTIONS.find((s) => pathname.startsWith(`${BASE}/${s.path}`));

  return (
    <>
      <PageHeader
        title={MANAGER.title}
        breadcrumbs={[
          { label: CATALOG.title, to: '/' },
          { label: MANAGER.title, to: BASE },
          ...(active ? [{ label: active.label }] : []),
        ]}
      />
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 3 }}>
          <Paper p="xs" component="nav" aria-label={MANAGER.sections} visibleFrom="md">
            <Stack gap={2}>
              {MANAGER_SECTIONS.map((s) =>
                isSectionReady(s.path) ? (
                  <NavLink
                    key={s.path}
                    component={Link}
                    to={`${BASE}/${s.path}`}
                    label={s.label}
                    active={active?.path === s.path}
                    aria-current={active?.path === s.path ? 'page' : undefined}
                    color="brand"
                    variant="light"
                  />
                ) : (
                  <NavLink
                    key={s.path}
                    component="span"
                    label={s.label}
                    description={MANAGER.inProgress}
                    disabled
                    aria-disabled="true"
                  />
                ),
              )}
            </Stack>
          </Paper>
          <Select
            hiddenFrom="md"
            label={MANAGER.section}
            data={MANAGER_SECTIONS.map((s) => ({
              value: s.path,
              label: isSectionReady(s.path) ? s.label : `${s.label} — ${MANAGER.inProgress.toLowerCase()}`,
              disabled: !isSectionReady(s.path),
            }))}
            value={active?.path ?? null}
            onChange={(v) => v && navigate(`${BASE}/${v}`)}
            allowDeselect={false}
            mb="sm"
          />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 9 }}>
          <Outlet />
        </Grid.Col>
      </Grid>
    </>
  );
}
