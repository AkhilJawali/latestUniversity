import PropTypes from 'prop-types';

// A4-345 UI-2 / FR-8 — banner for the terminal outcome. Status is conveyed by
// text (not color alone) per NFR-4; a modifier class adds colour as a secondary
// cue. Covers all five terminal GenerationStatus values.
const OUTCOMES = {
  COMPLETED: { label: 'Completed', tone: 'success', note: 'A complete draft was generated.' },
  TIMED_OUT: {
    label: 'Partial (timed out)',
    tone: 'warning',
    note: 'Generation hit the time limit. This is a partial draft — some sessions remain unplaced.',
  },
  INFEASIBLE: {
    label: 'Infeasible',
    tone: 'destructive',
    note: 'No valid timetable exists for the current constraints. See the conflicting constraints below.',
  },
  FAILED: {
    label: 'Failed',
    tone: 'destructive',
    note: 'Generation failed unexpectedly. Please try again.',
  },
  CANCELLED: {
    label: 'Cancelled',
    tone: 'muted',
    note: 'Generation was cancelled.',
  },
};

export default function OutcomeBanner({ status }) {
  const outcome = OUTCOMES[status];
  if (!outcome) return null;

  return (
    <div className={`outcome-banner outcome-banner--${outcome.tone}`} role="status">
      <span className="outcome-banner__label">{outcome.label}</span>
      <span className="outcome-banner__note">{outcome.note}</span>
    </div>
  );
}

OutcomeBanner.propTypes = {
  status: PropTypes.oneOf(['COMPLETED', 'TIMED_OUT', 'INFEASIBLE', 'FAILED', 'CANCELLED']),
};
