import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { actorHeaders } from '@/features/scheduling/approval/lib/approval-flow';
import { apiClient } from '@/lib/api-client';

// Approval workflow (A4-19: /api/v1/approvals) and publication (A4-21:
// POST /api/v1/timetables/{draftId}/publish). Every mutation refreshes the approval
// queries, because a final approval also publishes the draft on the server.

const keys = {
  drafts: (departmentId) => ['approval', 'drafts', departmentId],
  workflow: (draftId) => ['approval', 'workflow', draftId],
  pipelines: () => ['approval', 'pipelines'],
};

/** Drafts of a department, newest first (bare TimetableDraftDto[]). */
export function useDepartmentDrafts(departmentId) {
  return useQuery({
    queryKey: keys.drafts(departmentId),
    enabled: departmentId != null,
    queryFn: async () => (await apiClient.get('/timetables', { params: { departmentId } })).data,
  });
}

/** Latest approval workflow of a draft with its history; null when never submitted. */
export function useDraftWorkflow(draftId) {
  return useQuery({
    queryKey: keys.workflow(draftId),
    enabled: draftId != null,
    queryFn: async () => {
      try {
        return (await apiClient.get(`/approvals/draft/${draftId}`)).data;
      } catch (err) {
        if (err?.response?.status === 404) return null;
        throw err;
      }
    },
  });
}

/** Approval pipelines with their ordered levels (Coordinator → HOD → Dean/Registrar). */
export function useApprovalPipelines() {
  return useQuery({
    queryKey: keys.pipelines(),
    staleTime: 5 * 60 * 1000,
    queryFn: async () => (await apiClient.get('/approvals/pipelines')).data,
  });
}

function useApprovalMutation(request) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: request,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['approval'] });
      qc.invalidateQueries({ queryKey: ['generation', 'draft'] });
    },
  });
}

export function useSubmitForApproval() {
  return useApprovalMutation(({ draftId, comments, actor }) =>
    apiClient
      .post('/approvals/submit', { draftId, comments: comments || undefined }, actorHeaders(actor))
      .then((r) => r.data),
  );
}

export function useApproveDraft() {
  return useApprovalMutation(({ workflowId, comments, actor }) =>
    apiClient
      .post(`/approvals/${workflowId}/approve`, { comments: comments || undefined }, actorHeaders(actor))
      .then((r) => r.data),
  );
}

export function useRejectDraft() {
  return useApprovalMutation(({ workflowId, rejectionReason, comments, actor }) =>
    apiClient
      .post(
        `/approvals/${workflowId}/reject`,
        { rejectionReason, comments: comments || undefined },
        actorHeaders(actor),
      )
      .then((r) => r.data),
  );
}

export function usePublishDraft() {
  return useApprovalMutation(({ draftId, actor }) =>
    apiClient.post(`/timetables/${draftId}/publish`, null, actorHeaders(actor)).then((r) => r.data),
  );
}
