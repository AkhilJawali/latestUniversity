import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import AppShell from '@/components/layout/AppShell';

// A4-335 AC#2 (shell renders) + shell navigation entries.
describe('AppShell', () => {
  const renderShell = (children) =>
    render(
      <MemoryRouter>
        <AppShell>{children}</AppShell>
      </MemoryRouter>
    );

  it('renders header, primary nav, and main content (AC#2)', () => {
    renderShell(<p>content</p>);
    expect(screen.getByText('UTMS')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument();
    expect(screen.getByText('content')).toBeInTheDocument();
  });

  it('exposes navigation entries for the scheduling-engine sections', () => {
    renderShell(<p>content</p>);
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Engine Configuration' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Timetable Generation' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Approvals' })).toHaveAttribute('href', '/scheduling/approvals');
  });
});
