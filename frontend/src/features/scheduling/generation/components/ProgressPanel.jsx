import PropTypes from 'prop-types';

// A4-345 FR-2 / UI-1 — progress while a generation is IN_PROGRESS. Shows status,
// phase, best-so-far session count, and elapsed time. aria-live announces
// updates to screen readers (NFR-4). Not a black box.
const PHASE_LABELS = {
  LOADING_DATA: 'Loading data',
  DERIVING_SESSIONS: 'Deriving sessions',
  PROPAGATING: 'Propagating constraints',
  SOLVING: 'Solving',
  OPTIMIZING: 'Optimizing',
  STORING_RESULTS: 'Storing results',
};

export default function ProgressPanel({ status }) {
  if (!status) return null;

  const { phase, progress, bestSoFarCount, totalSessions, elapsedSeconds } = status;
  const hasProgress = typeof progress === 'number';

  return (
    <section className="card progress-panel" role="status" aria-live="polite">
      <h2>Generating…</h2>

      <dl className="progress-grid">
        <div>
          <dt>Phase</dt>
          <dd>{phase ? (PHASE_LABELS[phase] ?? phase) : '—'}</dd>
        </div>
        <div>
          <dt>Sessions placed (best so far)</dt>
          <dd>
            {bestSoFarCount ?? 0}
            {totalSessions != null ? ` / ${totalSessions}` : ''}
          </dd>
        </div>
        <div>
          <dt>Elapsed</dt>
          <dd>{elapsedSeconds != null ? `${elapsedSeconds}s` : '—'}</dd>
        </div>
      </dl>

      {hasProgress ? (
        <progress className="progress-bar" max="100" value={progress} aria-label="Generation progress">
          {progress}%
        </progress>
      ) : (
        <progress className="progress-bar" aria-label="Generation in progress" />
      )}
    </section>
  );
}

ProgressPanel.propTypes = {
  status: PropTypes.shape({
    phase: PropTypes.string,
    progress: PropTypes.number,
    bestSoFarCount: PropTypes.number,
    totalSessions: PropTypes.number,
    elapsedSeconds: PropTypes.number,
  }),
};
