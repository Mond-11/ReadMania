import { Center, Loader } from '@mantine/core';
import { COMMON } from '../labels.js';

export function PageLoader({ minHeight = 240 }) {
  return (
    <Center mih={minHeight} role="status" aria-live="polite">
      <Loader color="brand" aria-label={COMMON.loading} />
    </Center>
  );
}
