import PropTypes from 'prop-types';

import { useInfeasibility } from '@/features/scheduling/generation/api/useGeneration';

// A4-345 FR-7 — infeasibility report. Rendered by the page only when the request
// is INFEASIBLE (the backend returns 404 otherwise). All strings rendered as text.
export default function InfeasibilityReport({ requestId }) {
  const { data, isLoading, isError } = useInfeasibility(requestId, true);

  if (isError) {
    return (
      <section className="card" aria-label="Infeasibility report">
        <h2>Infeasibility Report</h2>
        <p className="form-error" role="alert">Could not load the infeasibility report. Please try again.</p>
      </section>
    );
  }

  if (isLoading || !data) {
    return (
      <section className="card" aria-label="Infeasibility report">
        <h2>Infeasibility Report</h2>
        <p className="muted">Loading…</p>
      </section>
    );
  }

  const conflicts = data.conflicts ?? [];

  return (
    <section className="card infeasibility-report" aria-label="Infeasibility report">
      <h2>Infeasibility Report</h2>
      <p className="infeasibility-summary">{data.summary}</p>
      {data.detectedAt && (
        <p className="muted">Detected at {new Date(data.detectedAt).toLocaleString()}</p>
      )}

      <h3>Conflicting Constraints</h3>
      {conflicts.length === 0 ? (
        <div className="empty-state">
          <p>No specific conflicts were recorded.</p>
        </div>
      ) : (
        <ul className="conflict-items">
          {conflicts.map((c) => (
            <li key={c.id} className="conflict-item">
              <p className="conflict-session">{c.affectedSessionDescription}</p>
              <p className="conflict-constraints">{c.conflictingConstraints}</p>
              <p className="conflict-explanation">{c.explanation}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

InfeasibilityReport.propTypes = {
  requestId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
};
