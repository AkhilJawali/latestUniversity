import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-445 §5.1 — slot management + per-day preview against the verified A4-10 routes.
// Slots live nested in the grid, so add/remove invalidate the grid-by-campus query. There
// is no slot-edit endpoint (OQ-1) — the UI models "edit" as remove-then-add in the modal.
const BASE = '/time-slots';

const gridKeys = {
  byCampus: (campusId) => ['master-data', 'time-slot-grid', 'campus', campusId],
};
const effectiveKey = (gridId, day) => ['master-data', 'time-slot-grid', 'effective', gridId, day];

// FR-3.3 — add a slot to the grid; refresh the grid (which carries the slot list).
export function useAddSlot(gridId, campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(`${BASE}/${gridId}/slots`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: gridKeys.byCampus(campusId) }),
  });
}

// FR-3.4 — remove a slot from the grid.
export function useRemoveSlot(gridId, campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (slotId) =>
      (await apiClient.delete(`${BASE}/${gridId}/slots/${slotId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: gridKeys.byCampus(campusId) }),
  });
}

// FR-3.6 — effective slots for one weekday (day-overrides merged over all-days slots).
// enabled only when a grid and a specific weekday are chosen (preview mode).
export function useEffectiveSlots(gridId, day) {
  return useQuery({
    queryKey: effectiveKey(gridId, day),
    enabled: gridId != null && Boolean(day),
    queryFn: async () =>
      (await apiClient.get(`${BASE}/${gridId}/effective`, { params: { day } })).data,
  });
}
