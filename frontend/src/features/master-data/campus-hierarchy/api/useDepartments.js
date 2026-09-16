import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-410 §5.4 — CRUD hooks for departments, against the verified A4-2 route
// /api/v1/departments (apiClient baseURL is /api/v1). List is filtered by the
// selected parent campus id (PD-1). Query is disabled until a campus is chosen.
const BASE = '/departments';

const keys = {
  list: (campusId) => ['master-data', 'departments', 'list', campusId],
};

export function useDepartments(campusId) {
  return useQuery({
    queryKey: keys.list(campusId),
    queryFn: () => fetchAllPages(BASE, { campusId }),
    enabled: campusId != null,
    staleTime: 5 * 60 * 1000,
  });
}

/** Single department by id ({ data: department }), e.g. to find its campus. */
export function useDepartment(id) {
  return useQuery({
    queryKey: ['master-data', 'departments', 'byId', id],
    queryFn: async () => (await apiClient.get(`${BASE}/${id}`)).data,
    enabled: id != null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateDepartment(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, { ...body, campusId })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}

export function useUpdateDepartment(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}

export function useDeleteDepartment(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list(campusId) }),
  });
}
