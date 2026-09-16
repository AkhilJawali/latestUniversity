import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-430 §5.1 — CRUD hooks for rooms against the verified A4-6 route /api/v1/rooms
// (apiClient baseURL is /api/v1). The list is paginated ({data, meta}) and filtered by
// campusId / roomType / building / minCapacity / equipmentTag (server-side, PD-3).
const BASE = '/rooms';

const keys = {
  list: (params) => ['master-data', 'rooms', 'list', params],
};

export function useRooms(params) {
  return useQuery({
    queryKey: keys.list(params),
    queryFn: async () => (await apiClient.get(BASE, { params })).data,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'rooms', 'list'] }),
  });
}

export function useUpdateRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => (await apiClient.put(`${BASE}/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'rooms', 'list'] }),
  });
}

export function useDeleteRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'rooms', 'list'] }),
  });
}
