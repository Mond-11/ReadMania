import { AppShell, Container } from '@mantine/core';
import { Outlet } from 'react-router';
import { COMMON } from '../shared/labels.js';
import { Header } from './Header.jsx';

export function Layout() {
  return (
    <AppShell header={{ height: 64 }} padding={0}>
      <a className="rm-skip-link" href="#main-content">
        {COMMON.skipToContent}
      </a>
      <AppShell.Header className="rm-header">
        <Header />
      </AppShell.Header>
      <AppShell.Main id="main-content">
        <Container size="xl" py="lg">
          <Outlet />
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}
