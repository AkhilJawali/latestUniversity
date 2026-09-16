import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import {
  useCreatePattern,
  useUpdatePattern,
  useWorkingDayPattern,
} from '../api/useWorkingDayPattern';
import { PATTERN_TYPES } from '../constants/calendar-constants';
import { patternSchema } from '../schemas/calendar-schemas';

// A4-440 §9 — per-campus working-day pattern editor. Create-or-edit (upsert): if a pattern
// exists it is prefilled and saved via PUT, otherwise saved via POST (PD-A440-4). Pattern
// type is an enum + strings (workingSaturdays "1,3", customDefinition) — not weekday
// checkboxes (no such backend field). Conditional required per FR-7.3. No delete (backend
// returns 422). This is an inline section, not a modal.
const EMPTY = { patternType: 'FIVE_DAY', workingSaturdays: '', customDefinition: '' };

export default function WorkingDayPatternEditor({ campusId, onToast }) {
  const query = useWorkingDayPattern(campusId);
  const createMut = useCreatePattern(campusId);
  const updateMut = useUpdatePattern(campusId);

  const existing = query.data?.data ?? null;
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);

  // Prefill from the loaded pattern (or reset to blank when none / campus changes).
  useEffect(() => {
    if (existing) {
      setValues({
        patternType: existing.patternType ?? 'FIVE_DAY',
        workingSaturdays: existing.workingSaturdays ?? '',
        customDefinition: existing.customDefinition ?? '',
      });
    } else {
      setValues(EMPTY);
    }
    setErrors({});
    setFormMessage(null);
  }, [existing, campusId]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];
  const isPending = createMut.isPending || updateMut.isPending;

  const submit = (e) => {
    e.preventDefault();
    const result = validateWith(patternSchema, values);
    if (!result.success) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    setFormMessage(null);
    const body = { campusId: Number(campusId), ...result.data };
    const mut = existing ? updateMut : createMut;
    mut.mutate(body, {
      onSuccess: () => onToast?.('success', 'Working-day pattern saved.'),
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) setErrors(mapped.fields);
        else setFormMessage(mapped.message);
      },
    });
  };

  const showSaturdays = values.patternType === 'ALTERNATE_SATURDAY';
  const showCustom = values.patternType === 'CUSTOM';

  return (
    <section className="card" aria-label="Working-day pattern">
      <div className="section-head">
        <h2>Working-day pattern</h2>
      </div>

      {query.isLoading && <p className="table-empty">Loading pattern…</p>}

      {!query.isLoading && (
        <form onSubmit={submit} noValidate className="pattern-form">
          <p className="page-subtitle">
            {existing ? 'Editing the current pattern for this campus.' : 'No pattern yet — create one.'}
          </p>

          <div className="form-field">
            <label htmlFor="pat-type">
              Pattern type <span className="req">*</span>
            </label>
            <select
              id="pat-type"
              value={values.patternType}
              onChange={setField('patternType')}
              aria-invalid={Boolean(err('patternType'))}
              aria-describedby={err('patternType') ? 'pat-type-error' : undefined}
            >
              {PATTERN_TYPES.map((p) => (
                <option key={p.value} value={p.value}>
                  {p.label}
                </option>
              ))}
            </select>
            {err('patternType') && (
              <p id="pat-type-error" className="field-error">
                {err('patternType')}
              </p>
            )}
          </div>

          {showSaturdays && (
            <div className="form-field">
              <label htmlFor="pat-sat">
                Working Saturdays <span className="req">*</span>
              </label>
              <input
                id="pat-sat"
                value={values.workingSaturdays}
                onChange={setField('workingSaturdays')}
                placeholder="e.g. 1,3 (1st and 3rd Saturday)"
                aria-invalid={Boolean(err('workingSaturdays'))}
                aria-describedby={err('workingSaturdays') ? 'pat-sat-error' : undefined}
              />
              {err('workingSaturdays') && (
                <p id="pat-sat-error" className="field-error">
                  {err('workingSaturdays')}
                </p>
              )}
            </div>
          )}

          {showCustom && (
            <div className="form-field">
              <label htmlFor="pat-custom">
                Custom definition <span className="req">*</span>
              </label>
              <textarea
                id="pat-custom"
                rows={3}
                value={values.customDefinition}
                onChange={setField('customDefinition')}
                placeholder="Describe the custom working-day pattern"
                aria-invalid={Boolean(err('customDefinition'))}
                aria-describedby={err('customDefinition') ? 'pat-custom-error' : undefined}
              />
              {err('customDefinition') && (
                <p id="pat-custom-error" className="field-error">
                  {err('customDefinition')}
                </p>
              )}
            </div>
          )}

          {formMessage && (
            <p className="form-error" role="alert">
              {formMessage}
            </p>
          )}

          <div className="form-actions">
            <button type="submit" className="btn btn--primary" disabled={isPending}>
              {isPending ? 'Saving…' : existing ? 'Update pattern' : 'Create pattern'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}

WorkingDayPatternEditor.propTypes = {
  campusId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  onToast: PropTypes.func,
};
