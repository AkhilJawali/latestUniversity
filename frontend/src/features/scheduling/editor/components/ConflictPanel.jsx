import PropTypes from 'prop-types';

// Conflict type labels (previously in conflict-types.js)
const CONFLICT_LABELS = {
  FACULTY_DOUBLE_BOOKING: 'Faculty double-booking',
  ROOM_DOUBLE_BOOKING: 'Room double-booking',
  BATCH_CLASH: 'Batch clash',
  ROOM_CAPACITY: 'Room capacity exceeded',
  FACULTY_DAILY_HOURS: 'Faculty daily hours exceeded',
  FACULTY_WEEKLY_HOURS: 'Faculty weekly hours exceeded',
  FACULTY_CONSECUTIVE_HOURS: 'Faculty consecutive hours exceeded',
  TRAVEL_TIME: 'Travel time violation',
  HARD_BLOCK: 'Hard block violation',
};

function conflictLabel(type) {
  return CONFLICT_LABELS[type] || type;
}

// A4-15 §8 (FR-8) — shows conflicts for the editor. Two groups: the last attempted
// placement's conflicts (top, actionable) and the full-draft conflict list. Each row shows
// the friendly type label + the engine's human-readable description, and focuses the
// involved (day, slot) cell on click. Renders only conflicts the engine returned (KD-A15-1);
// descriptions are escaped text (NFR-4).
function ConflictRow({ conflict, onFocusCell }) {
  const key =
    conflict.dayOfWeek && conflict.slotDefinitionId
      ? `${conflict.dayOfWeek}|${conflict.slotDefinitionId}`
      : null;
  return (
    <li className="conflict-row">
      <span className="conflict-row__type">{conflictLabel(conflict.type)}</span>
      <span className="conflict-row__desc">{conflict.description}</span>
      {key && (
        <button
          type="button"
          className="link-btn"
          onClick={() => onFocusCell?.(key)}
          aria-label={`Show ${conflictLabel(conflict.type)} on the grid`}
        >
          Show on grid
        </button>
      )}
    </li>
  );
}

ConflictRow.propTypes = {
  conflict: PropTypes.object.isRequired,
  onFocusCell: PropTypes.func,
};

export default function ConflictPanel({ lastPlacementConflicts, draftConflicts, isLoading, onFocusCell }) {
  return (
    <aside className="conflict-panel" aria-label="Conflicts">
      <section>
        <h3>Last placement</h3>
        {(!lastPlacementConflicts || lastPlacementConflicts.length === 0) && (
          <p className="conflict-panel__empty">No conflicts for the last placement.</p>
        )}
        {lastPlacementConflicts && lastPlacementConflicts.length > 0 && (
          <ul className="conflict-list">
            {lastPlacementConflicts.map((c, i) => (
              <ConflictRow key={`lp-${i}`} conflict={c} onFocusCell={onFocusCell} />
            ))}
          </ul>
        )}
      </section>

      <section>
        <h3>All draft conflicts</h3>
        {isLoading && <p className="conflict-panel__empty">Loading…</p>}
        {!isLoading && (!draftConflicts || draftConflicts.length === 0) && (
          <p className="conflict-panel__empty">No conflicts in the draft.</p>
        )}
        {!isLoading && draftConflicts && draftConflicts.length > 0 && (
          <ul className="conflict-list">
            {draftConflicts.map((c, i) => (
              <ConflictRow key={`dc-${i}`} conflict={c} onFocusCell={onFocusCell} />
            ))}
          </ul>
        )}
      </section>
    </aside>
  );
}

ConflictPanel.propTypes = {
  lastPlacementConflicts: PropTypes.array,
  draftConflicts: PropTypes.array,
  isLoading: PropTypes.bool,
  onFocusCell: PropTypes.func,
};
