import { Container, Group } from '@mantine/core';
import { Link } from 'react-router';
import { TERMS } from '../shared/labels.js';

export function Header() {
  return (
    <Container size="xl" h="100%">
      <Group h="100%" justify="space-between" wrap="nowrap">
        <Link to="/" className="rm-logo">
          {TERMS.appName}
        </Link>
        <Group gap="sm" wrap="nowrap">
          {/* Person B: navigation, cart, account menu */}
        </Group>
      </Group>
    </Container>
  );
}
