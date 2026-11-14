import { MantineProvider } from '@mantine/core';
import { DatesProvider } from '@mantine/dates';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider } from '@tanstack/react-query';
import 'dayjs/locale/uk';
import { queryClient } from '../shared/api/index.js';
import { cssVariablesResolver, theme } from '../shared/theme.js';

export function Providers({ children }) {
  return (
    <MantineProvider theme={theme} cssVariablesResolver={cssVariablesResolver} defaultColorScheme="light" forceColorScheme="light">
      <DatesProvider settings={{ locale: 'uk', firstDayOfWeek: 1 }}>
        <QueryClientProvider client={queryClient}>
          <Notifications position="top-right" limit={4} />
          {children}
        </QueryClientProvider>
      </DatesProvider>
    </MantineProvider>
  );
}
