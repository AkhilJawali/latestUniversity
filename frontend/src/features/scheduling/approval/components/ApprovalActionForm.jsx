import PropTypes from 'prop-types';
import { useState } from 'react';

// Inline confirm form for one approval action (submit / approve / reject / publish).
// Reject requires a reason; comments are optional and omitted for publish.
export default function ApprovalActionForm({
  title,
  confirmLabel,
  requireReason = false,
  withComments = true,
  warning,
  danger = false,
  isPending = false,
  errorMessage,
  onConfirm,
  onCancel,
}) {
  const [comments, setComments] = useState('');
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState(null);

  const submit = (e) => {
    e.preventDefault();
    if (requireReason && !reason.trim()) {
      setReasonError('Please give a reason for rejecting.');
      return;
    }
    setReasonError(null);
    onConfirm({ comments: comments.trim(), rejectionReason: reason.trim() });
  };

  return (
    <form className="approval-action" onSubmit={submit} noValidate aria-label={title}>
      <h3>{title}</h3>
      {warning && <p className="approval-warning">{warning}</p>}

      {requireReason && (
        <div className="form-field">
          <label htmlFor="rejectionReason">
            Reason <span aria-hidden="true" className="req">*</span>
          </label>
          <textarea
            id="rejectionReason"
            rows={2}
            maxLength={500}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={Boolean(reasonError)}
            aria-describedby={reasonError ? 'rejectionReason-error' : undefined}
          />
          {reasonError && (
            <p id="rejectionReason-error" className="field-error">
              {reasonError}
            </p>
          )}
        </div>
      )}

      {withComments && (
        <div className="form-field">
          <label htmlFor="approvalComments">Comments (optional)</label>
          <textarea
            id="approvalComments"
            rows={2}
            maxLength={2000}
            value={comments}
            onChange={(e) => setComments(e.target.value)}
          />
        </div>
      )}

      {errorMessage && (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      )}

      <div className="approval-action__buttons">
        <button type="button" className="btn" onClick={onCancel} disabled={isPending}>
          Cancel
        </button>
        <button type="submit" className={`btn ${danger ? 'btn--danger' : 'btn--primary'}`} disabled={isPending}>
          {isPending ? 'Saving…' : confirmLabel}
        </button>
      </div>
    </form>
  );
}

ApprovalActionForm.propTypes = {
  title: PropTypes.string.isRequired,
  confirmLabel: PropTypes.string.isRequired,
  requireReason: PropTypes.bool,
  withComments: PropTypes.bool,
  warning: PropTypes.string,
  danger: PropTypes.bool,
  isPending: PropTypes.bool,
  errorMessage: PropTypes.string,
  onConfirm: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};
