import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { DAYS_OF_WEEK } from '../constants/availability-options';
import { availabilityWindowSchema } from '../schemas/availability-schemas';

// A4-425 §5.3 (PD-2) — custom create/edit modal for an availability (unavailability)
// window. Bespoke because it needs <input type="time"> fields the generic
// ConfigFormModal doesn't provide. Runs the Zod schema on submit (no request on invalid
// — AC-3) and maps backend errors via mapApiError. Native <dialog> = focus trap + Escape.
export default function AvailabilityWindowModal({
  open,
  mode,
  initialValues,
  isPending,
  onSubmit,
  onClose,
}) {
  const isEditing = mode === 'edit';
  const ref = useRef(null);
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return undefined;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
    return undefined;
  }, [open]);

  useEffect(() => {
    if (open) {
      setValues(initialValues);
      setErrors({});
      setFormMessage(null);
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];

  const submit = (e) => {
    e.preventDefault();
    const result = validateWith(availabilityWindowSchema, values);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-3: no request on invalid input
    }
    setErrors({});
    setFormMessage(null);
    onSubmit(result.data, {
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) setErrors(mapped.fields);
        else setFormMessage(mapped.message);
      },
    });
  };

  return (
    <dialog
      ref={ref}
      className="modal"
      onCancel={onClose}
      aria-label={isEditing ? 'Edit availability window' : 'Add availability window'}
    >
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">
          {isEditing ? 'Edit availability window' : 'Add availability window'}
        </h2>

        <div className="form-field">
          <label htmlFor="aw-day">
            Day of week <span className="req">*</span>
          </label>
          <select
            id="aw-day"
            value={values.dayOfWeek ?? ''}
            onChange={setField('dayOfWeek')}
            aria-invalid={Boolean(err('dayOfWeek'))}
            aria-describedby={err('dayOfWeek') ? 'aw-day-error' : undefined}
          >
            <option value="">Select…</option>
            {DAYS_OF_WEEK.map((d) => (
              <option key={d} value={d}>
                {d.charAt(0) + d.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
          {err('dayOfWeek') && (
            <p id="aw-day-error" className="field-error">
              {err('dayOfWeek')}
            </p>
          )}
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="aw-start">
              Start time <span className="req">*</span>
            </label>
            <input
              id="aw-start"
              type="time"
              value={values.startTime ?? ''}
              onChange={setField('startTime')}
              aria-invalid={Boolean(err('startTime'))}
              aria-describedby={err('startTime') ? 'aw-start-error' : undefined}
            />
            {err('startTime') && (
              <p id="aw-start-error" className="field-error">
                {err('startTime')}
              </p>
            )}
          </div>
          <div className="form-field">
            <label htmlFor="aw-end">
              End time <span className="req">*</span>
            </label>
            <input
              id="aw-end"
              type="time"
              value={values.endTime ?? ''}
              onChange={setField('endTime')}
              aria-invalid={Boolean(err('endTime'))}
              aria-describedby={err('endTime') ? 'aw-end-error' : undefined}
            />
            {err('endTime') && (
              <p id="aw-end-error" className="field-error">
                {err('endTime')}
              </p>
            )}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="aw-reason">
            Reason code <span className="req">*</span>
          </label>
          <input
            id="aw-reason"
            value={values.reasonCode ?? ''}
            onChange={setField('reasonCode')}
            aria-invalid={Boolean(err('reasonCode'))}
            aria-describedby={err('reasonCode') ? 'aw-reason-error' : undefined}
          />
          <p className="hint">A short code, e.g. RESEARCH, ADMIN, PART_TIME (max 50 chars).</p>
          {err('reasonCode') && (
            <p id="aw-reason-error" className="field-error">
              {err('reasonCode')}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="aw-note">Note</label>
          <textarea
            id="aw-note"
            rows={2}
            value={values.reasonNote ?? ''}
            onChange={setField('reasonNote')}
            aria-invalid={Boolean(err('reasonNote'))}
            aria-describedby={err('reasonNote') ? 'aw-note-error' : undefined}
          />
          {err('reasonNote') && (
            <p id="aw-note-error" className="field-error">
              {err('reasonNote')}
            </p>
          )}
        </div>

        {formMessage && (
          <p className="form-error" role="alert">
            {formMessage}
          </p>
        )}

        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose} disabled={isPending}>
            Cancel
          </button>
          <button type="submit" className="btn btn--primary" disabled={isPending}>
            {isPending ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

AvailabilityWindowModal.propTypes = {
  open: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']).isRequired,
  initialValues: PropTypes.object.isRequired,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
