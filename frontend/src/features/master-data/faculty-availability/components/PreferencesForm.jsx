import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { useFacultyPreferences, useSetPreferences } from '../api/useFacultyPreferences';
import { SESSION_DISTRIBUTION, TIME_OF_DAY } from '../constants/availability-options';
import { preferenceSchema } from '../schemas/availability-schemas';

// Turn an enum token like NO_PREFERENCE into "No preference" for display.
function labelOf(token) {
  return token
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

// A4-425 §5.4 / FR-4 — inline soft-preferences form. Loads the current preference (the
// backend returns a NO_PREFERENCE default when unset, so the form always has values) and
// upserts via PUT. Validation via the shared validateWith; backend allowlist errors (422)
// surfaced inline.
export default function PreferencesForm({ facultyId }) {
  const query = useFacultyPreferences(facultyId);
  const save = useSetPreferences(facultyId);

  const [values, setValues] = useState({
    preferredTimeOfDay: 'NO_PREFERENCE',
    sessionDistribution: 'NO_PREFERENCE',
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);

  // Seed the form from the loaded preference.
  useEffect(() => {
    const p = query.data?.data;
    if (p) {
      setValues({
        preferredTimeOfDay: p.preferredTimeOfDay ?? 'NO_PREFERENCE',
        sessionDistribution: p.sessionDistribution ?? 'NO_PREFERENCE',
      });
    }
  }, [query.data]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];

  const submit = (e) => {
    e.preventDefault();
    setMessage(null);
    const result = validateWith(preferenceSchema, values);
    if (!result.success) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    save.mutate(result.data, {
      onSuccess: () => setMessage('Preferences saved.'),
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) setErrors(mapped.fields);
        else setMessage(mapped.message ?? 'Could not save preferences.');
      },
    });
  };

  return (
    <section className="card" aria-label="Scheduling preferences">
      <div className="section-head">
        <h2>Preferences (soft)</h2>
      </div>

      {query.isLoading ? (
        <p>Loading preferences…</p>
      ) : (
        <form className="prefs-form" onSubmit={submit} noValidate>
          <div className="form-field">
            <label htmlFor="pref-time">Preferred time of day</label>
            <select
              id="pref-time"
              value={values.preferredTimeOfDay}
              onChange={setField('preferredTimeOfDay')}
              aria-invalid={Boolean(err('preferredTimeOfDay'))}
            >
              {TIME_OF_DAY.map((t) => (
                <option key={t} value={t}>
                  {labelOf(t)}
                </option>
              ))}
            </select>
            {err('preferredTimeOfDay') && (
              <p className="field-error">{err('preferredTimeOfDay')}</p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="pref-dist">Session distribution</label>
            <select
              id="pref-dist"
              value={values.sessionDistribution}
              onChange={setField('sessionDistribution')}
              aria-invalid={Boolean(err('sessionDistribution'))}
            >
              {SESSION_DISTRIBUTION.map((d) => (
                <option key={d} value={d}>
                  {labelOf(d)}
                </option>
              ))}
            </select>
            {err('sessionDistribution') && (
              <p className="field-error">{err('sessionDistribution')}</p>
            )}
          </div>

          <button type="submit" className="btn btn--primary" disabled={save.isPending}>
            {save.isPending ? 'Saving…' : 'Save preferences'}
          </button>

          {message && (
            <p className="form-message" role="status">
              {message}
            </p>
          )}
        </form>
      )}
    </section>
  );
}

PreferencesForm.propTypes = {
  facultyId: PropTypes.number.isRequired,
};
