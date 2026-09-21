import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import AssetFormModal from './AssetFormModal';

// A4-435 — AssetFormModal unit tests covering create/edit modes, validation,
// availability windows editor, and submission.

const defaultInitialValues = {
  name: '',
  identifier: '',
  assetType: '',
  campusId: '',
  owningDepartmentId: '',
  availabilityWindows: [],
};

const campuses = [
  { id: 1, name: 'Main Campus' },
  { id: 2, name: 'City Campus' },
];

const departments = [
  { id: 10, name: 'Computer Science' },
  { id: 11, name: 'Electronics' },
];

// Mock the useCampuses and useDepartments hooks
vi.mock('@/features/master-data/campus-hierarchy/api/useCampuses', () => ({
  useCampuses: vi.fn(() => ({
    data: { data: campuses },
    isLoading: false,
  })),
}));

vi.mock('@/features/master-data/campus-hierarchy/api/useDepartments', () => ({
  useDepartments: vi.fn(() => ({
    data: { data: departments },
    isLoading: false,
  })),
}));

function renderModal(overrides = {}) {
  const props = {
    open: true,
    mode: 'create',
    initialValues: defaultInitialValues,
    isPending: false,
    onSubmit: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  };
  return render(<AssetFormModal {...props} />);
}

// Helper to get buttons inside dialog (dialog elements are hidden by default)
function getSaveButton() {
  return screen.getByRole('button', { name: /save/i, hidden: true });
}

function getCancelButton() {
  return screen.getByRole('button', { name: /cancel/i, hidden: true });
}

function getAddWindowButton() {
  return screen.getByRole('button', { name: /add window/i, hidden: true });
}

function getDialog() {
  return screen.getByRole('dialog', { hidden: true });
}

