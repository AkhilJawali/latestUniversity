import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { fetchAllPages } from '@/lib/fetch-all-pages';

// A4-410 §5.4 — CRUD hooks for campuses, against the verified A4-2 route
// /api/v1/campuses (apiClient baseURL is /api/v1). Campus is the top level,
// so its list takes no parent filter.
const BASE = '/campuses';

const keys = {
  list: () => ['master-data', 'campuses', 'list'],
};

export function useCampuses() {
  return useQuery({
    queryKey: keys.list(),
    queryFn: () => fetchAllPages(BASE),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCampus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list() }),
  });
}

export function useUpdateCampus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list() }),
  });
}

export function useDeleteCampus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.list() }),
  });
}
