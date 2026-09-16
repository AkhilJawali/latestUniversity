import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { DAYS_OF_WEEK } from '../constants/asset-options';
import { assetCreateSchema, assetEditSchema } from '../schemas/asset-schemas';

// A4-435 §5.3 (PD-2) — custom create/edit modal for a schedulable asset. Bespoke because
// it needs a campus->department two-step (create-only) and an embedded editable
// availability-window list (add/edit/remove rows). Windows are submitted inline and the
// backend replaces the whole set (PD-5). Runs the Zod schema on submit (no request on
// invalid — AC-4) and maps backend errors via mapApiError.
export default function AssetFormModal({
  open,
  mode,
  initialValues,
  currentDepartmentName,
  currentCampusName,
  isPending,
  onSubmit,
  onClose,
}) {
  const isEditing = mode === 'edit';
  const ref = useRef(null);
  const [values, setValues] = useState(initialValues);
  const [windows, setWindows] = useState([]);
  const [errors, setErrors] = useState({});
  const [windowErrors, setWindowErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);

  // Campus->department two-step for the create form (PD-4).
  const [campusSel, setCampusSel] = useState('');
  const campuses = useCampuses();
  const departments = useDepartments(campusSel ? Number(campusSel) : null);
  const campusRows = campuses.data?.data ?? [];
  const deptRows = departments.data?.data ?? [];

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
      setWindows(initialValues.availabilityWindows ?? []);
      setErrors({});
      setWindowErrors({});
      setFormMessage(null);
      setCampusSel('');
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];

  const addWindow = () =>
    setWindows((w) => [...w, { dayOfWeek: '', startTime: '', endTime: '' }]);
  const removeWindow = (i) => setWindows((w) => w.filter((_, idx) => idx !== i));
  const setWindowField = (i, field) => (e) =>
    setWindows((w) => w.map((row, idx) => (idx === i ? { ...row, [field]: e.target.value } : row)));

  const submit = (e) => {
    e.preventDefault();
    const schema = isEditing ? assetEditSchema : assetCreateSchema;
    const candidate = { ...values, availabilityWindows: windows };
    const result = validateWith(schema, candidate);
    if (!result.success) {
      // Split window errors (keyed like "availabilityWindows.0.endTime") from field errors.
      const fieldErrs = {};
      const winErrs = {};
      Object.entries(result.errors).forEach(([k, msg]) => {
        const m = k.match(/^availabilityWindows\.(\d+)\.(\w+)$/);
        if (m) {
          const idx = m[1];
          winErrs[idx] = { ...(winErrs[idx] ?? {}), [m[2]]: msg };
        } else {
          fieldErrs[k] = msg;
        }
      });
      setErrors(fieldErrs);
      setWindowErrors(winErrs);
      return; // AC-4: no request on invalid input
    }
    setErrors({});
    setWindowErrors({});
    setFormMessage(null);
    onSubmit(result.data, {
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) setErrors(mapped.fields);
        else setFormMessage(mapped.message);
      },
    });
  };

  const winErr = (i, field) => windowErrors[i]?.[field];

  return (
    <dialog
      ref={ref}
      className="modal modal--wide"
      onCancel={onClose}
      aria-label={isEditing ? 'Edit asset' : 'Add asset'}
    >
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">{isEditing ? 'Edit asset' : 'Add asset'}</h2>

        <div className="form-field">
          <label htmlFor="asset-name">
            Name <span className="req">*</span>
          </label>
          <input
            id="asset-name"
            value={values.name ?? ''}
            onChange={setField('name')}
            aria-invalid={Boolean(err('name'))}
            aria-describedby={err('name') ? 'asset-name-error' : undefined}
          />
          {err('name') && (
            <p id="asset-name-error" className="field-error">
              {err('name')}
            </p>
          )}
        </div>

        <div className="form-row">
          {isEditing ? (
            <div className="form-field">
              <label htmlFor="asset-identifier-ro">Identifier</label>
              <input id="asset-identifier-ro" value={values.identifier ?? ''} readOnly disabled />
            </div>
          ) : (
            <div className="form-field">
              <label htmlFor="asset-identifier">
                Identifier <span className="req">*</span>
              </label>
              <input
                id="asset-identifier"
                value={values.identifier ?? ''}
                onChange={setField('identifier')}
                aria-invalid={Boolean(err('identifier'))}
                aria-describedby={err('identifier') ? 'asset-identifier-error' : undefined}
              />
              {err('identifier') && (
                <p id="asset-identifier-error" className="field-error">
                  {err('identifier')}
                </p>
              )}
            </div>
          )}

          <div className="form-field">
            <label htmlFor="asset-type">
              Asset type <span className="req">*</span>
            </label>
            <input
              id="asset-type"
              value={values.assetType ?? ''}
              onChange={setField('assetType')}
              placeholder="e.g. PROJECTOR_SET"
              aria-invalid={Boolean(err('assetType'))}
              aria-describedby={err('assetType') ? 'asset-type-error' : undefined}
            />
            {err('assetType') && (
              <p id="asset-type-error" className="field-error">
                {err('assetType')}
              </p>
            )}
          </div>
        </div>

        {isEditing ? (
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="asset-dept-ro">Owning department</label>
              <input id="asset-dept-ro" value={currentDepartmentName ?? ''} readOnly disabled />
            </div>
            <div className="form-field">
              <label htmlFor="asset-campus-ro">Campus</label>
              <input id="asset-campus-ro" value={currentCampusName ?? ''} readOnly disabled />
            </div>
          </div>
        ) : (
          <div className="form-field">
            <label htmlFor="asset-campus">
              Campus &amp; owning department <span className="req">*</span>
            </label>
            <div className="two-step">
              <select
                id="asset-campus"
                aria-label="Campus"
                value={campusSel}
                onChange={(e) => {
                  setCampusSel(e.target.value);
                  setValues((v) => ({ ...v, campusId: e.target.value, owningDepartmentId: '' }));
                }}
                aria-invalid={Boolean(err('campusId'))}
              >
                <option value="">Select campus…</option>
                {campusRows.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              <select
                id="asset-department"
                aria-label="Owning department"
                value={values.owningDepartmentId ?? ''}
                onChange={setField('owningDepartmentId')}
                disabled={!campusSel || departments.isLoading}
                aria-invalid={Boolean(err('owningDepartmentId'))}
              >
                <option value="">Select department…</option>
                {deptRows.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>
            {err('campusId') && <p className="field-error">{err('campusId')}</p>}
            {err('owningDepartmentId') && <p className="field-error">{err('owningDepartmentId')}</p>}
          </div>
        )}

        <fieldset className="form-field">
          <legend>Availability windows</legend>
          {windows.length === 0 && <p className="table-empty">No windows.</p>}
          {windows.map((row, i) => (
            <div className="window-row" key={i}>
              <select
                aria-label={`Window ${i + 1} day`}
                value={row.dayOfWeek}
                onChange={setWindowField(i, 'dayOfWeek')}
                aria-invalid={Boolean(winErr(i, 'dayOfWeek'))}
              >
                <option value="">Day…</option>
                {DAYS_OF_WEEK.map((d) => (
                  <option key={d} value={d}>
                    {d.charAt(0) + d.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
              <input
                type="time"
                aria-label={`Window ${i + 1} start time`}
                value={row.startTime}
                onChange={setWindowField(i, 'startTime')}
                aria-invalid={Boolean(winErr(i, 'startTime'))}
              />
              <input
                type="time"
                aria-label={`Window ${i + 1} end time`}
                value={row.endTime}
                onChange={setWindowField(i, 'endTime')}
                aria-invalid={Boolean(winErr(i, 'endTime'))}
              />
              <button
                type="button"
                className="icon-btn icon-btn--danger"
                aria-label={`Remove window ${i + 1}`}
                onClick={() => removeWindow(i)}
              >
                Remove
              </button>
              {(winErr(i, 'dayOfWeek') || winErr(i, 'startTime') || winErr(i, 'endTime')) && (
                <p className="field-error window-error">
                  {winErr(i, 'dayOfWeek') || winErr(i, 'startTime') || winErr(i, 'endTime')}
                </p>
              )}
            </div>
          ))}
          <button type="button" className="btn" onClick={addWindow}>
            Add window
          </button>
        </fieldset>

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

AssetFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']).isRequired,
  initialValues: PropTypes.object.isRequired,
  currentDepartmentName: PropTypes.string,
  currentCampusName: PropTypes.string,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

AssetFormModal.defaultProps = {
  currentDepartmentName: '',
  currentCampusName: '',
};
