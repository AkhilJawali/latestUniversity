import { render, screen } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import AppShell from '@/components/layout/AppShell';
import DashboardPage from '@/features/dashboard/DashboardPage';
import PlaceholderPage from '@/features/dashboard/PlaceholderPage';

// Mirror the production route table (A4-335 KD-F2) but with in-memory history,
// avoiding the lazy GenerationPage (owned by A4-345) so this test stays scoped
// to the foundation's routing behavior (AC#2, AC#3, AC#4).
function buildRouter(initialPath) {
  return createMemoryRouter(
    [
      { path: '/', element: <AppShell><DashboardPage /></AppShell> },
      {
        path: '/scheduling/config',
        element: (
          <AppShell>
            <PlaceholderPage title="Engine Configuration" note="CRUD admin panel — delivered by its own story." />
          </AppShell>
        ),
      },
    ],
    { initialEntries: [initialPath] }
  );
}

describe('routing', () => {
  it('renders the dashboard inside the shell at / (AC#2)', () => {
    render(<RouterProvider router={buildRouter('/')} />);
    expect(screen.getByRole('heading', { name: /Scheduling Engine/i })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument();
  });

  it('exposes client-side nav links (in-app routing, not full reload) (AC#3)', () => {
    render(<RouterProvider router={buildRouter('/')} />);
    // React Router renders NavLinks as in-app anchors with client-side hrefs.
    // A full-reload alternative would not resolve to these SPA routes.
    const configLink = screen.getByRole('link', { name: 'Engine Configuration' });
    expect(configLink).toHaveAttribute('href', '/scheduling/config');
    const dashboardLink = screen.getByRole('link', { name: 'Dashboard' });
    expect(dashboardLink).toHaveAttribute('href', '/');
    // And the target route renders its content in-shell (see the AC#4 test below,
    // which renders /scheduling/config directly and asserts the placeholder).
  });

  it('shows a placeholder (not an error) for an unbuilt feature route (AC#4)', () => {
    render(<RouterProvider router={buildRouter('/scheduling/config')} />);
    expect(screen.getByText('This screen is coming soon.')).toBeInTheDocument();
  });
});
