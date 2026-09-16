import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PropTypes from 'prop-types';
import { useState } from 'react';

// Central TanStack Query provider. Defaults tuned per state-management steering:
// master data is cached briefly; feature hooks override staleTime as needed.
export default function AppProviders({ children }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

AppProviders.propTypes = {
  children: PropTypes.node,
};
