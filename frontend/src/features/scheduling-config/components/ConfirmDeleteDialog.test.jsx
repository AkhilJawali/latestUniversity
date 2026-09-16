import { fireEvent, render, screen } from '@testing-library/react';
import { beforeAll, describe, expect, it, vi } from 'vitest';

import ConfirmDeleteDialog from './ConfirmDeleteDialog';

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

// A4-340 AC#4 — Delete shows a confirmation dialog; on confirm the record is
// removed, on cancel nothing happens.
describe('ConfirmDeleteDialog', () => {
  it('confirms the delete when Delete is clicked', () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();

    render(
      <ConfirmDeleteDialog open label="rule 7" onConfirm={onConfirm} onCancel={onCancel} />,
    );

    expect(screen.getByText(/are you sure you want to delete/i)).toHaveTextContent('rule 7');
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onCancel).not.toHaveBeenCalled();
  });

  it('cancels without confirming when Cancel is clicked', () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();

    render(
      <ConfirmDeleteDialog open label="weight 3" onConfirm={onConfirm} onCancel={onCancel} />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('disables both actions while a delete is pending', () => {
    render(
      <ConfirmDeleteDialog
        open
        isPending
        label="slot CCC"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  });
});
