import { Button, Stack, Text, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { Link, Navigate, Outlet, useLocation, useMatches } from 'react-router';
import { ACCESS, PAGES, TERMS } from '../../../shared/labels.js';
import { PageLoader } from '../../../shared/ui/index.js';
import { useCurrentUser } from '../api.js';
import { AUTH_PATHS, homeFor, safeReturnPath } from '../paths.js';

// Access is declared on routes (U1):
//   handle: { roles: ['USER'] }   only these roles; a Гість is sent to the login page and returns after login
//   handle: { guestOnly: true }   login and registration pages; a logged-in user is sent on
// The deepest route that declares access wins. Routes without it are public and never wait for /api/auth/me.
function readAccess(matches) {
  let access = {};
  for (const m of matches) {
    if (m.handle?.roles || m.handle?.guestOnly) access = { roles: m.handle.roles, guestOnly: Boolean(m.handle.guestOnly) };
  }
  return access;
}

export function ProtectedRoute() {
  const matches = useMatches();
  const location = useLocation();
  const { user, isLoading } = useCurrentUser();
  const { roles, guestOnly } = readAccess(matches);

  if (!roles && !guestOnly) return <Outlet />;
  if (isLoading) return <PageLoader />;

  if (guestOnly) {
    if (!user) return <Outlet />;
    return <Navigate to={safeReturnPath(location.state?.from) ?? homeFor(user)} replace />;
  }

  if (!user) {
    return <Navigate to={AUTH_PATHS.login} replace state={{ from: location.pathname + location.search }} />;
  }
  if (!roles.includes(user.role)) return <Forbidden />;
  return <Outlet />;
}

function Forbidden() {
  useDocumentTitle(`${ACCESS.forbiddenTitle} — ${TERMS.appName}`);
  return (
    <Stack align="center" gap="sm" py={64} ta="center">
      <Text fz={64} fw={800} c="dimmed" lh={1} aria-hidden>403</Text>
      <Title order={1}>{ACCESS.forbiddenTitle}</Title>
      <Text c="dimmed">{ACCESS.forbiddenText}</Text>
      <Button component={Link} to="/" mt="sm">
        {PAGES.goHome}
      </Button>
    </Stack>
  );
}
