import PropTypes from 'prop-types';

// A4-15 §8 (FR-4 / AC-3, OQ-3) — shows candidate alternative slots for a conflicted
// session, derived client-side by probing empty cells with the same conflict-check. These
// are labelled "suggested (not engine-ranked)" because A4-16 has no alternatives endpoint
// yet. Clicking a candidate stages a move to that cell.
export default function AlternativesPopover({ open, loading, alternatives, onPick, onClose }) {
  if (!open) return null;
  return (
    <div className="alternatives-popover" role="dialog" aria-label="Suggested alternative slots">
      <div className="alternatives-popover__head">
        <h3>Suggested slots</h3>
        <button type="button" className="link-btn" onClick={onClose} aria-label="Close suggestions">
          Close
        </button>
      </div>
      <p className="alternatives-popover__note">Conflict-free candidates (not engine-ranked).</p>

      {loading && <p className="conflict-panel__empty">Finding alternatives…</p>}
      {!loading && (!alternatives || alternatives.length === 0) && (
        <p className="conflict-panel__empty">No conflict-free alternative found nearby.</p>
      )}
      {!loading && alternatives && alternatives.length > 0 && (
        <ul className="alternatives-list">
          {alternatives.map((alt) => (
            <li key={alt.key}>
              <button type="button" className="btn" onClick={() => onPick?.(alt)}>
                {alt.dayOfWeek.charAt(0) + alt.dayOfWeek.slice(1).toLowerCase()} · Slot{' '}
                {alt.slotDefinitionId}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

AlternativesPopover.propTypes = {
  open: PropTypes.bool,
  loading: PropTypes.bool,
  alternatives: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      dayOfWeek: PropTypes.string.isRequired,
      slotDefinitionId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    }),
  ),
  onPick: PropTypes.func,
  onClose: PropTypes.func,
};
