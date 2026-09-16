import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-410 §5.4 / PD-2 — CRUD hooks for sections. Sections are NESTED under a batch
// and NOT paginated: list/create at /api/v1/batches/{batchId}/sections (returns
// { data: [...] }); get/update/delete at /api/v1/sections/{id}. Disabled until a
// batch is chosen.
const keys = {
  list: (batchId) => ['master-data', 'sections', 'list', batchId],
};

export function useSections(batchId) {
  return useQuery({
    queryKey: keys.list(batchId),
    queryFn: async () => (await apiClient.get(`/batches/${batchId}/sections`)).data,
    enabled: batchId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateSection(batchId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(`/batches/${batchId}/sections`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(batchId) }),
  });
}

export function useUpdateSection(batchId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`/sections/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(batchId) }),
  });
}

export function useDeleteSection(batchId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`/sections/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(batchId) }),
  });
}
