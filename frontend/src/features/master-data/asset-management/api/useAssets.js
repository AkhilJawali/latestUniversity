import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-435 §5.1 — CRUD hooks for schedulable assets against the verified A4-7 route
// /api/v1/assets (apiClient baseURL is /api/v1). The list is paginated ({data, meta})
// and filtered by campusId / departmentId / assetType (server-side, PD-3). Availability
// windows are sent inline in the create/update body (whole-set replacement, PD-5).
const BASE = '/assets';

const keys = {
  list: (params) => ['master-data', 'assets', 'list', params],
};

export function useAssets(params) {
  return useQuery({
    queryKey: keys.list(params),
    queryFn: async () => (await apiClient.get(BASE, { params })).data,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'assets', 'list'] }),
  });
}

export function useUpdateAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'assets', 'list'] }),
  });
}

export function useDeleteAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'assets', 'list'] }),
  });
}
