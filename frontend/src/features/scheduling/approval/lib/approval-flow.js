// Pure helpers for the approvals screen: labels, which actions a draft offers, the stage
// of each approval level for the progress stepper, and the plain-language summary.

export const DRAFT_STATUS_LABELS = {
  DRAFT: 'Draft',
  UNDER_REVIEW: 'Under review',
  APPROVED: 'Approved',
  PUBLISHED: 'Published',
  SUPERSEDED: 'Superseded',
};

export const STEP_ACTION_LABELS = {
  SUBMITTED: 'Submitted',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

export const STAGE_LABELS = { done: 'Done', current: 'Waiting', upcoming: 'Not yet' };

/** "Version 3 · ODD 2024-25" — a draft named without its database id. */
export const draftTitle = (draft) =>
  draft ? `Version ${draft.version} · ${draft.semester} ${draft.academicYear}` : '';

/** True when the draft could not place every required session (e.g. a TIMED_OUT run). */
export const isIncomplete = (draft) =>
  draft != null &&
  draft.totalSessionsRequired != null &&
  (draft.totalSessionsPlaced ?? 0) < draft.totalSessionsRequired;

/** Draft-level buttons by draft status (approve/reject depend on the workflow instead). */
export function draftActions(status) {
  if (status === 'DRAFT') return ['submit'];
  if (status === 'APPROVED') return ['publish'];
  return [];
}

/** Stage of one pipeline level for the stepper: 'done' | 'current' | 'upcoming'. */
export function levelStage(levelIndex, workflow) {
  if (!workflow) return 'upcoming';
  if (workflow.state === 'APPROVED') return 'done';
  if (workflow.state !== 'IN_REVIEW') return 'upcoming';
  if (levelIndex < workflow.currentLevelIndex) return 'done';
  if (levelIndex === workflow.currentLevelIndex) return 'current';
  return 'upcoming';
}

/** True when approving now is the last level (approval then publishes the timetable). */
export function isFinalLevel(workflow, levels = []) {
  if (!workflow || levels.length === 0) return false;
  const maxIndex = Math.max(...levels.map((l) => l.levelIndex));
  return workflow.currentLevelIndex === maxIndex;
}

export function workflowSummary(draft, workflow) {
  if (draft.status === 'PUBLISHED') return 'Approved and published. This is the live timetable.';
  if (draft.status === 'SUPERSEDED') return 'A newer version replaced this draft.';
  if (!workflow) return 'Not sent for approval yet.';
  switch (workflow.state) {
    case 'IN_REVIEW':
      return `Waiting for ${workflow.currentLevelName} to approve or reject.`;
    case 'APPROVED':
      return 'Fully approved. Publish it to make it the live timetable.';
    case 'REJECTED_RETURNED':
      return 'Rejected and returned to the coordinator. Fix it (or generate again) and submit again.';
    default:
      return workflow.state;
  }
}

/**
 * Request config carrying the acting person's name as X-User-Id (there is no login yet).
 * Only printable ASCII is kept, because HTTP header values cannot carry other characters.
 */
export function actorHeaders(actor) {
  const clean = (actor ?? '').replace(/[^\x20-\x7E]/g, '').trim();
  return clean ? { headers: { 'X-User-Id': clean } } : undefined;
}

export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}
