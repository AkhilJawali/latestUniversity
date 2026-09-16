import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-15 §6 — editor data layer over the verified A4-16 conflict engine and the A4-11/A4-12
// session lock endpoints. Conflict endpoints return a BARE ConflictDto[] (no {data}
// envelope — verified in ConflictController). REST-first transport (PD-A15-2, OQ-4); the
// draft/sessions themselves are read via the existing A4-345 useGeneration hooks.
const draftBase = (draftId) => `/drafts/${draftId}`;
const ttBase = (draftId) => `/timetables/${draftId}`;

// FR-2/FR-3 — single proposed-placement conflict check. Returns ConflictDto[] (bare array).
export function useConflictCheck(draftId) {
  return useMutation({
    mutationFn: async (placement) =>
      (await apiClient.post(`${draftBase(draftId)}/conflict-check`, placement)).data,
  });
}

// FR-8 — full-draft conflict list (bare array).
export function useDraftConflicts(draftId) {
  return useQuery({
    queryKey: ['editor', 'draft-conflicts', draftId],
    queryFn: async () => (await apiClient.get(`${draftBase(draftId)}/conflicts`)).data,
    enabled: draftId != null,
  });
}

// FR-6 (OQ-2 interim) — advisory lock of the session being repositioned. Invalidates the
// A4-345 sessions query so the isLocked flag refreshes.
export function useLockSession(draftId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (sessionId) =>
      (await apiClient.post(`${ttBase(draftId)}/sessions/${sessionId}/lock`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['generation', 'sessions', draftId] }),
  });
}

// FR-6 (OQ-2 interim) — release the advisory lock on drop/cancel.
export function useUnlockSession(draftId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (sessionId) =>
      (await apiClient.post(`${ttBase(draftId)}/sessions/${sessionId}/unlock`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['generation', 'sessions', draftId] }),
  });
}