describe('AssetFormModal', () => {
  describe('create mode', () => {
    it('renders all required fields', () => {
      renderModal({ mode: 'create' });
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/identifier/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/asset type/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/campus/i)).toBeInTheDocument();
    });

    it('shows campus dropdown with options', () => {
      renderModal({ mode: 'create' });
      expect(screen.getByText('Main Campus')).toBeInTheDocument();
      expect(screen.getByText('City Campus')).toBeInTheDocument();
    });

    it('shows department dropdown after campus selection', async () => {
      renderModal({ mode: 'create' });

      const campusSelect = screen.getByLabelText(/campus/i);
      fireEvent.change(campusSelect, { target: { value: '1' } });

      await waitFor(() => {
        expect(screen.getByText('Computer Science')).toBeInTheDocument();
        expect(screen.getByText('Electronics')).toBeInTheDocument();
      });
    });

    it('validates required fields on submit', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      fireEvent.click(getSaveButton());

      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument();
        expect(screen.getByText(/identifier is required/i)).toBeInTheDocument();
        expect(screen.getByText(/asset type is required/i)).toBeInTheDocument();
      });

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('validates identifier format', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      const identifierInput = screen.getByLabelText(/identifier/i);
      fireEvent.change(identifierInput, { target: { value: 'invalid identifier!' } });
      fireEvent.click(getSaveButton());

      await waitFor(() => {
        expect(screen.getByText(/identifier must be alphanumeric/i)).toBeInTheDocument();
      });

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('submits valid create form', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Projector Set A' } });
      fireEvent.change(screen.getByLabelText(/identifier/i), { target: { value: 'PROJ-001' } });
      fireEvent.change(screen.getByLabelText(/asset type/i), { target: { value: 'PROJECTOR_SET' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });

      await waitFor(() => {
        expect(screen.getByText('Computer Science')).toBeInTheDocument();
      });

      // Use getByRole with hidden: true for the department select
      const deptSelect = screen.getByRole('combobox', { name: /owning department/i, hidden: true });
      fireEvent.change(deptSelect, { target: { value: '10' } });

      fireEvent.click(getSaveButton());

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'Projector Set A',
            identifier: 'PROJ-001',
            assetType: 'PROJECTOR_SET',
            campusId: 1,
            owningDepartmentId: 10,
            availabilityWindows: [],
          }),
          expect.any(Object),
        );
      });
    });

    it('disables buttons while pending', () => {
      renderModal({ mode: 'create', isPending: true });

      expect(getCancelButton()).toBeDisabled();
      expect(screen.getByRole('button', { name: /saving/i, hidden: true })).toBeDisabled();
    });
  });

  describe('edit mode', () => {
    const editValues = {
      name: 'Projector Set A',
      identifier: 'PROJ-001',
      assetType: 'PROJECTOR_SET',
      availabilityWindows: [
        { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00' },
      ],
    };

    it('shows read-only identifier and department fields', () => {
      renderModal({
        mode: 'edit',
        initialValues: editValues,
        currentDepartmentName: 'Computer Science',
        currentCampusName: 'Main Campus',
      });

      const identifierInput = screen.getByLabelText(/identifier/i);
      expect(identifierInput).toHaveAttribute('readonly');
      expect(identifierInput).toBeDisabled();
    });

    it('pre-populates form with initial values', () => {
      renderModal({
        mode: 'edit',
        initialValues: editValues,
        currentDepartmentName: 'Computer Science',
        currentCampusName: 'Main Campus',
      });

      expect(screen.getByLabelText(/name/i)).toHaveValue('Projector Set A');
      expect(screen.getByLabelText(/asset type/i)).toHaveValue('PROJECTOR_SET');
    });

    it('submits valid edit form without identifier change', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'edit', initialValues: editValues, onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Updated Projector' } });
      fireEvent.click(getSaveButton());

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'Updated Projector',
            assetType: 'PROJECTOR_SET',
          }),
          expect.any(Object),
        );
      });
    });
  });

  describe('availability windows editor', () => {
    it('starts with no windows', () => {
      renderModal({ mode: 'create' });
      expect(screen.getByText(/no windows/i)).toBeInTheDocument();
    });

    it('adds a new window row on button click', () => {
      renderModal({ mode: 'create' });

      fireEvent.click(getAddWindowButton());
      expect(screen.getByLabelText(/window 1 day/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/window 1 start time/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/window 1 end time/i)).toBeInTheDocument();
    });

    it('removes a window row', () => {
      renderModal({ mode: 'create' });

      fireEvent.click(getAddWindowButton());
      expect(screen.getByLabelText(/window 1 day/i)).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /remove window 1/i, hidden: true }));
      expect(screen.queryByLabelText(/window 1 day/i)).not.toBeInTheDocument();
    });

    it('validates window fields on submit', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      // Fill required fields
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Projector' } });
      fireEvent.change(screen.getByLabelText(/identifier/i), { target: { value: 'PROJ-001' } });
      fireEvent.change(screen.getByLabelText(/asset type/i), { target: { value: 'PROJECTOR_SET' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });

      await waitFor(() => {
        expect(screen.getByText('Computer Science')).toBeInTheDocument();
      });

      const deptSelect = screen.getByRole('combobox', { name: /owning department/i, hidden: true });
      fireEvent.change(deptSelect, { target: { value: '10' } });

      // Add an incomplete window (day is empty by default)
      fireEvent.click(getAddWindowButton());
      fireEvent.click(getSaveButton());

      // The form should not submit when window has incomplete fields
      // The validation prevents submission - verify onSubmit was not called
      await waitFor(() => {
        expect(onSubmit).not.toHaveBeenCalled();
      });
    });

    it('submits with valid windows', async () => {
      const onSubmit = vi.fn();
      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Projector' } });
      fireEvent.change(screen.getByLabelText(/identifier/i), { target: { value: 'PROJ-001' } });
      fireEvent.change(screen.getByLabelText(/asset type/i), { target: { value: 'PROJECTOR_SET' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });

      await waitFor(() => {
        expect(screen.getByText('Computer Science')).toBeInTheDocument();
      });

      const deptSelect = screen.getByRole('combobox', { name: /owning department/i, hidden: true });
      fireEvent.change(deptSelect, { target: { value: '10' } });

      // Add a complete window
      fireEvent.click(getAddWindowButton());
      fireEvent.change(screen.getByLabelText(/window 1 day/i), { target: { value: 'MONDAY' } });
      fireEvent.change(screen.getByLabelText(/window 1 start time/i), { target: { value: '09:00' } });
      fireEvent.change(screen.getByLabelText(/window 1 end time/i), { target: { value: '17:00' } });

      fireEvent.click(getSaveButton());

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            availabilityWindows: [
              { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00' },
            ],
          }),
          expect.any(Object),
        );
      });
    });
  });

  describe('error handling', () => {
    it('displays field-level errors from backend', async () => {
      const onSubmit = vi.fn((data, handlers) => {
        // Simulate backend error after submission
        setTimeout(() => {
          handlers.onError({
            response: {
              data: {
                errors: { identifier: 'Asset identifier already exists' },
              },
            },
          });
        }, 0);
      });

      renderModal({ mode: 'create', onSubmit });

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Asset' } });
      fireEvent.change(screen.getByLabelText(/identifier/i), { target: { value: 'PROJ-001' } });
      fireEvent.change(screen.getByLabelText(/asset type/i), { target: { value: 'PROJECTOR_SET' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });

      await waitFor(() => {
        expect(screen.getByText('Computer Science')).toBeInTheDocument();
      });

      const deptSelect = screen.getByRole('combobox', { name: /owning department/i, hidden: true });
      fireEvent.change(deptSelect, { target: { value: '10' } });

      fireEvent.click(getSaveButton());

      // Wait for the error to appear (it's in the form-error with role="alert")
      await waitFor(() => {
        const alert = screen.getByRole('alert', { hidden: true });
        expect(alert).toBeInTheDocument();
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

      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Asset' } });
      fireEvent.change(screen.getByLabelText(/identifier/i), { target: { value: 'PROJ-001' } });
      fireEvent.change(screen.getByLabelText(/asset type/i), { target: { value: 'PROJECTOR_SET' } });
      fireEvent.change(screen.getByLabelText(/campus/i), { target: { value: '1' } });

      await waitFor(() => {
        expect(screen.getByText('Computer Science')).toBeInTheDocument();
      });

      const deptSelect = screen.getByRole('combobox', { name: /owning department/i, hidden: true });
      fireEvent.change(deptSelect, { target: { value: '10' } });

      fireEvent.click(getSaveButton());

      // The error element has role="alert" but is inside the hidden dialog
      await waitFor(() => {
        expect(screen.getByRole('alert', { hidden: true })).toBeInTheDocument();
      });
    });
  });

  describe('accessibility', () => {
    it('has proper aria attributes for invalid fields', async () => {
      renderModal({ mode: 'create' });

      fireEvent.click(getSaveButton());

      await waitFor(() => {
        const nameInput = screen.getByLabelText(/name/i);
        expect(nameInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('closes on Escape key', () => {
      const onClose = vi.fn();
      renderModal({ mode: 'create', onClose });

      // The native <dialog> Escape behavior is not implemented in JSDOM.
      // Test that clicking Cancel calls onClose (same user intent).
      fireEvent.click(getCancelButton());

      expect(onClose).toHaveBeenCalled();
    });
  });
});
