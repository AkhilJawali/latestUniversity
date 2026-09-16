import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { ROOM_TYPES } from '../constants/room-options';
import { formatTags, parseTags, roomCreateSchema, roomEditSchema } from '../schemas/room-schemas';

// A4-430 §5.3 (PD-2) — custom create/edit modal for a room. Bespoke because it needs a
// multi-value equipment-tags input and create-only code + campus fields (both immutable
// on edit — read-only for context). Runs the Zod schema on submit (no request on invalid
// — AC-4) and maps backend errors via mapApiError. Native <dialog> = focus trap + Escape.
export default function RoomFormModal({
  open,
  mode,
  initialValues,
  currentCampusName,
  campuses,
  isPending,
  onSubmit,
  onClose,
}) {
  const isEditing = mode === 'edit';
  const ref = useRef(null);
  const [values, setValues] = useState(initialValues);
  const [tagsText, setTagsText] = useState('');
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
      setValues(initialValues);
      setTagsText(formatTags(initialValues.equipmentTags));
      setErrors({});
      setFormMessage(null);
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const err = (name) => errors[name];

  const submit = (e) => {
    e.preventDefault();
    const schema = isEditing ? roomEditSchema : roomCreateSchema;
    const candidate = { ...values, equipmentTags: parseTags(tagsText) };
    const result = validateWith(schema, candidate);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-4: no request on invalid input
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

  const campusRows = campuses ?? [];

  return (
    <dialog
      ref={ref}
      className="modal"
      onCancel={onClose}
      aria-label={isEditing ? 'Edit room' : 'Add room'}
    >
      <form onSubmit={submit} noValidate>
        <h2 className="modal-title">{isEditing ? 'Edit room' : 'Add room'}</h2>

        <div className="form-field">
          <label htmlFor="room-name">
            Name <span className="req">*</span>
          </label>
          <input
            id="room-name"
            value={values.name ?? ''}
            onChange={setField('name')}
            aria-invalid={Boolean(err('name'))}
            aria-describedby={err('name') ? 'room-name-error' : undefined}
          />
          {err('name') && (
            <p id="room-name-error" className="field-error">
              {err('name')}
            </p>
          )}
        </div>

        {isEditing ? (
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="room-code-ro">Code</label>
              <input id="room-code-ro" value={values.code ?? ''} readOnly disabled />
            </div>
            <div className="form-field">
              <label htmlFor="room-campus-ro">Campus</label>
              <input id="room-campus-ro" value={currentCampusName ?? ''} readOnly disabled />
            </div>
          </div>
        ) : (
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="room-code">
                Code <span className="req">*</span>
              </label>
              <input
                id="room-code"
                value={values.code ?? ''}
                onChange={setField('code')}
                aria-invalid={Boolean(err('code'))}
                aria-describedby={err('code') ? 'room-code-error' : undefined}
              />
              {err('code') && (
                <p id="room-code-error" className="field-error">
                  {err('code')}
                </p>
              )}
            </div>
            <div className="form-field">
              <label htmlFor="room-campus">
                Campus <span className="req">*</span>
              </label>
              <select
                id="room-campus"
                value={values.campusId ?? ''}
                onChange={setField('campusId')}
                aria-invalid={Boolean(err('campusId'))}
                aria-describedby={err('campusId') ? 'room-campus-error' : undefined}
              >
                <option value="">Select…</option>
                {campusRows.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              {err('campusId') && (
                <p id="room-campus-error" className="field-error">
                  {err('campusId')}
                </p>
              )}
            </div>
          </div>
        )}

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="room-capacity">
              Capacity <span className="req">*</span>
            </label>
            <input
              id="room-capacity"
              type="number"
              min="1"
              value={values.capacity ?? ''}
              onChange={setField('capacity')}
              aria-invalid={Boolean(err('capacity'))}
              aria-describedby={err('capacity') ? 'room-capacity-error' : undefined}
            />
            {err('capacity') && (
              <p id="room-capacity-error" className="field-error">
                {err('capacity')}
              </p>
            )}
          </div>
          <div className="form-field">
            <label htmlFor="room-type">
              Room type <span className="req">*</span>
            </label>
            <select
              id="room-type"
              value={values.roomType ?? ''}
              onChange={setField('roomType')}
              aria-invalid={Boolean(err('roomType'))}
              aria-describedby={err('roomType') ? 'room-type-error' : undefined}
            >
              <option value="">Select…</option>
              {ROOM_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            {err('roomType') && (
              <p id="room-type-error" className="field-error">
                {err('roomType')}
              </p>
            )}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="room-tags">Equipment tags</label>
          <input
            id="room-tags"
            value={tagsText}
            onChange={(e) => setTagsText(e.target.value)}
            placeholder="e.g. projector, computer_lab, smart_board"
          />
          <p className="hint">Comma-separated tags.</p>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="room-building">Building</label>
            <input
              id="room-building"
              value={values.building ?? ''}
              onChange={setField('building')}
              aria-invalid={Boolean(err('building'))}
              aria-describedby={err('building') ? 'room-building-error' : undefined}
            />
            {err('building') && (
              <p id="room-building-error" className="field-error">
                {err('building')}
              </p>
            )}
          </div>
          <div className="form-field">
            <label htmlFor="room-floor">Floor</label>
            <input
              id="room-floor"
              value={values.floor ?? ''}
              onChange={setField('floor')}
              aria-invalid={Boolean(err('floor'))}
              aria-describedby={err('floor') ? 'room-floor-error' : undefined}
            />
            {err('floor') && (
              <p id="room-floor-error" className="field-error">
                {err('floor')}
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

RoomFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']).isRequired,
  initialValues: PropTypes.object.isRequired,
  currentCampusName: PropTypes.string,
  campuses: PropTypes.array,
  isPending: PropTypes.bool,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

RoomFormModal.defaultProps = {
  currentCampusName: '',
  campuses: [],
};
