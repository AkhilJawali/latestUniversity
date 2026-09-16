import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-425 §5.1 / FR-2 — availability window hooks against verified A4-5 routes, all nested
// under the faculty id:
// GET    /faculty/{facultyId}/availability            -> { data: [window, ...] } (plain list)
// POST   /faculty/{facultyId}/availability            (201 { data })
// PUT    /faculty/{facultyId}/availability/{windowId} (200 { data })
// DELETE /faculty/{facultyId}/availability/{windowId} (204)
const keys = {
  list: (facultyId) => ['master-data', 'faculty', 'availability', facultyId],
};

const base = (facultyId) => `/faculty/${facultyId}/availability`;

export function useAvailabilityWindows(facultyId) {
  return useQuery({
    queryKey: keys.list(facultyId),
    queryFn: async () => (await apiClient.get(base(facultyId))).data,
    enabled: facultyId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateWindow(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(base(facultyId), body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}

export function useUpdateWindow(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) =>
      (await apiClient.put(`${base(facultyId)}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}

export function useDeleteWindow(facultyId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (windowId) => (await apiClient.delete(`${base(facultyId)}/${windowId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(facultyId) }),
  });
}
