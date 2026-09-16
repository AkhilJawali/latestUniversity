import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-420 §5.1 / FR-5 — campus-association sub-resource hooks against A4-4 routes.
// The GET was added as the OQ-1 backend follow-up so the UI can read the current set:
// GET    /faculty/{id}/campuses            -> { data: [{ campusId, name, code }, ...] }
// POST   /faculty/{id}/campuses/{campusId}
// DELETE /faculty/{id}/campuses/{campusId} (backend blocks removing the last one -> 422)
const keys = {
  list: (facultyId) => ['master-data', 'faculty', 'campuses', facultyId],
};

export function useFacultyCampuses(facultyId) {
  return useQuery({
    queryKey: keys.list(facultyId),
    queryFn: async () => (await apiClient.get(`/faculty/${facultyId}/campuses`)).data,
    enabled: facultyId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAddFacultyCampus(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (campusId) =>
      (await apiClient.post(`/faculty/${facultyId}/campuses/${campusId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}

export function useRemoveFacultyCampus(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (campusId) =>
      (await apiClient.delete(`/faculty/${facultyId}/campuses/${campusId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}
