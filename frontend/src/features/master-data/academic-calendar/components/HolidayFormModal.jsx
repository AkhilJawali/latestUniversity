import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { HOLIDAY_SCOPES } from '../constants/calendar-constants';
import { holidaySchema } from '../schemas/calendar-schemas';

// A4-440 §9 — add a holiday to a calendar (add-only + separate delete; no edit, OQ-3).
// Holidays are date ranges (start + end), labelled by description, with a scope
// (Campus-specific / Institution-wide). No within-semester check — the backend does not
// bind holidays to the semester (KD-A440-1). Native <dialog> for focus trap + Escape.
const EMPTY = { startDate: '', endDate: '', description: '', scope: 'CAMPUS_SPECIFIC' };

export default function HolidayFormModal({ open, isPending, onSubmit, onClose }) {
  const ref = useRef(null);
  const [values, setValues] = useState(EMPTY);
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
      setValues(EMPTY);
      setErrors({});
      setFormMessage(null);
    }
  }, [open]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];

  const submit = (e) => {
    e.preventDefault();
    const result = validateWith(holidaySchema, values);
    if (!result.success) {
      setErrors(result.errors);
      return;
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
    <dialog ref={ref} className="modal" onCancel={onClose} aria-label="Add holiday">
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">Add holiday</h2>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="hol-start">
              Start date <span className="req">*</span>
            </label>
            <input
              id="hol-start"
              type="date"
              value={values.startDate}
              onChange={setField('startDate')}
              aria-invalid={Boolean(err('startDate'))}
              aria-describedby={err('startDate') ? 'hol-start-error' : undefined}
            />
            {err('startDate') && (
              <p id="hol-start-error" className="field-error">
                {err('startDate')}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="hol-end">
              End date <span className="req">*</span>
            </label>
            <input
              id="hol-end"
              type="date"
              value={values.endDate}
              onChange={setField('endDate')}
              aria-invalid={Boolean(err('endDate'))}
              aria-describedby={err('endDate') ? 'hol-end-error' : undefined}
            />
            {err('endDate') && (
              <p id="hol-end-error" className="field-error">
                {err('endDate')}
              </p>
            )}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="hol-desc">
            Description <span className="req">*</span>
          </label>
          <input
            id="hol-desc"
            value={values.description}
            onChange={setField('description')}
            placeholder="e.g. Diwali break"
            aria-invalid={Boolean(err('description'))}
            aria-describedby={err('description') ? 'hol-desc-error' : undefined}
          />
          {err('description') && (
            <p id="hol-desc-error" className="field-error">
              {err('description')}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="hol-scope">
            Scope <span className="req">*</span>
          </label>
          <select
            id="hol-scope"
            value={values.scope}
            onChange={setField('scope')}
            aria-invalid={Boolean(err('scope'))}
            aria-describedby={err('scope') ? 'hol-scope-error' : undefined}
          >
            {HOLIDAY_SCOPES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
          {err('scope') && (
            <p id="hol-scope-error" className="field-error">
              {err('scope')}
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
            {isPending ? 'Saving…' : 'Add holiday'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

HolidayFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
