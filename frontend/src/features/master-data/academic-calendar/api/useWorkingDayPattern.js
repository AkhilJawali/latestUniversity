import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-440 §6 — per-campus working-day pattern. One pattern per campus (UC-3). The editor
// is create-or-edit (upsert) driven by the GET 404: no pattern yet => POST, else PUT
// (PD-A440-4). Backend base is /api/v1/academic-calendars/patterns.
const BASE = '/academic-calendars/patterns';

const keys = { byCampus: (campusId) => ['master-data', 'working-day-pattern', campusId] };

// FR-7.1 — load the existing pattern; a 404 means "no pattern yet" and resolves to
// { data: null } rather than surfacing as a query error (PD-A440-4).
export function useWorkingDayPattern(campusId) {
  return useQuery({
    queryKey: keys.byCampus(campusId),
    enabled: campusId != null,
    queryFn: async () => {
      try {
        return (await apiClient.get(`${BASE}/campus/${campusId}`)).data;
      } catch (err) {
        if (err?.response?.status === 404) return { data: null };
        throw err;
      }
    },
  });
}

// FR-7.2 — create when none exists (body includes campusId).
export function useCreatePattern(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-7.2 — update the existing pattern (body campusId is ignored by the backend).
export function useUpdatePattern(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.put(`${BASE}/campus/${campusId}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}
