import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-420 §5.1 — CRUD hooks for faculty against the verified A4-4 route /api/v1/faculty
// (apiClient baseURL is /api/v1). The list is paginated ({data, meta}) and filtered by
// exact departmentId / designation / competencyCourseId (PD-OQ2; no free-text search).
const BASE = '/faculty';

const keys = {
  list: (params) => ['master-data', 'faculty', 'list', params],
};

export function useFacultyList(params) {
  return useQuery({
    queryKey: keys.list(params),
    queryFn: () => fetchAllPages(BASE, params),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateFaculty() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'faculty', 'list'] }),
  });
}

export function useUpdateFaculty() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'faculty', 'list'] }),
  });
}

export function useDeleteFaculty() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'faculty', 'list'] }),
  });
}
