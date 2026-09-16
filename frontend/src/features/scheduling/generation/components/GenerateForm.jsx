import PropTypes from 'prop-types';
import { useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import { useTriggerGeneration } from '@/features/scheduling/generation/api/useGeneration';
import GenerationStartError from '@/features/scheduling/generation/components/GenerationStartError';
import { validateGenerationForm } from '@/features/scheduling/generation/lib/generationSchema';

// A4-345 FR-1 — trigger form. The department is picked by code (campus first, because a
// department code is only unique within its campus); the backend still receives the
// department id, which the user never sees. Zod-validated before submit; the backend
// re-validates.
const EMPTY = { campusId: '', departmentId: '', semester: '', academicYear: '', seed: '' };

export default function GenerateForm({ onStarted }) {
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const trigger = useTriggerGeneration();

  const campuses = useCampuses();
  const campusRows = campuses.data?.data ?? [];
  // With a single campus there is nothing to choose — use it directly.
  const campusId = values.campusId || (campusRows.length === 1 ? String(campusRows[0].id) : '');
  const departments = useDepartments(campusId ? Number(campusId) : null);
  const departmentRows = departments.data?.data ?? [];

  const setField = (name) => (e) => {
    setValues((v) => ({ ...v, [name]: e.target.value }));
  };

  const setCampus = (e) => {
    setValues((v) => ({ ...v, campusId: e.target.value, departmentId: '' }));
  };

  const submit = (e) => {
    e.preventDefault();
    const result = validateGenerationForm(values);
    if (!result.success) {
      setErrors(result.errors);
      return;
    }
    setErrors({});
    trigger.mutate(result.data, {
      onSuccess: (data) => onStarted(data.requestId),
    });
  };

  let departmentPlaceholder = 'Select department…';
  if (!campusId) departmentPlaceholder = 'Select a campus first';
  else if (departments.isLoading) departmentPlaceholder = 'Loading departments…';
  else if (departmentRows.length === 0) departmentPlaceholder = 'No departments on this campus';

  return (
    <form className="card gen-form" onSubmit={submit} noValidate>
      <h2>Generate Timetable</h2>

      <div className="form-field">
        <label htmlFor="campusId">
          Campus <span aria-hidden="true" className="req">*</span>
        </label>
        <select id="campusId" name="campusId" value={campusId} onChange={setCampus}>
          <option value="">{campuses.isLoading ? 'Loading campuses…' : 'Select campus…'}</option>
          {campusRows.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} — {c.name}
            </option>
          ))}
        </select>
        {campuses.isError && (
          <p className="field-error" role="alert">
            Could not load campuses.
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="departmentId">
          Department <span aria-hidden="true" className="req">*</span>
        </label>
        <select
          id="departmentId"
          name="departmentId"
          value={values.departmentId}
          onChange={setField('departmentId')}
          disabled={!campusId}
          aria-invalid={Boolean(errors.departmentId)}
          aria-describedby={errors.departmentId ? 'departmentId-error' : undefined}
        >
          <option value="">{departmentPlaceholder}</option>
          {departmentRows.map((d) => (
            <option key={d.id} value={d.id}>
              {d.code} — {d.name}
            </option>
          ))}
        </select>
        {errors.departmentId && (
          <p id="departmentId-error" className="field-error">
            {errors.departmentId}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="semester">
          Semester <span aria-hidden="true" className="req">*</span>
        </label>
        <input
          id="semester"
          name="semester"
          type="text"
          value={values.semester}
          onChange={setField('semester')}
          aria-invalid={Boolean(errors.semester)}
          aria-describedby={errors.semester ? 'semester-error' : undefined}
        />
        {errors.semester && (
          <p id="semester-error" className="field-error">
            {errors.semester}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="academicYear">
          Academic Year <span aria-hidden="true" className="req">*</span>
        </label>
        <input
          id="academicYear"
          name="academicYear"
          type="text"
          value={values.academicYear}
          onChange={setField('academicYear')}
          aria-invalid={Boolean(errors.academicYear)}
          aria-describedby={errors.academicYear ? 'academicYear-error' : undefined}
        />
        {errors.academicYear && (
          <p id="academicYear-error" className="field-error">
            {errors.academicYear}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="seed">Seed (optional)</label>
        <input
          id="seed"
          name="seed"
          type="number"
          inputMode="numeric"
          value={values.seed}
          onChange={setField('seed')}
          aria-invalid={Boolean(errors.seed)}
          aria-describedby={errors.seed ? 'seed-error' : undefined}
        />
        {errors.seed && (
          <p id="seed-error" className="field-error">
            {errors.seed}
          </p>
        )}
      </div>

      {/* FR-1.4: busy (503), already running (409) and precondition (422) failures are explained. */}
      {trigger.isError && <GenerationStartError error={trigger.error} campusId={campusId} />}

      <div className="form-actions">
        <button type="submit" className="btn btn--primary" disabled={trigger.isPending}>
          {trigger.isPending ? 'Starting…' : 'Generate'}
        </button>
      </div>
    </form>
  );
}

GenerateForm.propTypes = {
  onStarted: PropTypes.func.isRequired,
};
