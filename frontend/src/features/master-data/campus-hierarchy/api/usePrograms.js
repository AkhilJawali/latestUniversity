import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-410 §5.4 — CRUD hooks for programs, against the verified A4-2 route
// /api/v1/programs (apiClient baseURL is /api/v1). List is filtered by the
// selected parent department id (PD-1). Disabled until a department is chosen.
const BASE = '/programs';

const keys = {
  list: (departmentId) => ['master-data', 'programs', 'list', departmentId],
};

export function usePrograms(departmentId) {
  return useQuery({
    queryKey: keys.list(departmentId),
    queryFn: () => fetchAllPages(BASE, { departmentId }),
    enabled: departmentId != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateProgram(departmentId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, { ...body, departmentId })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(departmentId) }),
  });
}

export function useUpdateProgram(departmentId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(departmentId) }),
  });
}

export function useDeleteProgram(departmentId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(departmentId) }),
  });
}
