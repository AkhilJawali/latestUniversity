import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-345 data layer — TanStack Query hooks over the existing A4-11 / A4-12
// scheduling endpoints. All calls go through the single shared apiClient
// (baseURL /api/v1). No component builds its own HTTP client (FR-9.1).

// Backend GenerationStatus enum. Terminal = every status except IN_PROGRESS.
export const TERMINAL_STATUSES = ['COMPLETED', 'TIMED_OUT', 'INFEASIBLE', 'FAILED', 'CANCELLED'];

export function isTerminal(status) {
  return TERMINAL_STATUSES.includes(status);
}

// Polling cadence while a generation is running (design PD-2).
const POLL_INTERVAL_MS = 2000;

/** FR-1: trigger a generation. Returns { requestId, status, statusUrl, ... }. */
export function useTriggerGeneration() {
  return useMutation({
    mutationFn: (body) => apiClient.post('/timetables/generate', body).then((r) => r.data),
  });
}

/**
 * FR-2: poll generation status. Polls every 2s while IN_PROGRESS and stops
 * once the status is terminal (refetchInterval returns false).
 */
export function useGenerationStatus(requestId) {
  return useQuery({
    queryKey: ['generation', 'status', requestId],
    queryFn: () =>
      apiClient.get(`/timetables/generate/${requestId}/status`).then((r) => r.data),
    enabled: requestId != null,
    refetchInterval: (query) =>
      isTerminal(query.state.data?.status) ? false : POLL_INTERVAL_MS,
  });
}

/** FR-3: draft summary (feasibility/quality scores, counts, version). */
export function useDraft(draftId) {
  return useQuery({
    queryKey: ['generation', 'draft', draftId],
    queryFn: () => apiClient.get(`/timetables/${draftId}`).then((r) => r.data),
    enabled: draftId != null,
  });
}

/**
 * FR-4: placed sessions, paginated and sortable. `sort` is a Spring sort
 * expression such as "dayOfWeek,asc". Previous page data is kept while the
 * next page loads to avoid a flash of empty table.
 */
export function useSessions(draftId, { page = 0, size = 20, sort } = {}) {
  return useQuery({
    queryKey: ['generation', 'sessions', draftId, page, size, sort],
    queryFn: () =>
      apiClient
        .get(`/timetables/${draftId}/sessions`, { params: { page, size, sort } })
        .then((r) => r.data),
    enabled: draftId != null,
    // v5: keep the previous page's rows visible while the next page loads
    // (avoids a flash of an empty table on page/sort changes).
    placeholderData: keepPreviousData,
  });
}

/** FR-5: soft-constraint violations for a draft. */
export function useViolations(draftId) {
  return useQuery({
    queryKey: ['generation', 'violations', draftId],
    queryFn: () => apiClient.get(`/timetables/${draftId}/violations`).then((r) => r.data),
    enabled: draftId != null,
  });
}

/** FR-6: unplaced sessions (only meaningful for a TIMED_OUT / partial draft). */
export function useUnplaced(draftId, enabled = true) {
  return useQuery({
    queryKey: ['generation', 'unplaced', draftId],
    queryFn: () => apiClient.get(`/timetables/${draftId}/unplaced`).then((r) => r.data),
    enabled: enabled && draftId != null,
  });
}

/**
 * FR-7: infeasibility report. The backend returns 404 unless the request is
 * INFEASIBLE, so the caller must only enable this when status === 'INFEASIBLE'
 * (design Section 8) to avoid a guaranteed 404.
 */
export function useInfeasibility(requestId, enabled = false) {
  return useQuery({
    queryKey: ['generation', 'infeasibility', requestId],
    queryFn: () =>
      apiClient.get(`/timetables/generate/${requestId}/infeasibility`).then((r) => r.data),
    enabled: enabled && requestId != null,
  });
}
