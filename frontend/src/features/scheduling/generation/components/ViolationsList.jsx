import PropTypes from 'prop-types';

import { useViolations } from '@/features/scheduling/generation/api/useGeneration';

// A4-345 FR-5 — soft-constraint violations for a draft. Empty list renders an
// explicit empty state (FR-5.2). All backend strings rendered as text (NFR-3).
export default function ViolationsList({ draftId }) {
  const { data, isLoading, isError } = useViolations(draftId);

  if (isError) {
    return (
      <section className="card" aria-label="Soft-constraint violations">
        <h2>Soft-Constraint Violations</h2>
        <p className="form-error" role="alert">Could not load violations. Please try again.</p>
      </section>
    );
  }

  if (isLoading) {
    return (
      <section className="card" aria-label="Soft-constraint violations">
        <h2>Soft-Constraint Violations</h2>
        <p className="muted">Loading…</p>
      </section>
    );
  }

  const violations = data ?? [];

  return (
    <section className="card violations-list" aria-label="Soft-constraint violations">
      <h2>Soft-Constraint Violations</h2>

      {violations.length === 0 ? (
        <div className="empty-state">
          <p>No soft-constraint violations.</p>
        </div>
      ) : (
        <ul className="violation-items">
          {violations.map((v) => (
            <li key={v.id} className="violation-item">
              <span className="violation-type">{v.constraintType}</span>
              <span className="violation-entity">
                {v.affectedEntityType} #{v.affectedEntityId}
              </span>
              <p className="violation-desc">{v.description}</p>
              {v.relaxationReason && (
                <p className="violation-reason">Relaxation: {v.relaxationReason}</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

ViolationsList.propTypes = {
  draftId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
};
