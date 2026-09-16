import { RouterProvider } from 'react-router-dom';

import ErrorBoundary from '@/components/feedback/ErrorBoundary';
import AppProviders from '@/app/providers';
import { router } from '@/app/router';

export default function App() {
  return (
    <ErrorBoundary>
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>
    </ErrorBoundary>
  );
}
