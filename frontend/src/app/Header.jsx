import { Badge, Burger, Button, Container, Drawer, Group, Stack, Text, UnstyledButton, VisuallyHidden } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconBooks, IconGift, IconLayoutDashboard, IconUserCircle } from '@tabler/icons-react';
import { Link, useLocation, useNavigate } from 'react-router';
import { AUTH_PATHS, useCurrentUser, useLogout } from '../features/auth/index.js';
import { useCartCount } from '../features/cart/index.js';
import { NAV, TERMS } from '../shared/labels.js';
import { notifySuccess } from '../shared/notify.js';

// U8: «Довідка» opens the PDF guide for the role (files in public/help). The manager guide is not linked for
// Гість or Користувач; the IT specialist guide stays in docs/guides and is not published on the site.
const HELP_URLS = {
  user: '/help/readmania-user-guide.pdf',
  manager: '/help/readmania-manager-guide.pdf',
};

// First-level pages by role (U1, UX lab 3): the header gives direct access to them from anywhere.
// '/gift-set' and '/cart' are person A's pages; '/account' and '/manager' are person B's.
function navLinks({ isUser, isManager }) {
  return [
    { to: '/', label: NAV.catalog, icon: IconBooks, match: (p) => p === '/' || p.startsWith('/books/') },
    isUser && { to: '/gift-set', label: NAV.giftSet, icon: IconGift },
    isUser && { to: '/account', label: NAV.account, icon: IconUserCircle },
    isManager && { to: '/manager', label: NAV.managerPanel, icon: IconLayoutDashboard },
  ].filter(Boolean);
}

const isActive = (link, pathname) =>
  link.match ? link.match(pathname) : pathname === link.to || pathname.startsWith(`${link.to}/`);

export function Header() {
  const [opened, { toggle, close }] = useDisclosure(false);
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const session = useCurrentUser();
  const { isLoading, isGuest, isUser } = session;
  const cartCount = useCartCount(isUser);
  const logout = useLogout();

  const links = navLinks(session);

  // C10: after logout the catalog is shown in Гість mode. Navigate first so a protected page does not bounce to /login.
  const onLogout = () => {
    close();
    navigate('/');
    logout.mutate(undefined, { onSettled: () => notifySuccess(NAV.loggedOut) });
  };

  const cartLink = isUser && (
    <Link
      to="/cart"
      className="rm-nav-link"
      aria-current={isActive({ to: '/cart' }, pathname) ? 'page' : undefined}
      onClick={close}
    >
      {NAV.cart}
      {cartCount > 0 && (
        <>
          <Badge ml={6} size="sm" circle color="brand" variant="white" aria-hidden>
            {cartCount}
          </Badge>
          <VisuallyHidden>{`${NAV.cartWithCount} ${cartCount}`}</VisuallyHidden>
        </>
      )}
    </Link>
  );

  // Hidden until /api/auth/me answers, so a Менеджер never briefly gets the user guide.
  const helpUrl = isLoading ? null : session.isManager ? HELP_URLS.manager : HELP_URLS.user;
  const help = helpUrl && (
    <a href={helpUrl} target="_blank" rel="noreferrer" className="rm-nav-link" onClick={close}>
      {NAV.help}
      <VisuallyHidden>{`. ${NAV.helpOpensPdf}`}</VisuallyHidden>
    </a>
  );

  // Nothing is rendered for the account slot until /api/auth/me answers, so the header does not flicker.
  const accountAction = isLoading ? null : isGuest ? (
    <Button component={Link} to={AUTH_PATHS.login} state={{ from: pathname }} variant="white" onClick={close}>
      {NAV.login}
    </Button>
  ) : (
    <UnstyledButton className="rm-nav-link" onClick={onLogout} disabled={logout.isPending}>
      {NAV.logout}
    </UnstyledButton>
  );

  const renderLinks = (onClick) =>
    links.map((link) => (
      <Link
        key={link.to}
        to={link.to}
        className="rm-nav-link"
        aria-current={isActive(link, pathname) ? 'page' : undefined}
        onClick={onClick}
      >
        <link.icon size={18} stroke={1.75} aria-hidden />
        {link.label}
      </Link>
    ));

  return (
    <Container size="xl" h="100%">
      <Group h="100%" justify="space-between" wrap="nowrap" gap="md">
        <Link to="/" className="rm-logo">
          {TERMS.appName}
        </Link>

        <Group component="nav" aria-label={NAV.main} gap={4} wrap="nowrap" visibleFrom="md">
          {renderLinks()}
        </Group>

        <Group gap={4} wrap="nowrap" visibleFrom="md">
          {help}
          {cartLink}
          {accountAction}
        </Group>

        <Burger
          opened={opened}
          onClick={toggle}
          hiddenFrom="md"
          color="var(--rm-header-text)"
          aria-label={opened ? NAV.closeMenu : NAV.openMenu}
          aria-expanded={opened}
        />
      </Group>

      <Drawer
        opened={opened}
        onClose={close}
        position="right"
        size="xs"
        title={NAV.menu}
        hiddenFrom="md"
        classNames={{ content: 'rm-drawer', header: 'rm-drawer' }}
      >
        <Stack component="nav" aria-label={NAV.main} gap={4}>
          {renderLinks(close)}
          {cartLink}
          {help}
          {session.user && (
            <Text size="sm" c="var(--rm-header-text)" px="sm" pt="sm" style={{ overflowWrap: 'anywhere' }}>
              {session.user.email}
            </Text>
          )}
          {accountAction}
        </Stack>
      </Drawer>
    </Container>
  );
}
