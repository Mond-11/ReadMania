import { Badge } from '@mantine/core';
import { labelOf } from '../labels.js';

// Tone is decoration only: the text label always carries the meaning (never color alone).
const TONE_STYLE = {
  success: { color: 'brand', variant: 'light' },
  danger: { color: 'red', variant: 'light' },
  info: { variant: 'filled', style: { backgroundColor: 'var(--rm-accent-bg)', color: 'var(--mantine-color-black)' } },
  neutral: { color: 'gray', variant: 'default' },
};

// Default tone per API enum value; override with the `tone` prop when a screen needs something else.
const VALUE_TONE = {
  ACTIVE: 'success',
  PAID: 'success',
  DELIVERED: 'success',
  COMPLETED: 'success',
  AWAITING_PAYMENT: 'info',
  GIFT: 'info',
  BLOCKED: 'danger',
};

export function StatusBadge({ map, value, tone, ...props }) {
  const style = TONE_STYLE[tone ?? VALUE_TONE[value] ?? 'neutral'];
  return (
    <Badge radius="sm" styles={{ root: { textTransform: 'none' } }} {...style} {...props}>
      {labelOf(map, value)}
    </Badge>
  );
}
