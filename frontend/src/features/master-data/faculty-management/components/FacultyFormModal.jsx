import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { DESIGNATIONS } from '../constants/faculty-options';
import { facultyCreateSchema, facultyEditSchema } from '../schemas/faculty-schemas';

// Field names each mode actually renders an input for — used to decide whether a
// mapped backend field error is visible, so a 400 on an omitted field still surfaces.
function renderedFieldNames(isEditing) {
  const common = ['name', 'designation', 'qualification', 'homeDepartmentId', 'minWeeklyLoad', 'maxWeeklyLoad'];
  return isEditing ? common : [...common, 'identifier', 'campusIds'];
}

// A4-420 §5.3 (PD-2) — custom create/edit modal. The generic ConfigFormModal can't
// express the multi-selects (campuses, competencies) or the campus->department
// dependent picker this form needs, so this is bespoke but mirrors the shared modal's
// behavior: run the Zod schema on submit (no request on invalid — AC-5) and map backend
// errors via mapApiError. Native <dialog> gives focus trap + Escape (NFR-2).
export default function FacultyFormModal({
  open,
  mode,
  initialValues,
  currentDepartment,
  courses,
  isPending,
  onSubmit,
  onClose,
}) {
  const isEditing = mode === 'edit';
  const ref = useRef(null);
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);

  // Campus->department two-step for the home-department picker (PD-3).
  const [deptCampusId, setDeptCampusId] = useState('');
  const campuses = useCampuses();
  const departments = useDepartments(deptCampusId ? Number(deptCampusId) : null);
  const campusRows = campuses.data?.data ?? [];
  const deptRows = departments.data?.data ?? [];

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return undefined;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
    return undefined;
  }, [open]);

  // Reset when (re)opened for a new target.
  useEffect(() => {
    if (open) {
      setValues(initialValues);
      setErrors({});
      setFormMessage(null);
      setDeptCampusId('');
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));

  const toggleInArray = (name, id) => {
    setValues((v) => {
      const arr = v[name] ?? [];
      return arr.includes(id)
        ? { ...v, [name]: arr.filter((x) => x !== id) }
        : { ...v, [name]: [...arr, id] };
    });
  };

  const submit = (e) => {
    e.preventDefault();
    const schema = isEditing ? facultyEditSchema : facultyCreateSchema;
    const result = validateWith(schema, values);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-5: no request on invalid input
    }
    setErrors({});
    setFormMessage(null);
    onSubmit(result.data, {
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) {
          setErrors(mapped.fields);
          // If none of the returned field errors map to a field this form renders
          // (e.g. a 400 on an immutable field the edit form omits), the per-field
          // errors would be invisible — show a form-level message so the user still
          // sees why the submit was rejected.
          const rendered = renderedFieldNames(isEditing);
          const anyVisible = Object.keys(mapped.fields).some((k) => rendered.includes(k));
          if (!anyVisible) {
            setFormMessage('Could not save. Please review the submitted values and try again.');
          }
        } else {
          setFormMessage(mapped.message);
        }
      },
    });
  };

  const err = (name) => errors[name];

  return (
    <dialog
      ref={ref}
      className="modal"
      onCancel={onClose}
      aria-label={isEditing ? 'Edit faculty' : 'Add faculty'}
    >
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">{isEditing ? 'Edit faculty' : 'Add faculty'}</h2>

        <div className="form-field">
          <label htmlFor="fac-name">
            Name <span className="req">*</span>
          </label>
          <input
            id="fac-name"
            value={values.name ?? ''}
            onChange={setField('name')}
            aria-invalid={Boolean(err('name'))}
          />
          {err('name') && <p className="field-error">{err('name')}</p>}
        </div>

        {!isEditing && (
          <div className="form-field">
            <label htmlFor="fac-identifier">
              Identifier <span className="req">*</span>
            </label>
            <input
              id="fac-identifier"
              value={values.identifier ?? ''}
              onChange={setField('identifier')}
              aria-invalid={Boolean(err('identifier'))}
            />
            {err('identifier') && <p className="field-error">{err('identifier')}</p>}
          </div>
        )}

        <div className="form-field">
          <label htmlFor="fac-designation">
            Designation <span className="req">*</span>
          </label>
          <select
            id="fac-designation"
            value={values.designation ?? ''}
            onChange={setField('designation')}
            aria-invalid={Boolean(err('designation'))}
          >
            <option value="">Select…</option>
            {DESIGNATIONS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
          {err('designation') && <p className="field-error">{err('designation')}</p>}
        </div>

        <div className="form-field">
          <label htmlFor="fac-qualification">
            Qualification <span className="req">*</span>
          </label>
          <textarea
            id="fac-qualification"
            rows={2}
            value={values.qualification ?? ''}
            onChange={setField('qualification')}
            aria-invalid={Boolean(err('qualification'))}
          />
          {err('qualification') && <p className="field-error">{err('qualification')}</p>}
        </div>

        <div className="form-field">
          <label htmlFor="fac-dept-campus">
            Home department <span className="req">*</span>
          </label>
          {isEditing && currentDepartment?.id && !deptCampusId && (
            <p className="hint">
              Current: {currentDepartment.name}. Pick a campus below to change it.
            </p>
          )}
          <div className="two-step">
            <select
              id="fac-dept-campus"
              aria-label="Campus for home department"
              value={deptCampusId}
              onChange={(e) => {
                setDeptCampusId(e.target.value);
                setValues((v) => ({ ...v, homeDepartmentId: '' }));
              }}
            >
              <option value="">{isEditing ? 'Keep current…' : 'Select campus…'}</option>
              {campusRows.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            <select
              id="fac-department"
              aria-label="Home department"
              value={values.homeDepartmentId ?? ''}
              onChange={setField('homeDepartmentId')}
              disabled={!deptCampusId || departments.isLoading}
              aria-invalid={Boolean(err('homeDepartmentId'))}
            >
              {/* In edit mode before a campus is chosen, show the current department so
                  it is visible and preserved (the two-step can't load it without the
                  campus id). Choosing a campus switches to the live department list. */}
              {isEditing && currentDepartment?.id && !deptCampusId ? (
                <option value={currentDepartment.id}>{currentDepartment.name}</option>
              ) : (
                <option value="">Select department…</option>
              )}
              {deptRows.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
          {err('homeDepartmentId') && <p className="field-error">{err('homeDepartmentId')}</p>}
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="fac-min">Min weekly load</label>
            <input
              id="fac-min"
              type="number"
              step="0.1"
              value={values.minWeeklyLoad ?? ''}
              onChange={setField('minWeeklyLoad')}
              aria-invalid={Boolean(err('minWeeklyLoad'))}
            />
            {err('minWeeklyLoad') && <p className="field-error">{err('minWeeklyLoad')}</p>}
          </div>
          <div className="form-field">
            <label htmlFor="fac-max">Max weekly load</label>
            <input
              id="fac-max"
              type="number"
              step="0.1"
              value={values.maxWeeklyLoad ?? ''}
              onChange={setField('maxWeeklyLoad')}
              aria-invalid={Boolean(err('maxWeeklyLoad'))}
            />
            {err('maxWeeklyLoad') && <p className="field-error">{err('maxWeeklyLoad')}</p>}
          </div>
        </div>

        {!isEditing && (
          <>
            <fieldset
              className="form-field"
              aria-describedby={err('campusIds') ? 'campusIds-error' : undefined}
            >
              <legend>
                Campus associations <span className="req">*</span>
              </legend>
              <div className="checkbox-list">
                {campusRows.map((c) => (
                  <label key={c.id} className="checkbox-row">
                    <input
                      type="checkbox"
                      checked={(values.campusIds ?? []).includes(c.id)}
                      onChange={() => toggleInArray('campusIds', c.id)}
                    />
                    {c.name}
                  </label>
                ))}
              </div>
              {err('campusIds') && (
                <p id="campusIds-error" className="field-error" role="alert">
                  {err('campusIds')}
                </p>
              )}
            </fieldset>

            <fieldset className="form-field">
              <legend>Subject competencies</legend>
              <div className="checkbox-list">
                {courses.map((c) => (
                  <label key={c.id} className="checkbox-row">
                    <input
                      type="checkbox"
                      checked={(values.competencyCourseIds ?? []).includes(c.id)}
                      onChange={() => toggleInArray('competencyCourseIds', c.id)}
                    />
                    {c.code} — {c.name}
                  </label>
                ))}
              </div>
            </fieldset>
          </>
        )}

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

FacultyFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']).isRequired,
  initialValues: PropTypes.object.isRequired,
  currentDepartment: PropTypes.shape({
    id: PropTypes.number,
    name: PropTypes.string,
  }),
  courses: PropTypes.array,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

FacultyFormModal.defaultProps = {
  currentDepartment: null,
  courses: [],
};
