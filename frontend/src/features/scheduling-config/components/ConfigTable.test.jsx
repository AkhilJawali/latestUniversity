import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import ConfigTable from './ConfigTable';

// A4-340 section 5.2 — ConfigTable renders loading / error / empty / populated states.
const columns = [
  { key: 'componentType', header: 'Component Type' },
  { key: 'slotDurationMinutes', header: 'Slot Duration' },
];

const noop = () => {};

function renderTable(overrides = {}) {
  const props = {
    entityLabel: 'Derivation Rules',
    addLabel: 'Add rule',
    columns,
    rows: [],
    onAdd: noop,
    onEdit: noop,
    onDelete: noop,
    onRetry: noop,
    ...overrides,
  };
  return render(<ConfigTable {...props} />);
}

describe('ConfigTable', () => {
  it('shows an error state with a Retry button', () => {
    const onRetry = vi.fn();
    renderTable({ isError: true, onRetry });
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('shows an empty state with an Add call to action', () => {
    renderTable({ rows: [] });
    expect(screen.getByText(/no derivation rules yet/i)).toBeInTheDocument();
  });

  it('renders rows with Edit and Delete actions', () => {
    renderTable({
      rows: [{ id: 7, componentType: 'LECTURE', slotDurationMinutes: 60 }],
    });
    expect(screen.getByText('LECTURE')).toBeInTheDocument();
    expect(screen.getByText('60')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Edit Derivation Rules 7' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete Derivation Rules 7' })).toBeInTheDocument();
  });

  it('marks the table busy while loading', () => {
    renderTable({ isLoading: true });
    expect(screen.getByRole('table')).toHaveAttribute('aria-busy', 'true');
  });
});
