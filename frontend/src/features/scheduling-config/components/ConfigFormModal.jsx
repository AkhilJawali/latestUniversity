import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { mapApiError } from '@/lib/api-error';
import { validateWith } from '../schemas/config-schemas';

// A4-340 section 5.5 — generic add/edit modal driven by a `fields` spec. Runs the
// Zod schema on submit; on failure sets field errors and sends NO request
// (FR-6.3 / AC-5). On mutation error, maps the backend envelope (400 -> per-field,
// 409 -> message) via mapApiError. Native <dialog> gives focus trap + Escape (NFR-3).
export default function ConfigFormModal({
  open,
  title,
  fields,
  schema,
  initialValues,
  isPending,
  onSubmit,
  onClose,
}) {
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

  // Reset the form whenever it (re)opens for a new target.
  useEffect(() => {
    if (open) {
      setValues(initialValues);
      setErrors({});
      setFormMessage(null);
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => {
    const val = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setValues((v) => ({ ...v, [name]: val }));
  };

  const submit = (e) => {
    e.preventDefault();
    const result = validateWith(schema, values);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-5: no request sent on invalid input
    }
    setErrors({});
    setFormMessage(null);
    onSubmit(result.data, {
      onError: (err) => {
        const mapped = mapApiError(err);
        if (mapped.fields) setErrors(mapped.fields);
        else setFormMessage(mapped.message);
      },
    });
  };

  return (
    <dialog ref={ref} className="modal" onCancel={onClose} aria-label={title}>
      <form onSubmit={submit} noValidate>
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button
            type="button"
            className="modal-close"
            aria-label="Close"
            onClick={onClose}
            disabled={isPending}
          >
            ✕
          </button>
        </div>

        <div className="modal-body">
        {fields.map((f) => {
          const errId = errors[f.name] ? `${f.name}-error` : undefined;
          return (
            <div
              className={f.type === 'checkbox' ? 'form-field form-field--checkbox' : 'form-field'}
              key={f.name}
            >
              <label htmlFor={f.name}>
                {f.label}
                {f.required && (
                  <span aria-hidden="true" className="req">
                    {' '}
                    *
                  </span>
                )}
              </label>

              {f.type === 'select' ? (
                <select
                  id={f.name}
                  name={f.name}
                  value={values[f.name] ?? ''}
                  onChange={setField(f.name)}
                  aria-invalid={Boolean(errors[f.name])}
                  aria-describedby={errId}
                >
                  <option value="">Select…</option>
                  {f.options.map((opt) => (
                    <option key={opt.value ?? opt} value={opt.value ?? opt}>
                      {opt.label ?? opt}
                    </option>
                  ))}
                </select>
              ) : f.type === 'checkbox' ? (
                <input
                  id={f.name}
                  name={f.name}
                  type="checkbox"
                  checked={Boolean(values[f.name])}
                  onChange={setField(f.name)}
                />
              ) : (
                <input
                  id={f.name}
                  name={f.name}
                  type={f.type ?? 'text'}
                  value={values[f.name] ?? ''}
                  onChange={setField(f.name)}
                  aria-invalid={Boolean(errors[f.name])}
                  aria-describedby={errId}
                />
              )}

              {errors[f.name] && (
                <p id={errId} className="field-error">
                  {errors[f.name]}
                </p>
              )}
            </div>
          );
        })}

        {formMessage && (
          <p className="form-error" role="alert">
            {formMessage}
          </p>
        )}
        </div>

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

ConfigFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
  fields: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      type: PropTypes.string,
      required: PropTypes.bool,
      options: PropTypes.array,
    }),
  ).isRequired,
  schema: PropTypes.object.isRequired,
  initialValues: PropTypes.object.isRequired,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
