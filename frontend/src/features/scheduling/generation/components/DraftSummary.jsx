import PropTypes from 'prop-types';

import { formatScorePercent, formatScoreRaw } from '@/features/scheduling/generation/lib/formatScore';

// A4-345 FR-3 — draft summary: feasibility and quality scores plus placement
// counts, version and generated time. Scores shown as a percentage with the
// raw value in the title attribute (PD-4).
export default function DraftSummary({ draft }) {
  if (!draft) return null;

  const {
    feasibilityScore,
    qualityScore,
    totalSessionsPlaced,
    totalSessionsRequired,
    version,
    violationCount,
    generatedAt,
  } = draft;

  return (
    <section className="card draft-summary" aria-label="Draft summary">
      <h2>Draft Summary</h2>
      <dl className="summary-grid">
        <div>
          <dt>Feasibility score</dt>
          <dd title={formatScoreRaw(feasibilityScore)}>{formatScorePercent(feasibilityScore)}</dd>
        </div>
        <div>
          <dt>Quality score</dt>
          <dd title={formatScoreRaw(qualityScore)}>{formatScorePercent(qualityScore)}</dd>
        </div>
        <div>
          <dt>Sessions placed</dt>
          <dd>
            {totalSessionsPlaced ?? 0}
            {totalSessionsRequired != null ? ` / ${totalSessionsRequired}` : ''}
          </dd>
        </div>
        <div>
          <dt>Soft-constraint violations</dt>
          <dd>{violationCount ?? 0}</dd>
        </div>
        <div>
          <dt>Version</dt>
          <dd>{version ?? '—'}</dd>
        </div>
        <div>
          <dt>Generated at</dt>
          <dd>{generatedAt ? new Date(generatedAt).toLocaleString() : '—'}</dd>
        </div>
      </dl>
    </section>
  );
}

DraftSummary.propTypes = {
  draft: PropTypes.shape({
    feasibilityScore: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    qualityScore: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    totalSessionsPlaced: PropTypes.number,
    totalSessionsRequired: PropTypes.number,
    version: PropTypes.number,
    violationCount: PropTypes.number,
    generatedAt: PropTypes.string,
  }),
};
