import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { calendarCreateSchema } from '../schemas/calendar-schemas';

// A4-440 §9 (PD-A440-1) — create-only modal for an academic calendar. Bespoke because it
// needs date inputs. Runs the Zod schema on submit (no request on invalid — AC-5) and
// maps backend errors via mapApiError. Calendar has no update endpoint, so this is
// create-only (OQ-3). Native <dialog> gives focus trap + Escape (NFR-3).
const EMPTY = {
  academicYear: '',
  semesterIdentifier: '',
  semesterStartDate: '',
  semesterEndDate: '',
};

export default function CalendarFormModal({ open, isPending, onSubmit, onClose }) {
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
    const result = validateWith(calendarCreateSchema, values);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-5: no request on invalid input
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
    <dialog ref={ref} className="modal" onCancel={onClose} aria-label="New academic calendar">
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">New academic calendar</h2>

        <div className="form-field">
          <label htmlFor="cal-year">
            Academic year <span className="req">*</span>
          </label>
          <input
            id="cal-year"
            value={values.academicYear}
            onChange={setField('academicYear')}
            placeholder="e.g. 2026-27"
            aria-invalid={Boolean(err('academicYear'))}
            aria-describedby={err('academicYear') ? 'cal-year-error' : undefined}
          />
          {err('academicYear') && (
            <p id="cal-year-error" className="field-error">
              {err('academicYear')}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="cal-sem">
            Semester identifier <span className="req">*</span>
          </label>
          <input
            id="cal-sem"
            value={values.semesterIdentifier}
            onChange={setField('semesterIdentifier')}
            placeholder="e.g. ODD / SEM-1"
            aria-invalid={Boolean(err('semesterIdentifier'))}
            aria-describedby={err('semesterIdentifier') ? 'cal-sem-error' : undefined}
          />
          {err('semesterIdentifier') && (
            <p id="cal-sem-error" className="field-error">
              {err('semesterIdentifier')}
            </p>
          )}
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="cal-start">
              Semester start <span className="req">*</span>
            </label>
            <input
              id="cal-start"
              type="date"
              value={values.semesterStartDate}
              onChange={setField('semesterStartDate')}
              aria-invalid={Boolean(err('semesterStartDate'))}
              aria-describedby={err('semesterStartDate') ? 'cal-start-error' : undefined}
            />
            {err('semesterStartDate') && (
              <p id="cal-start-error" className="field-error">
                {err('semesterStartDate')}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="cal-end">
              Semester end <span className="req">*</span>
            </label>
            <input
              id="cal-end"
              type="date"
              value={values.semesterEndDate}
              onChange={setField('semesterEndDate')}
              aria-invalid={Boolean(err('semesterEndDate'))}
              aria-describedby={err('semesterEndDate') ? 'cal-end-error' : undefined}
            />
            {err('semesterEndDate') && (
              <p id="cal-end-error" className="field-error">
                {err('semesterEndDate')}
              </p>
            )}
          </div>
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

CalendarFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
