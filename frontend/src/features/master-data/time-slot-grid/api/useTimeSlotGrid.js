import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-445 §5.1 — grid lifecycle against the verified A4-10 routes under
// /api/v1/time-slots (apiClient baseURL is /api/v1). One grid per campus. The grid load
// returns 404 when the campus has no grid; that is mapped to a null grid (empty state),
// not an error (PD-3). Grid = create + rename (name only) + delete; slots are managed
// via useSlotDefinitions.
const BASE = '/time-slots';

const keys = {
  byCampus: (campusId) => ['master-data', 'time-slot-grid', 'campus', campusId],
};

// FR-1.2 — load the campus grid; 404 => { data: null } (no grid yet), not a query error.
export function useGridByCampus(campusId) {
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

// FR-2.3 — create a grid for a campus (body: campusId, gridName, slots[]).
export function useCreateGrid(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-2.4 — rename the grid (name only; slots unaffected).
export function useRenameGrid(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, gridName }) =>
      (await apiClient.put(`${BASE}/${id}`, { gridName })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-2.5 — delete the grid (soft; cascades to its slots).
export function useDeleteGrid(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}
