import PropTypes from 'prop-types';

import ApprovalActionForm from '@/features/scheduling/approval/components/ApprovalActionForm';
import { ApprovalHistory, ApprovalStepper } from '@/features/scheduling/approval/components/ApprovalProgress';
import StatusBadge from '@/features/scheduling/approval/components/StatusBadge';
import {
  draftActions,
  draftTitle,
  isFinalLevel,
  isIncomplete,
  workflowSummary,
} from '@/features/scheduling/approval/lib/approval-flow';

// Approval panel for the selected draft: where it is in Coordinator → HOD → Dean/Registrar,
// the buttons allowed right now, the inline confirm form, and the history.

function formProps(action, draft, workflow, levels) {
  switch (action) {
    case 'submit':
      return {
        title: 'Submit for approval',
        confirmLabel: 'Submit',
        warning: isIncomplete(draft)
          ? `This draft is incomplete: ${draft.totalSessionsPlaced} of ${draft.totalSessionsRequired} sessions are placed.`
          : undefined,
      };
    case 'approve':
      return {
        title: `Approve as ${workflow?.currentLevelName ?? 'reviewer'}`,
        confirmLabel: 'Approve',
        warning: isFinalLevel(workflow, levels)
          ? 'This is the final approval — the timetable will be published.'
          : undefined,
      };
    case 'reject':
      return { title: 'Reject', confirmLabel: 'Reject', requireReason: true, danger: true };
    case 'publish':
      return {
        title: 'Publish this timetable',
        confirmLabel: 'Publish',
        withComments: false,
        warning:
          'It becomes the live timetable for this department and semester. Any previously published version is replaced.',
      };
    default:
      return null;
  }
}

export default function WorkflowPanel({
  draft,
  workflow,
  levels,
  isLoading,
  isError,
  action,
  isPending,
  errorMessage,
  onAction,
  onConfirm,
  onCancel,
}) {
  const inReview = workflow?.state === 'IN_REVIEW';
  const draftButtons = draftActions(draft.status);
  const form = action ? formProps(action, draft, workflow, levels) : null;

  return (
    <section className="card workflow-panel" aria-label="Approval progress">
      <div className="workflow-head">
        <h2>{draftTitle(draft)}</h2>
        <StatusBadge status={draft.status} />
      </div>

      {isError && (
        <p className="form-error" role="alert">
          Could not load the approval details. Please try again.
        </p>
      )}
      {!isError && isLoading && <p className="approval-muted">Loading approval details…</p>}

      {!isError && !isLoading && (
        <>
          <ApprovalStepper levels={levels} workflow={workflow} />
          <p className="workflow-summary">{workflowSummary(draft, workflow)}</p>

          {!form && (
            <div className="workflow-buttons">
              {inReview && (
                <button type="button" className="btn btn--primary" onClick={() => onAction('approve')}>
                  Approve as {workflow.currentLevelName}
                </button>
              )}
              {inReview && (
                <button type="button" className="btn btn--danger" onClick={() => onAction('reject')}>
                  Reject
                </button>
              )}
              {draftButtons.includes('submit') && (
                <button type="button" className="btn btn--primary" onClick={() => onAction('submit')}>
                  Submit for approval
                </button>
              )}
              {draftButtons.includes('publish') && (
                <button type="button" className="btn btn--primary" onClick={() => onAction('publish')}>
                  Publish
                </button>
              )}
            </div>
          )}

          {form && (
            <ApprovalActionForm
              key={action}
              {...form}
              isPending={isPending}
              errorMessage={errorMessage}
              onConfirm={(values) => onConfirm(action, values)}
              onCancel={onCancel}
            />
          )}

          <ApprovalHistory steps={workflow?.steps} />
        </>
      )}
    </section>
  );
}

WorkflowPanel.propTypes = {
  draft: PropTypes.object.isRequired,
  workflow: PropTypes.object,
  levels: PropTypes.array,
  isLoading: PropTypes.bool,
  isError: PropTypes.bool,
  action: PropTypes.oneOf(['submit', 'approve', 'reject', 'publish']),
  isPending: PropTypes.bool,
  errorMessage: PropTypes.string,
  onAction: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};
