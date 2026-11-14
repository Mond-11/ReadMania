import { Text } from '@mantine/core';
import { formatMoney } from '../format.js';

export function Money({ value, ...props }) {
  return (
    <Text component="span" style={{ whiteSpace: 'nowrap' }} {...props}>
      {formatMoney(value)}
    </Text>
  );
}
