import PropTypes from 'prop-types';
import { useEffect, useMemo, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { makeOrientationSchema } from '../schemas/calendar-schemas';

// A4-440 §9 — add an orientation period to a calendar (add-only; no edit, no delete —
// OQ-3). Fields: start/end date, optional description. Advisory within-semester check via
// the schema factory (KD-A440-1); the backend enforces it (422 handled). Native <dialog>
// for focus trap + Escape.
const EMPTY = { startDate: '', endDate: '', description: '' };

export default function OrientationFormModal({
  open,
  semesterStartDate,
  semesterEndDate,
  isPending,
  onSubmit,
  onClose,
}) {
  const ref = useRef(null);
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);

  const schema = useMemo(
    () => makeOrientationSchema({ semesterStartDate, semesterEndDate }),
    [semesterStartDate, semesterEndDate],
  );

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
    const result = validateWith(schema, values);
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
    <dialog ref={ref} className="modal" onCancel={onClose} aria-label="Add orientation period">
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">Add orientation period</h2>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="ori-start">
              Start date <span className="req">*</span>
            </label>
            <input
              id="ori-start"
              type="date"
              value={values.startDate}
              onChange={setField('startDate')}
              aria-invalid={Boolean(err('startDate'))}
              aria-describedby={err('startDate') ? 'ori-start-error' : undefined}
            />
            {err('startDate') && (
              <p id="ori-start-error" className="field-error">
                {err('startDate')}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="ori-end">
              End date <span className="req">*</span>
            </label>
            <input
              id="ori-end"
              type="date"
              value={values.endDate}
              onChange={setField('endDate')}
              aria-invalid={Boolean(err('endDate'))}
              aria-describedby={err('endDate') ? 'ori-end-error' : undefined}
            />
            {err('endDate') && (
              <p id="ori-end-error" className="field-error">
                {err('endDate')}
              </p>
            )}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="ori-desc">Description</label>
          <input
            id="ori-desc"
            value={values.description}
            onChange={setField('description')}
            placeholder="Optional"
            aria-invalid={Boolean(err('description'))}
            aria-describedby={err('description') ? 'ori-desc-error' : undefined}
          />
          {err('description') && (
            <p id="ori-desc-error" className="field-error">
              {err('description')}
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
            {isPending ? 'Saving…' : 'Add orientation'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

OrientationFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  semesterStartDate: PropTypes.string,
  semesterEndDate: PropTypes.string,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
