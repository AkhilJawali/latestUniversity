import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-415 §5.4 — CRUD hooks for courses, against the verified A4-3 route
// /api/v1/courses (apiClient baseURL is /api/v1). Global list (no parent filter);
// filtering is client-side (PD-1).
const BASE = '/courses';

const keys = {
  list: () => ['master-data', 'courses', 'list'],
};

export function useCourses() {
  return useQuery({
    queryKey: keys.list(),
    queryFn: () => fetchAllPages(BASE),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCourse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list() }),
  });
}

export function useUpdateCourse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list() }),
  });
}

export function useDeleteCourse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list() }),
  });
}
