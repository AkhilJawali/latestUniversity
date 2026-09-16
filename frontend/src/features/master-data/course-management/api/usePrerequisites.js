import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-415 §5.4 / FR-5 — prerequisite sub-resource hooks against verified A4-3 routes:
// GET  /courses/{id}/prerequisites            -> { data: [prereqCourseId, ...] }
// POST /courses/{id}/prerequisites/{prereqId}
// DELETE /courses/{id}/prerequisites/{prereqId}
const keys = {
  list: (courseId) => ['master-data', 'courses', 'prereqs', courseId],
};

export function usePrerequisites(courseId) {
  return useQuery({
    queryKey: keys.list(courseId),
    queryFn: async () => (await apiClient.get(`/courses/${courseId}/prerequisites`)).data,
    enabled: courseId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAddPrerequisite(courseId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (prerequisiteId) =>
      (await apiClient.post(`/courses/${courseId}/prerequisites/${prerequisiteId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(courseId) }),
  });
}

export function useRemovePrerequisite(courseId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (prerequisiteId) =>
      (await apiClient.delete(`/courses/${courseId}/prerequisites/${prerequisiteId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(courseId) }),
  });
}
