import PropTypes from 'prop-types';
import { useEffect, useMemo, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { EXAM_TYPES } from '../constants/calendar-constants';
import { makeExamWindowSchema } from '../schemas/calendar-schemas';

// A4-440 §9 — add an exam window to a calendar (add-only; no edit, no delete — OQ-3).
// Fields: start/end date, exam type (Mid-/End-semester, Supplementary), optional
// description. Advisory within-semester check via the schema factory (KD-A440-1); the
// backend enforces it authoritatively (422 handled). Native <dialog> for focus trap.
const EMPTY = { startDate: '', endDate: '', examType: 'MID_SEMESTER', description: '' };

export default function ExamWindowFormModal({
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
    () => makeExamWindowSchema({ semesterStartDate, semesterEndDate }),
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
    <dialog ref={ref} className="modal" onCancel={onClose} aria-label="Add exam window">
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">Add exam window</h2>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="exam-start">
              Start date <span className="req">*</span>
            </label>
            <input
              id="exam-start"
              type="date"
              value={values.startDate}
              onChange={setField('startDate')}
              aria-invalid={Boolean(err('startDate'))}
              aria-describedby={err('startDate') ? 'exam-start-error' : undefined}
            />
            {err('startDate') && (
              <p id="exam-start-error" className="field-error">
                {err('startDate')}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="exam-end">
              End date <span className="req">*</span>
            </label>
            <input
              id="exam-end"
              type="date"
              value={values.endDate}
              onChange={setField('endDate')}
              aria-invalid={Boolean(err('endDate'))}
              aria-describedby={err('endDate') ? 'exam-end-error' : undefined}
            />
            {err('endDate') && (
              <p id="exam-end-error" className="field-error">
                {err('endDate')}
              </p>
            )}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="exam-type">
            Exam type <span className="req">*</span>
          </label>
          <select
            id="exam-type"
            value={values.examType}
            onChange={setField('examType')}
            aria-invalid={Boolean(err('examType'))}
            aria-describedby={err('examType') ? 'exam-type-error' : undefined}
          >
            {EXAM_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
          {err('examType') && (
            <p id="exam-type-error" className="field-error">
              {err('examType')}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="exam-desc">Description</label>
          <input
            id="exam-desc"
            value={values.description}
            onChange={setField('description')}
            placeholder="Optional"
            aria-invalid={Boolean(err('description'))}
            aria-describedby={err('description') ? 'exam-desc-error' : undefined}
          />
          {err('description') && (
            <p id="exam-desc-error" className="field-error">
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
            {isPending ? 'Saving…' : 'Add exam window'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

ExamWindowFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  semesterStartDate: PropTypes.string,
  semesterEndDate: PropTypes.string,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
