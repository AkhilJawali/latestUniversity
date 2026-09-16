import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-425 §5.1 / FR-4 — faculty scheduling preferences against verified A4-5 routes:
// GET /faculty/{facultyId}/preferences -> { data: {...} } (returns a default
//     NO_PREFERENCE object when none set — never 404)
// PUT /faculty/{facultyId}/preferences -> { data: {...} } (upsert; one per faculty)
const keys = {
  get: (facultyId) => ['master-data', 'faculty', 'preferences', facultyId],
};

export function useFacultyPreferences(facultyId) {
  return useQuery({
    queryKey: keys.get(facultyId),
    queryFn: async () => (await apiClient.get(`/faculty/${facultyId}/preferences`)).data,
    enabled: facultyId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSetPreferences(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.put(`/faculty/${facultyId}/preferences`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.get(facultyId) }),
  });
}
