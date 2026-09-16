import PropTypes from 'prop-types';

import { dayScopeLabel, labelOf, SLOT_TYPES } from '../constants/time-slot-options';

// A4-445 §5.5 — the grid's slots rendered as a day timeline ordered by start time
// (start, end, duration [read-only from durationMinutes], type badge, day scope, label).
// Break/lunch rows are visually distinguished from teaching (PD-8, AC-3). Row actions
// (Edit / Remove) are shown for the grid's own slots; in per-day preview mode the source
// is the effective-slots list and actions are hidden (PD-9) since a new slot's day scope
// would be ambiguous there.
const trim = (t) => (t == null ? '' : String(t).slice(0, 5));

export default function SlotTimeline({ slots, preview, onEdit, onRemove }) {
  const sorted = [...(slots ?? [])].sort((a, b) => trim(a.startTime).localeCompare(trim(b.startTime)));

  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Start</th>
            <th scope="col">End</th>
            <th scope="col">Duration</th>
            <th scope="col">Type</th>
            <th scope="col">Day scope</th>
            <th scope="col">Label</th>
            {!preview && (
              <th scope="col" className="col-actions">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {sorted.length === 0 && (
            <tr>
              <td colSpan={preview ? 6 : 7} className="table-empty">
                No slots yet.
              </td>
            </tr>
          )}
          {sorted.map((s) => {
            const type = s.slotType;
            const rowClass =
              type === 'BREAK' ? 'slot-row--break' : type === 'LUNCH' ? 'slot-row--lunch' : '';
            const badgeClass =
              type === 'BREAK'
                ? 'badge badge--break'
                : type === 'LUNCH'
                  ? 'badge badge--lunch'
                  : 'badge badge--teaching';
            return (
              <tr key={s.id} className={rowClass}>
                <td>{trim(s.startTime)}</td>
                <td>{trim(s.endTime)}</td>
                <td>{s.durationMinutes != null ? `${s.durationMinutes} min` : ''}</td>
                <td>
                  <span className={badgeClass}>{labelOf(SLOT_TYPES, type)}</span>
                </td>
                <td>{dayScopeLabel(s.applicableDay)}</td>
                <td>{s.label}</td>
                {!preview && (
                  <td className="row-actions">
                    <button
                      type="button"
                      className="icon-btn"
                      aria-label={`Edit slot ${trim(s.startTime)}–${trim(s.endTime)}`}
                      onClick={() => onEdit(s)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="icon-btn icon-btn--danger"
                      aria-label={`Remove slot ${trim(s.startTime)}–${trim(s.endTime)}`}
                      onClick={() => onRemove(s)}
                    >
                      Remove
                    </button>
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

SlotTimeline.propTypes = {
  slots: PropTypes.array,
  preview: PropTypes.bool,
  onEdit: PropTypes.func,
  onRemove: PropTypes.func,
};
