import { useMutation, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-415 §5.4 / FR-6 — cross-listing sub-resource hooks against verified A4-3 routes:
// POST   /courses/{id}/cross-listings/{departmentId}
// DELETE /courses/{id}/cross-listings/{departmentId}
// There is no cross-listing LIST endpoint; the course's isCrossListed flag reflects
// state, so on success we invalidate the course list to refresh it (FR-6.2).
const courseListKey = ['master-data', 'courses', 'list'];

export function useAddCrossListing(courseId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (departmentId) =>
      (await apiClient.post(`/courses/${courseId}/cross-listings/${departmentId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: courseListKey }),
  });
}

export function useRemoveCrossListing(courseId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (departmentId) =>
      (await apiClient.delete(`/courses/${courseId}/cross-listings/${departmentId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: courseListKey }),
  });
}
