/**
 * A4-430 §5.1 — TanStack Query hooks for room CRUD.
 * Server-side filtering + pagination (paged endpoint).
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

const BASE = '/rooms';

/**
 * Query keys for room cache.
 */
const keys = {
  list: (params) => ['master-data', 'rooms', 'list', params],
};

/**
 * Fetch rooms with server-side filters and pagination.
 * @param {Object} params - Filter params
 * @param {number} [params.page] - Page number (0-indexed)
 * @param {number} [params.size] - Page size
 * @param {number} [params.campusId] - Filter by campus
 * @param {string} [params.roomType] - Filter by room type
 * @param {string} [params.building] - Filter by building
 * @param {number} [params.minCapacity] - Minimum capacity
 * @param {string} [params.equipmentTag] - Equipment tag filter
 */
export function useRooms(params = {}) {
  return useQuery({
    queryKey: keys.list(params),
    queryFn: async () => {
      const { data } = await apiClient.get(BASE, { params });
      return data; // { data: [...], meta: { page, size, totalElements, totalPages } }
    },
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Create a new room.
 */
export function useCreateRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => {
      const { data } = await apiClient.post(BASE, body);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'rooms'] }),
  });
}

/**
 * Update an existing room.
 * Only mutable fields: name, capacity, roomType, equipmentTags, building, floor.
 */
export function useUpdateRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }) => {
      const { data } = await apiClient.put(`${BASE}/${id}`, body);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'rooms'] }),
  });
}

/**
 * Soft-delete a room.
 */
export function useDeleteRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => {
      await apiClient.delete(`${BASE}/${id}`);
      return id;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master-data', 'rooms'] }),
  });
}
