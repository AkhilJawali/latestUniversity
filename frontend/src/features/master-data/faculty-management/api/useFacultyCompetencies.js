import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-420 §5.1 / FR-4 — competency sub-resource hooks against verified A4-4 routes:
// GET    /faculty/{id}/competencies            -> { data: [courseId, ...] }
// POST   /faculty/{id}/competencies            (JSON array of course ids, bulk, <=50)
// DELETE /faculty/{id}/competencies/{courseId}
const keys = {
  list: (facultyId) => ['master-data', 'faculty', 'competencies', facultyId],
};

export function useFacultyCompetencies(facultyId) {
  return useQuery({
    queryKey: keys.list(facultyId),
    queryFn: async () => (await apiClient.get(`/faculty/${facultyId}/competencies`)).data,
    enabled: facultyId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAddCompetencies(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (courseIds) =>
      (await apiClient.post(`/faculty/${facultyId}/competencies`, courseIds)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}

export function useRemoveCompetency(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (courseId) =>
      (await apiClient.delete(`/faculty/${facultyId}/competencies/${courseId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}
