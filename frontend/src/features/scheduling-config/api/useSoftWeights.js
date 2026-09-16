import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-340 section 5.3 — CRUD hooks for soft-constraint weights, against the real
// A4-390 backend route: /api/v1/scheduling-config/soft-constraint-weights
// (SoftConstraintWeightController @RequestMapping). apiClient baseURL is /api/v1.
const BASE = '/scheduling-config/soft-constraint-weights';

const keys = {
  list: (campusId) => ['scheduling-config', 'soft-weights', 'list', campusId],
};

export function useSoftWeights(campusId) {
  return useQuery({
    queryKey: keys.list(campusId),
    queryFn: () => fetchAllPages(BASE, { campusId }),
    enabled: campusId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateSoftWeight(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, { ...body, campusId })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}

export function useUpdateSoftWeight(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}

export function useDeleteSoftWeight(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}
