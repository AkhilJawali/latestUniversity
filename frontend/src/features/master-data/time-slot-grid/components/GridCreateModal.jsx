import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { DAY_OPTIONS, SLOT_TYPES } from '../constants/time-slot-options';
import { gridCreateSchema, toSlotRequest } from '../schemas/time-slot-schemas';

// A4-445 §5.3 — create a grid: a grid name plus an in-modal slot list the admin builds
// before the first save. Client pre-checks ≥1 slot and ≥1 TEACHING (FR-4.2) via
// gridCreateSchema before submitting a single POST /time-slots with {campusId, gridName,
// slots[]}. Backend re-validates (day-aware overlap 409, no-teaching 422, start≥end 422),
// surfaced inline via mapApiError. Native <dialog> for focus trap + Escape.
const EMPTY_SLOT = { startTime: '', endTime: '', slotType: 'TEACHING', applicableDay: '', label: '' };

export default function GridCreateModal({ open, campusId, isPending, onSubmit, onClose }) {
  const ref = useRef(null);
  const [gridName, setGridName] = useState('');
  const [slots, setSlots] = useState([{ ...EMPTY_SLOT }]);
  const [errors, setErrors] = useState({});
  const [slotErrors, setSlotErrors] = useState({});
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
      setGridName('');
      setSlots([{ ...EMPTY_SLOT }]);
      setErrors({});
      setSlotErrors({});
      setFormMessage(null);
    }
  }, [open]);

  const addRow = () => setSlots((s) => [...s, { ...EMPTY_SLOT }]);
  const removeRow = (i) => setSlots((s) => s.filter((_, idx) => idx !== i));
  const setSlotField = (i, field) => (e) =>
    setSlots((s) => s.map((row, idx) => (idx === i ? { ...row, [field]: e.target.value } : row)));

  const submit = (e) => {
    e.preventDefault();
    const candidate = { gridName, slots };
    const result = validateWith(gridCreateSchema, candidate);
    if (!result.success) {
      // Split slot-row errors ("slots.0.endTime") from top-level errors.
      const top = {};
      const rows = {};
      Object.entries(result.errors).forEach(([k, msg]) => {
        const m = k.match(/^slots\.(\d+)\.(\w+)$/);
        if (m) {
          rows[m[1]] = { ...(rows[m[1]] ?? {}), [m[2]]: msg };
        } else {
          top[k] = msg;
        }
      });
      setErrors(top);
      setSlotErrors(rows);
      return; // AC-5: no request on invalid input
    }
    setErrors({});
    setSlotErrors({});
    setFormMessage(null);
    const body = {
      campusId: Number(campusId),
      gridName: result.data.gridName,
      slots: result.data.slots.map(toSlotRequest),
    };
    onSubmit(body, {
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) setErrors(mapped.fields);
        else setFormMessage(mapped.message);
      },
    });
  };

  const slotErr = (i, field) => slotErrors[i]?.[field];

  return (
    <dialog ref={ref} className="modal modal--wide" onCancel={onClose} aria-label="Create grid">
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">Create time-slot grid</h2>

        <div className="form-field">
          <label htmlFor="grid-name">
            Grid name <span className="req">*</span>
          </label>
          <input
            id="grid-name"
            value={gridName}
            onChange={(e) => setGridName(e.target.value)}
            placeholder="e.g. Main campus weekday grid"
            aria-invalid={Boolean(errors.gridName)}
            aria-describedby={errors.gridName ? 'grid-name-error' : undefined}
          />
          {errors.gridName && (
            <p id="grid-name-error" className="field-error">
              {errors.gridName}
            </p>
          )}
        </div>

        <fieldset className="form-field">
          <legend>
            Slots <span className="req">*</span> (at least one teaching slot)
          </legend>
          {errors.slots && <p className="field-error">{errors.slots}</p>}
          {slots.map((row, i) => (
            <div className="slot-row" key={i}>
              <input
                type="time"
                aria-label={`Slot ${i + 1} start time`}
                value={row.startTime}
                onChange={setSlotField(i, 'startTime')}
                aria-invalid={Boolean(slotErr(i, 'startTime'))}
              />
              <input
                type="time"
                aria-label={`Slot ${i + 1} end time`}
                value={row.endTime}
                onChange={setSlotField(i, 'endTime')}
                aria-invalid={Boolean(slotErr(i, 'endTime'))}
              />
              <select
                aria-label={`Slot ${i + 1} type`}
                value={row.slotType}
                onChange={setSlotField(i, 'slotType')}
              >
                {SLOT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
              <select
                aria-label={`Slot ${i + 1} day scope`}
                value={row.applicableDay}
                onChange={setSlotField(i, 'applicableDay')}
              >
                {DAY_OPTIONS.map((d) => (
                  <option key={d.value || 'all'} value={d.value}>
                    {d.label}
                  </option>
                ))}
              </select>
              <input
                aria-label={`Slot ${i + 1} label`}
                value={row.label}
                onChange={setSlotField(i, 'label')}
                placeholder="Label (optional)"
                aria-invalid={Boolean(slotErr(i, 'label'))}
              />
              <button
                type="button"
                className="icon-btn icon-btn--danger"
                aria-label={`Remove slot ${i + 1}`}
                onClick={() => removeRow(i)}
                disabled={slots.length === 1}
              >
                Remove
              </button>
              {(slotErr(i, 'startTime') || slotErr(i, 'endTime') || slotErr(i, 'label') || slotErr(i, 'slotType')) && (
                <p className="field-error slot-error">
                  {slotErr(i, 'startTime') ||
                    slotErr(i, 'endTime') ||
                    slotErr(i, 'slotType') ||
                    slotErr(i, 'label')}
                </p>
              )}
            </div>
          ))}
          <button type="button" className="btn" onClick={addRow}>
            Add slot
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
            {isPending ? 'Saving…' : 'Create grid'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

GridCreateModal.propTypes = {
  open: PropTypes.bool.isRequired,
  campusId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
