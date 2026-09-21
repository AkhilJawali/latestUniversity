/**
 * A4-430 §5.3 — Room create/edit modal.
 * Custom modal with equipment tags input and create-only code/campus fields.
 */
import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { ROOM_TYPES } from '../constants/room-options';
import {
  roomCreateSchema,
  roomEditSchema,
  parseTags,
  formatTags,
} from '../schemas/room-schemas';

/**
 * Field names rendered by each mode — used to check visibility of mapped errors.
 */
function renderedFieldNames(isEditing) {
  const common = ['name', 'capacity', 'roomType', 'equipmentTags', 'building', 'floor'];
  return isEditing ? common : [...common, 'code', 'campusId'];
}

export default function RoomFormModal({
  open,
  mode,
  initialValues,
  campuses,
  isPending,
  onSubmit,
  onClose,
}) {
  const isEditing = mode === 'edit';
  const ref = useRef(null);
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [formMessage, setFormMessage] = useState(null);
  const [tagInput, setTagInput] = useState('');

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return undefined;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
    return undefined;
  }, [open]);

  // Reset on open
  useEffect(() => {
    if (open) {
      setValues(initialValues);
      setErrors({});
      setFormMessage(null);
      setTagInput(formatTags(initialValues.equipmentTags));
    }
  }, [open, initialValues]);

  const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));

  const handleTagInput = (e) => {
    setTagInput(e.target.value);
    setValues((v) => ({ ...v, equipmentTags: parseTags(e.target.value) }));
  };

  const submit = (e) => {
    e.preventDefault();
    const schema = isEditing ? roomEditSchema : roomCreateSchema;
    const result = validateWith(schema, values);
    if (!result.success) {
      setErrors(result.errors);
      return; // AC-4: no request on invalid
    }
    setErrors({});
    setFormMessage(null);
    onSubmit(result.data, {
      onError: (error) => {
        const mapped = mapApiError(error);
        if (mapped.fields) {
          setErrors(mapped.fields);
          const rendered = renderedFieldNames(isEditing);
          const anyVisible = Object.keys(mapped.fields).some((k) => rendered.includes(k));
          if (!anyVisible) {
            setFormMessage('Could not save. Please check the values and try again.');
          }
        } else {
          setFormMessage(mapped.message);
        }
      },
    });
  };

  const close = () => {
    if (!isPending) onClose();
  };

  return (
    <dialog ref={ref} className="modal" onCancel={close} aria-label={isEditing ? 'Edit room' : 'Create room'}>
      <div className="modal-header">
        <h2 className="modal-title">{isEditing ? 'Edit Room' : 'Create Room'}</h2>
        <button type="button" className="modal-close" aria-label="Close" onClick={close} disabled={isPending}>
          ✕
        </button>
      </div>

      <form className="modal-body" onSubmit={submit}>
        {formMessage && (
          <p className="form-message form-message--error" role="alert">
            {formMessage}
          </p>
        )}

        <div className="form-field">
          <label htmlFor="room-name">Name *</label>
          <input
            id="room-name"
            type="text"
            value={values.name ?? ''}
            onChange={setField('name')}
            disabled={isPending}
            aria-describedby={errors.name ? 'room-name-error' : undefined}
          />
          {errors.name && (
            <p id="room-name-error" className="field-error" role="alert">
              {errors.name}
            </p>
          )}
        </div>

        {!isEditing && (
          <>
            <div className="form-field">
              <label htmlFor="room-code">Code *</label>
              <input
                id="room-code"
                type="text"
                value={values.code ?? ''}
                onChange={setField('code')}
                disabled={isPending}
                placeholder="e.g., LH-101"
                aria-describedby={errors.code ? 'room-code-error' : undefined}
              />
              {errors.code && (
                <p id="room-code-error" className="field-error" role="alert">
                  {errors.code}
                </p>
              )}
            </div>

            <div className="form-field">
              <label htmlFor="room-campus">Campus *</label>
              <select
                id="room-campus"
                value={values.campusId ?? ''}
                onChange={setField('campusId')}
                disabled={isPending}
                aria-describedby={errors.campusId ? 'room-campus-error' : undefined}
              >
                <option value="">Select campus</option>
                {campuses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              {errors.campusId && (
                <p id="room-campus-error" className="field-error" role="alert">
                  {errors.campusId}
                </p>
              )}
            </div>
          </>
        )}

        {isEditing && initialValues.code && (
          <div className="form-field">
            <label>Code</label>
            <input type="text" value={initialValues.code} disabled className="input--readonly" />
          </div>
        )}

        {isEditing && initialValues.campusName && (
          <div className="form-field">
            <label>Campus</label>
            <input type="text" value={initialValues.campusName} disabled className="input--readonly" />
          </div>
        )}

        <div className="form-field">
          <label htmlFor="room-capacity">Capacity *</label>
          <input
            id="room-capacity"
            type="number"
            min="1"
            value={values.capacity ?? ''}
            onChange={setField('capacity')}
            disabled={isPending}
            aria-describedby={errors.capacity ? 'room-capacity-error' : undefined}
          />
          {errors.capacity && (
            <p id="room-capacity-error" className="field-error" role="alert">
              {errors.capacity}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="room-type">Room Type *</label>
          <select
            id="room-type"
            value={values.roomType ?? ''}
            onChange={setField('roomType')}
            disabled={isPending}
            aria-describedby={errors.roomType ? 'room-type-error' : undefined}
          >
            <option value="">Select type</option>
            {ROOM_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
          {errors.roomType && (
            <p id="room-type-error" className="field-error" role="alert">
              {errors.roomType}
            </p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="room-tags">Equipment Tags</label>
          <input
            id="room-tags"
            type="text"
            value={tagInput}
            onChange={handleTagInput}
            disabled={isPending}
            placeholder="e.g., projector, computer_lab, whiteboard"
            aria-describedby="room-tags-hint"
          />
          <p id="room-tags-hint" className="field-hint">
            Separate tags with commas
          </p>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="room-building">Building</label>
            <input
              id="room-building"
              type="text"
              value={values.building ?? ''}
              onChange={setField('building')}
              disabled={isPending}
              aria-describedby={errors.building ? 'room-building-error' : undefined}
            />
            {errors.building && (
              <p id="room-building-error" className="field-error" role="alert">
                {errors.building}
              </p>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="room-floor">Floor</label>
            <input
              id="room-floor"
              type="text"
              value={values.floor ?? ''}
              onChange={setField('floor')}
              disabled={isPending}
              aria-describedby={errors.floor ? 'room-floor-error' : undefined}
            />
            {errors.floor && (
              <p id="room-floor-error" className="field-error" role="alert">
                {errors.floor}
              </p>
            )}
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="btn" onClick={close} disabled={isPending}>
            Cancel
          </button>
          <button type="submit" className="btn btn--primary" disabled={isPending}>
            {isPending ? 'Saving…' : isEditing ? 'Save' : 'Create'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

RoomFormModal.propTypes = {
  open: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']).isRequired,
  initialValues: PropTypes.shape({
    name: PropTypes.string,
    code: PropTypes.string,
    campusId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    campusName: PropTypes.string,
    capacity: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    roomType: PropTypes.string,
    equipmentTags: PropTypes.arrayOf(PropTypes.string),
    building: PropTypes.string,
    floor: PropTypes.string,
  }).isRequired,
  campuses: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
    })
  ).isRequired,
  isPending: PropTypes.bool.isRequired,
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
