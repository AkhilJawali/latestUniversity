import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-340 section 5.3 — CRUD hooks for institution common slots, against the real
// A4-390 backend route: /api/v1/scheduling-config/common-slots
// (CommonSlotController @RequestMapping). apiClient baseURL is /api/v1.
const BASE = '/scheduling-config/common-slots';

const keys = {
  list: (campusId) => ['scheduling-config', 'common-slots', 'list', campusId],
};

export function useCommonSlots(campusId) {
  return useQuery({
    queryKey: keys.list(campusId),
    queryFn: () => fetchAllPages(BASE, { campusId }),
    enabled: campusId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCommonSlot(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, { ...body, campusId })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}

export function useUpdateCommonSlot(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}

export function useDeleteCommonSlot(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}
