import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-410 §5.4 — CRUD hooks for batches, against the verified A4-2 route
// /api/v1/batches (apiClient baseURL is /api/v1). List is filtered by the
// selected parent program id (PD-1). Disabled until a program is chosen.
const BASE = '/batches';

const keys = {
  list: (programId) => ['master-data', 'batches', 'list', programId],
};

export function useBatches(programId) {
  return useQuery({
    queryKey: keys.list(programId),
    queryFn: () => fetchAllPages(BASE, { programId }),
    enabled: programId != null,
    staleTime: 5 * 60 * 1000,
  });
}

// Single batch by id — used by the sections page to read the batch strength for
// the section sub-strength total validation.
export function useBatch(batchId) {
  return useQuery({
    queryKey: ['master-data', 'batches', 'byId', batchId],
    queryFn: async () => (await apiClient.get(`${BASE}/${batchId}`)).data,
    enabled: batchId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateBatch(programId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, { ...body, programId })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(programId) }),
  });
}

export function useUpdateBatch(programId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(programId) }),
  });
}

export function useDeleteBatch(programId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(programId) }),
  });
}
