import PropTypes from 'prop-types';

import {
  formatDateTime,
  levelStage,
  STAGE_LABELS,
  STEP_ACTION_LABELS,
} from '@/features/scheduling/approval/lib/approval-flow';

// The approval levels as a stepper (done / waiting / not yet) and the append-only history
// of who did what, when, and why.

export function ApprovalStepper({ levels, workflow }) {
  const ordered = [...(levels ?? [])].sort((a, b) => a.levelIndex - b.levelIndex);
  if (ordered.length === 0) return null;
  return (
    <ol className="approval-stepper" aria-label="Approval levels">
      {ordered.map((level) => {
        const stage = levelStage(level.levelIndex, workflow);
        return (
          <li
            key={level.levelIndex}
            className={`approval-step approval-step--${stage}`}
            aria-current={stage === 'current' ? 'step' : undefined}
          >
            <span className="approval-step__name">{level.levelName}</span>
            <span className="approval-step__stage">{STAGE_LABELS[stage]}</span>
          </li>
        );
      })}
    </ol>
  );
}

ApprovalStepper.propTypes = {
  levels: PropTypes.array,
  workflow: PropTypes.object,
};

export function ApprovalHistory({ steps }) {
  if (!steps?.length) return null;
  return (
    <div className="table-scroll">
      <table className="data-table approval-history">
        <caption>History</caption>
        <thead>
          <tr>
            <th scope="col">When</th>
            <th scope="col">Level</th>
            <th scope="col">Action</th>
            <th scope="col">By</th>
            <th scope="col">Reason / comments</th>
          </tr>
        </thead>
        <tbody>
          {steps.map((s) => (
            <tr key={s.id}>
              <td>{formatDateTime(s.actedAt)}</td>
              <td>{s.levelName}</td>
              <td>{STEP_ACTION_LABELS[s.action] ?? s.action}</td>
              <td>{s.actorUserId}</td>
              <td>
                {[s.rejectionReason && `Reason: ${s.rejectionReason}`, s.comments]
                  .filter(Boolean)
                  .join(' — ') || '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

ApprovalHistory.propTypes = {
  steps: PropTypes.array,
};
