import PropTypes from 'prop-types';

import { conflictLabel } from '../constants/conflict-types';

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
