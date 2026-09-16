import PropTypes from 'prop-types';
import { useEffect, useRef } from 'react';

// A4-340 section 5.7 — delete confirmation. Uses the native <dialog> element which
// provides a focus trap and Escape-to-dismiss for free (NFR-3 / ui-standards).
export default function ConfirmDeleteDialog({ open, label, isPending, onConfirm, onCancel }) {
  const ref = useRef(null);

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return undefined;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
    return undefined;
  }, [open]);

  return (
    <dialog ref={ref} className="modal" onCancel={onCancel} aria-label="Confirm delete">
      <div className="modal-header">
        <h2 className="modal-title">Delete confirmation</h2>
        <button
          type="button"
          className="modal-close"
          aria-label="Close"
          onClick={onCancel}
          disabled={isPending}
        >
          ✕
        </button>
      </div>

      <div className="modal-body">
        <p className="confirm-text">
          Are you sure you want to delete {label}? This action cannot be undone.
        </p>
      </div>

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={isPending}>
          Cancel
        </button>
        <button type="button" className="btn btn--danger" onClick={onConfirm} disabled={isPending}>
          {isPending ? 'Deleting…' : 'Delete'}
        </button>
      </div>
    </dialog>
  );
}

ConfirmDeleteDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  label: PropTypes.string.isRequired,
  isPending: PropTypes.bool,
  onConfirm: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};
