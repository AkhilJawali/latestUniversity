import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartment, useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import {
  useApprovalPipelines,
  useApproveDraft,
  useDepartmentDrafts,
  useDraftWorkflow,
  usePublishDraft,
  useRejectDraft,
  useSubmitForApproval,
} from '@/features/scheduling/approval/api/useApprovals';
import DraftList from '@/features/scheduling/approval/components/DraftList';
import WorkflowPanel from '@/features/scheduling/approval/components/WorkflowPanel';
import { DRAFT_STATUS_LABELS, draftTitle } from '@/features/scheduling/approval/lib/approval-flow';
import TimetableGrid from '@/features/scheduling/generation/components/TimetableGrid';
import { mapApiError } from '@/lib/api-error';

import './approval.css';
// The weekly grid carries its own styles (.timetable-grid / .tt-*), which live with the
// generation feature that first used it.
import '@/features/scheduling/generation/generation.css';

// Approvals: pick a department's timetable draft, submit it, approve or reject it at each
// level (Coordinator → HOD → Dean/Registrar) and publish it. The selected department and
// draft live in the URL (?departmentId=&draftId=) so the generation page can link here.

const ACTOR_KEY = 'utms.approvals.actor';

const toId = (value) => (value ? Number(value) : null);

function readActor() {
  try {
    return window.localStorage.getItem(ACTOR_KEY) ?? '';
  } catch {
    return '';
  }
}

function writeActor(value) {
  try {
    window.localStorage.setItem(ACTOR_KEY, value);
  } catch {
    // storage unavailable — the name simply isn't remembered
  }
}

const successText = {
  submit: (w) => `Sent for approval. It is now waiting for ${w.currentLevelName}.`,
  approve: (w) =>
    w.state === 'APPROVED'
      ? 'Final approval given. The timetable is published automatically — if its status still says Approved, click Publish.'
      : `Approved. It now waits for ${w.currentLevelName}.`,
  reject: (w) =>
    w.state === 'REJECTED_RETURNED'
      ? 'Rejected and returned to the coordinator.'
      : `Rejected and sent back to ${w.currentLevelName}.`,
  publish: (r) =>
    `Published. ${r.affectedFacultyCount} teacher(s) and ${r.affectedBatchCount} batch(es) are affected.` +
    (r.supersededDraftId ? ' The previously published version was replaced.' : ''),
};

