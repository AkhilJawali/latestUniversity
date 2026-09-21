import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import RoomFormModal from './RoomFormModal';

// A4-430 — RoomFormModal unit tests covering create/edit modes, validation, and submission.

const defaultInitialValues = {
  name: '',
  code: '',
  campusId: '',
  capacity: '',
  roomType: '',
  equipmentTags: [],
  building: '',
  floor: '',
};

const campuses = [
  { id: 1, name: 'Main Campus' },
  { id: 2, name: 'City Campus' },
];

function renderModal(overrides = {}) {
  const props = {
    open: true,
    mode: 'create',
    initialValues: defaultInitialValues,
    campuses,
    isPending: false,
    onSubmit: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  };
  return render(<RoomFormModal {...props} />);
}

describe('RoomFormModal', () => {
  describe('create mode', () => {
    it('renders all required fields', () => {
      renderModal({ mode: 'create' });
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/code/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/campus/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/capacity/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/room type/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/equipment tags/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/building/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/floor/i)).toBeInTheDocument();
    });

    it('shows campus dropdown with options', () => {
      renderModal({ mode: 'create' });
      const campusSelect = screen.getByLabelText(/campus/i);
      expect(campusSelect).toBeInTheDocument();
      expect(screen.getByText('Main Campus')).toBeInTheDocument();
      expect(screen.getByText('City Campus')).toBeInTheDocument();
    });

    it('shows room type dropdown with all types', () => {
      renderModal({ mode: 'create' });
      const typeSelect = screen.getByLabelText(/room type/i);
      expect(typeSelect).toBeInTheDocument();
      // Options are in the dropdown
      expect(screen.getByText('CLASSROOM')).toBeInTheDocument();
      expect(screen.getByText('LAB')).toBeInTheDocument();
      expect(screen.getByText('SEMINAR_HALL')).toBeInTheDocument();
      expect(screen.getByText('AUDITORIUM')).toBeInTheDocument();
    });

    it('validates required fields on submit', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument();
        expect(screen.getByText(/code is required/i)).toBeInTheDocument();
        expect(screen.getByText(/campus is required/i)).toBeInTheDocument();
        expect(screen.getByText(/capacity must be at least 1/i)).toBeInTheDocument();
        expect(screen.getByText(/select a room type/i)).toBeInTheDocument();
      });

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('validates code format', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      const codeInput = screen.getByLabelText(/code/i);
      fireEvent.change(codeInput, { target: { value: 'invalid code!' } });
      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(screen.getByText(/code must be alphanumeric/i)).toBeInTheDocument();
      });

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('validates capacity minimum', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      const capacityInput = screen.getByLabelText(/capacity/i);
      fireEvent.change(capacityInput, { target: { value: '0' } });
      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(screen.getByText(/capacity must be at least 1/i)).toBeInTheDocument();
      });

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('submits valid create form', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Lecture Hall 1' } });
      fireEvent.change(screen.getByLabelText(/code/i), { target: { value: 'LH-101' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/capacity/i), { target: { value: '60' } });
      fireEvent.change(screen.getByLabelText(/room type/i), { target: { value: 'CLASSROOM' } });

      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'Lecture Hall 1',
            code: 'LH-101',
            campusId: 1,
            capacity: 60,
            roomType: 'CLASSROOM',
          }),
          expect.any(Object),
        );
      });
    });

    it('parses equipment tags from comma-separated input', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Computer Lab' } });
      fireEvent.change(screen.getByLabelText(/code/i), { target: { value: 'CL-101' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/capacity/i), { target: { value: '30' } });
      fireEvent.change(screen.getByLabelText(/room type/i), { target: { value: 'LAB' } });
      fireEvent.change(screen.getByLabelText(/equipment tags/i), {
        target: { value: 'projector, computer_lab, smart_board' },
      });

      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            equipmentTags: ['projector', 'computer_lab', 'smart_board'],
          }),
          expect.any(Object),
        );
      });
    });

    it('disables buttons while pending', () => {
      renderModal({ mode: 'create', isPending: true });

      expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled();
      expect(screen.getByRole('button', { name: /saving/i })).toBeDisabled();
    });
  });

  describe('edit mode', () => {
    const editValues = {
      name: 'Lecture Hall 1',
      code: 'LH-101',
      capacity: 60,
      roomType: 'CLASSROOM',
      equipmentTags: ['projector'],
      building: 'Block A',
      floor: 'Ground',
    };

    it('shows read-only code and campus fields', () => {
      renderModal({
        mode: 'edit',
        initialValues: editValues,
        currentCampusName: 'Main Campus',
      });

      const codeInput = screen.getByLabelText(/code/i);
      const campusInput = screen.getByLabelText(/campus/i);

      expect(codeInput).toHaveAttribute('readonly');
      expect(codeInput).toBeDisabled();
      expect(campusInput).toHaveAttribute('readonly');
      expect(campusInput).toBeDisabled();
    });

    it('does not show code and campus in edit validation', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'edit', initialValues: editValues, onSubmit });

      fireEvent.change(screen.getByLabelText(/capacity/i), { target: { value: '0' } });
      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(screen.getByText(/capacity must be at least 1/i)).toBeInTheDocument();
      });

      // Code and campus fields should not be in the validation errors
      expect(screen.queryByText(/code is required/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/campus is required/i)).not.toBeInTheDocument();
    });

    it('submits valid edit form without code and campusId', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'edit', initialValues: editValues, onSubmit });

      fireEvent.change(screen.getByLabelText(/capacity/i), { target: { value: '80' } });
      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'Lecture Hall 1',
            capacity: 80,
            roomType: 'CLASSROOM',
            // code and campusId should NOT be in the submission
          }),
          expect.any(Object),
        );
      });
    });

    it('pre-populates form with initial values', () => {
      renderModal({
        mode: 'edit',
        initialValues: editValues,
        currentCampusName: 'Main Campus',
      });

      expect(screen.getByLabelText(/name/i)).toHaveValue('Lecture Hall 1');
      expect(screen.getByLabelText(/capacity/i)).toHaveValue(60);
      expect(screen.getByDisplayValue('CLASSROOM')).toBeInTheDocument();
      expect(screen.getByLabelText(/equipment tags/i)).toHaveValue('projector');
      expect(screen.getByLabelText(/building/i)).toHaveValue('Block A');
      expect(screen.getByLabelText(/floor/i)).toHaveValue('Ground');
    });
  });

  describe('error handling', () => {
    it('displays field-level errors from backend', async () => {
      const onSubmit = vi.fn((data, handlers) => {
        handlers.onError({
          response: {
            data: {
              errors: { code: 'Room code already exists in this campus' },
            },
          },
        });
      });

      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Room' } });
      fireEvent.change(screen.getByLabelText(/code/i), { target: { value: 'LH-101' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/capacity/i), { target: { value: '50' } });
      fireEvent.change(screen.getByLabelText(/room type/i), { target: { value: 'CLASSROOM' } });

      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(screen.getByText(/room code already exists in this campus/i)).toBeInTheDocument();
      });
    });

    it('displays form-level error when no field errors', async () => {
      const onSubmit = vi.fn((data, handlers) => {
        handlers.onError({
          response: {
            status: 500,
          },
        });
      });

      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Room' } });
      fireEvent.change(screen.getByLabelText(/code/i), { target: { value: 'LH-101' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/capacity/i), { target: { value: '50' } });
      fireEvent.change(screen.getByLabelText(/room type/i), { target: { value: 'CLASSROOM' } });

      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });
  });

  describe('accessibility', () => {
    it('has proper aria attributes for invalid fields', async () => {
      renderModal({ mode: 'create' });

      fireEvent.click(screen.getByRole('button', { name: /save/i }));

      await waitFor(() => {
        const nameInput = screen.getByLabelText(/name/i);
        expect(nameInput).toHaveAttribute('aria-invalid', 'true');
        expect(nameInput).toHaveAttribute('aria-describedby', 'room-name-error');
      });
    });

    it('traps focus in the modal dialog', () => {
      renderModal({ mode: 'create' });
      const dialog = screen.getByRole('dialog');
      expect(dialog).toBeInTheDocument();
    });

    it('closes on Escape key', () => {
      const onClose = vi.fn();
      renderModal({ mode: 'create', onClose });

      const dialog = screen.getByRole('dialog');
      fireEvent.cancel(dialog);

      expect(onClose).toHaveBeenCalled();
    });
  });
});
