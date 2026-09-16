import { fireEvent, render, screen } from '@testing-library/react';
import { beforeAll, describe, expect, it, vi } from 'vitest';

import { derivationRuleSchema } from '../schemas/config-schemas';
import ConfigFormModal from './ConfigFormModal';

// jsdom does not implement <dialog>.showModal()/close(); stub them so the
// component's open/close effect can run.
beforeAll(() => {
  HTMLDialogElement.prototype.showModal = vi.fn(function showModal() {
    this.open = true;
  });
  HTMLDialogElement.prototype.close = vi.fn(function close() {
    this.open = false;
  });
});

const fields = [
  {
    name: 'componentType',
    label: 'Component Type',
    type: 'select',
    required: true,
    options: ['LECTURE', 'TUTORIAL', 'PRACTICAL'],
  },
  { name: 'slotDurationMinutes', label: 'Slot Duration (min)', type: 'number', required: true },
  { name: 'hoursPerSession', label: 'Hours per Session', type: 'number', required: true },
  { name: 'description', label: 'Description', type: 'text' },
];

const emptyValues = {
  componentType: '',
  slotDurationMinutes: '',
  hoursPerSession: '',
  description: '',
};

function renderModal(overrides = {}) {
  const props = {
    open: true,
    title: 'Add Derivation Rule',
    fields,
    schema: derivationRuleSchema,
    initialValues: emptyValues,
    onSubmit: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  };
  return { props, ...render(<ConfigFormModal {...props} />) };
}

describe('ConfigFormModal', () => {
  // A4-340 AC#2 — Add: submit a valid form -> onSubmit called with parsed data.
  it('submits parsed data when the form is valid (Add)', () => {
    const onSubmit = vi.fn();
    renderModal({ onSubmit });

    fireEvent.change(screen.getByLabelText(/Component Type/), {
      target: { value: 'LECTURE' },
    });
    fireEvent.change(screen.getByLabelText(/Slot Duration/), { target: { value: '60' } });
    fireEvent.change(screen.getByLabelText(/Hours per Session/), { target: { value: '1' } });

    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const [payload] = onSubmit.mock.calls[0];
    expect(payload.componentType).toBe('LECTURE');
    expect(payload.slotDurationMinutes).toBe(60); // coerced to number by the schema
    expect(payload.hoursPerSession).toBe(1);
  });

  // A4-340 AC#3 — Edit: the modal is pre-filled from initialValues.
  it('pre-fills the form from initialValues (Edit)', () => {
    renderModal({
      title: 'Edit Derivation Rule',
      initialValues: {
        componentType: 'TUTORIAL',
        slotDurationMinutes: '90',
        hoursPerSession: '1.5',
        description: 'Tut',
      },
    });

    expect(screen.getByLabelText(/Component Type/)).toHaveValue('TUTORIAL');
    expect(screen.getByLabelText(/Slot Duration/)).toHaveValue(90);
    expect(screen.getByLabelText(/Hours per Session/)).toHaveValue(1.5);
  });

  // A4-340 AC#5 — invalid input: inline errors shown, NO request sent.
  it('shows inline errors and does not submit when invalid', () => {
    const onSubmit = vi.fn();
    renderModal({ onSubmit }); // all fields empty

    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSubmit).not.toHaveBeenCalled();
    // The required component type is invalid; its field error is rendered.
    const select = screen.getByLabelText(/Component Type/);
    expect(select).toHaveAttribute('aria-invalid', 'true');
  });

  it('closes without submitting when Cancel is clicked', () => {
    const onSubmit = vi.fn();
    const onClose = vi.fn();
    renderModal({ onSubmit, onClose });

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