export default function ApprovalsPage() {
  const [params, setParams] = useSearchParams();
  const departmentId = toId(params.get('departmentId'));
  const selectedDraftId = toId(params.get('draftId'));

  const [campusChoice, setCampusChoice] = useState('');
  const [actor, setActor] = useState(readActor);
  const [action, setAction] = useState(null);
  const [notice, setNotice] = useState(null);

  const campuses = useCampuses();
  const campusRows = campuses.data?.data ?? [];
  const department = useDepartment(departmentId);
  const campusId =
    campusChoice ||
    String(department.data?.data?.campusId ?? '') ||
    (campusRows.length === 1 ? String(campusRows[0].id) : '');
  const departments = useDepartments(campusId ? Number(campusId) : null);
  const departmentRows = departments.data?.data ?? [];

  const drafts = useDepartmentDrafts(departmentId);
  const draftRows = drafts.data ?? [];
  const selectedDraft = draftRows.find((d) => d.id === selectedDraftId) ?? null;
  const workflow = useDraftWorkflow(selectedDraft ? selectedDraft.id : null);
  const pipelines = useApprovalPipelines();
  const pipelineRows = pipelines.data ?? [];
  const pipeline =
    pipelineRows.find((p) => p.id === workflow.data?.pipelineId) ??
    pipelineRows.find((p) => p.isActive) ??
    null;

  const mutations = {
    submit: useSubmitForApproval(),
    approve: useApproveDraft(),
    reject: useRejectDraft(),
    publish: usePublishDraft(),
  };
  const active = action ? mutations[action] : null;

  const showDraft = (draftId, nextAction = null) => {
    if (nextAction) mutations[nextAction].reset();
    setNotice(null);
    setAction(nextAction);
    setParams({ departmentId: String(departmentId), draftId: String(draftId) });
  };

  const confirm = (type, { comments, rejectionReason }) => {
    const vars = {
      submit: { draftId: selectedDraft.id, comments },
      approve: { workflowId: workflow.data?.id, comments },
      reject: { workflowId: workflow.data?.id, rejectionReason, comments },
      publish: { draftId: selectedDraft.id },
    }[type];
    mutations[type].mutate(
      { ...vars, actor },
      {
        onSuccess: (data) => {
          setAction(null);
          setNotice(successText[type](data));
        },
      },
    );
  };

  return (
    <div className="approvals-page">
      <h1 className="page-title">Approvals</h1>
      <p className="page-subtitle">
        Send a generated timetable for approval, approve or reject it at each level, and publish it.
      </p>

      <section className="card approvals-filters">
        <div className="form-field">
          <label htmlFor="approvalCampus">Campus</label>
          <select
            id="approvalCampus"
            value={campusId}
            onChange={(e) => {
              setCampusChoice(e.target.value);
              setAction(null);
              setParams({});
            }}
          >
            <option value="">{campuses.isLoading ? 'Loading campuses…' : 'Select campus…'}</option>
            {campusRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.code} — {c.name}
              </option>
            ))}
          </select>
        </div>

        <div className="form-field">
          <label htmlFor="approvalDepartment">Department</label>
          <select
            id="approvalDepartment"
            value={departmentId ?? ''}
            disabled={!campusId}
            onChange={(e) => {
              setAction(null);
              setNotice(null);
              setParams(e.target.value ? { departmentId: e.target.value } : {});
            }}
          >
            <option value="">{campusId ? 'Select department…' : 'Select a campus first'}</option>
            {departmentRows.map((d) => (
              <option key={d.id} value={d.id}>
                {d.code} — {d.name}
              </option>
            ))}
          </select>
        </div>

        <div className="form-field">
          <label htmlFor="approvalActor">Acting as</label>
          <input
            id="approvalActor"
            type="text"
            maxLength={100}
            placeholder="e.g. Dr. Rao (HOD)"
            value={actor}
            onChange={(e) => {
              setActor(e.target.value);
              writeActor(e.target.value);
            }}
          />
          <p className="approval-hint">Shown in the approval history (there is no login yet).</p>
        </div>
      </section>

      {notice && (
        <p className="approval-notice" role="status">
          {notice}
        </p>
      )}

      {departmentId == null ? (
        <div className="empty-state">
          <p>Choose a campus and department to see its timetable drafts.</p>
        </div>
      ) : (
        <section className="card" aria-label="Timetable drafts">
          <h2>Timetable drafts</h2>
          {drafts.isError && (
            <p className="form-error" role="alert">
              Could not load drafts. Please try again.
            </p>
          )}
          {drafts.isLoading && <p className="approval-muted">Loading drafts…</p>}
          {!drafts.isLoading && !drafts.isError && draftRows.length === 0 && (
            <div className="empty-state">
              <p>No timetable has been generated for this department yet.</p>
            </div>
          )}
          {draftRows.length > 0 && (
            <DraftList
              drafts={draftRows}
              selectedDraftId={selectedDraft?.id ?? null}
              onSelect={(id) => showDraft(id)}
              onAction={(id, type) => showDraft(id, type)}
            />
          )}
        </section>
      )}

      {selectedDraft && (
        <WorkflowPanel
          draft={selectedDraft}
          workflow={workflow.data ?? null}
          levels={pipeline?.levels ?? []}
          isLoading={workflow.isLoading || pipelines.isLoading}
          isError={workflow.isError}
          action={action}
          isPending={active?.isPending ?? false}
          errorMessage={active?.isError ? mapApiError(active.error).message : undefined}
          onAction={(type) => {
            mutations[type].reset();
            setNotice(null);
            setAction(type);
          }}
          onConfirm={confirm}
          onCancel={() => setAction(null)}
        />
      )}

      {/* The timetable being reviewed. Reviewers need to see what they are approving, and
          after final approval this is the approved/published timetable itself. */}
      {selectedDraft && (
        <>
          <p className="approval-muted" role="status">
            Showing {draftTitle(selectedDraft)} —{' '}
            {DRAFT_STATUS_LABELS[selectedDraft.status] ?? selectedDraft.status}.
          </p>
          <TimetableGrid draftId={selectedDraft.id} departmentId={departmentId} />
        </>
      )}
    </div>
  );
}
