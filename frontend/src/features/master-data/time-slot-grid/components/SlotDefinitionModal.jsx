import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { useAddSlot, useRemoveSlot } from '../api/useSlotDefinitions';
import { DAY_OPTIONS, SLOT_TYPES } from '../constants/time-slot-options';
import { slotDefinitionSchema, toSlotRequest } from '../schemas/time-slot-schemas';

// A4-445 §5.4 — add/edit a slot definition. Fields: start/end time, slotType,
// applicableDay ("All days" or a weekday override), optional label. There is no backend
// slot-edit endpoint (OQ-1), so "edit" is modelled as remove-then-add: on save the old
// slot is removed then the new values are added. If the add fails (e.g. 409 overlap), the
// modal stays open with the mapped error so the admin can correct and retry the add — no
// silent loss (§6 caveat). Native <dialog> gives focus trap + Escape.
const EMPTY = { startTime: '', endTime: '', slotType: 'TEACHING', applicableDay: '', label: '' };

export default function SlotDefinitionModal({
  open,
  mode,
  gridId,
  campusId,
  initialValues,
  onDone,
  onClose,
}) {
  const isEditing = mode === 'edit';
  const ref = useRef(null);
  const [values, setValues] = useState(initialValues ?? EMPTY);
  const [errors, setErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);

  const addMut = useAddSlot(gridId, campusId);
  const removeMut = useRemoveSlot(gridId, campusId);
  const isPending = addMut.isPending || removeMut.isPending;

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return undefined;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
    return undefined;
  }, [open]);

  useEffect(() => {
    if (open) {
      setValues(initialValues ?? EMPTY);
      setErrors({});
      setFormMessage(null);
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];

  const doAdd = (body) =>
    addMut.mutate(body, {
      onSuccess: () => onDone?.(isEditing ? 'Slot updated.' : 'Slot added.'),
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) setErrors(mapped.fields);
        else setFormMessage(mapped.message);
      },
    });

  const submit = (e) => {
    e.preventDefault();
    const result = validateWith(slotDefinitionSchema, values);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-5: no request on invalid input
    }
    setErrors({});
    setFormMessage(null);
    const body = toSlotRequest(result.data);

    if (isEditing) {
      // OQ-1 remove-then-add. Remove the old slot first; only add on success.
      removeMut.mutate(initialValues.id, {
        onSuccess: () => doAdd(body),
        onError: (error) => setFormMessage(mapApiError(error).message),
      });
    } else {
      doAdd(body);
    }
  };

  return (
    <dialog ref={ref} className="modal" onCancel={onClose} aria-label={isEditing ? 'Edit slot' : 'Add slot'}>
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">{isEditing ? 'Edit slot' : 'Add slot'}</h2>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="slot-start">
              Start time <span className="req">*</span>
            </label>
            <input
              id="slot-start"
              type="time"
              value={values.startTime}
              onChange={setField('startTime')}
              aria-invalid={Boolean(err('startTime'))}
              aria-describedby={err('startTime') ? 'slot-start-error' : undefined}
            />
            {err('startTime') && (
              <p id="slot-start-error" className="field-error">
                {err('startTime')}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="slot-end">
              End time <span className="req">*</span>
            </label>
            <input
              id="slot-end"
              type="time"
              value={values.endTime}
              onChange={setField('endTime')}
              aria-invalid={Boolean(err('endTime'))}
              aria-describedby={err('endTime') ? 'slot-end-error' : undefined}
            />
            {err('endTime') && (
              <p id="slot-end-error" className="field-error">
                {err('endTime')}
              </p>
            )}
          </div>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="slot-type">
              Type <span className="req">*</span>
            </label>
            <select
              id="slot-type"
              value={values.slotType}
              onChange={setField('slotType')}
              aria-invalid={Boolean(err('slotType'))}
              aria-describedby={err('slotType') ? 'slot-type-error' : undefined}
            >
              {SLOT_TYPES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
            {err('slotType') && (
              <p id="slot-type-error" className="field-error">
                {err('slotType')}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="slot-day">Day scope</label>
            <select id="slot-day" value={values.applicableDay} onChange={setField('applicableDay')}>
              {DAY_OPTIONS.map((d) => (
                <option key={d.value || 'all'} value={d.value}>
                  {d.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="slot-label">Label</label>
          <input
            id="slot-label"
            value={values.label}
            onChange={setField('label')}
            placeholder="Optional (e.g. Period 1)"
            aria-invalid={Boolean(err('label'))}
            aria-describedby={err('label') ? 'slot-label-error' : undefined}
          />
          {err('label') && (
            <p id="slot-label-error" className="field-error">
              {err('label')}
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
            {isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Add slot'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

SlotDefinitionModal.propTypes = {
  open: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['add', 'edit']).isRequired,
  gridId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  campusId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  initialValues: PropTypes.object,
  onDone: PropTypes.func,
  onClose: PropTypes.func.isRequired,
};
