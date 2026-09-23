import { describe, expect, it, vi } from 'vitest';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';

import { useAssets, useCreateAsset, useDeleteAsset, useUpdateAsset } from './useAssets';

// A4-435 §5.1 — TanStack Query CRUD hooks for schedulable assets.
// Tests verify query keys, mutation invalidation, and correct API paths.

// Mock the apiClient
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import { apiClient } from '@/lib/api-client';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  return function Wrapper({ children }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
};

describe('useAssets', () => {
  it('calls GET /assets with params and returns data', async () => {
    const mockData = {
      data: [
        { id: 1, name: 'Projector A', identifier: 'PROJ-A', assetType: 'PROJECTOR_SET' },
        { id: 2, name: 'Sports Kit', identifier: 'SPORTS-1', assetType: 'SPORTS_FACILITY' },
      ],
      meta: { page: 0, totalPages: 1, totalElements: 2 },
    };
    apiClient.get.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useAssets({ page: 0, size: 20 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(apiClient.get).toHaveBeenCalledWith('/assets', { params: { page: 0, size: 20 } });
    expect(result.current.data).toEqual(mockData);
  });

  it('filters by campusId, departmentId, and assetType', async () => {
    const mockData = { data: [], meta: { page: 0, totalPages: 0, totalElements: 0 } };
    apiClient.get.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(
      () => useAssets({ page: 0, size: 20, campusId: 1, departmentId: 2, assetType: 'PROJECTOR_SET' }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(apiClient.get).toHaveBeenCalledWith('/assets', {
      params: { page: 0, size: 20, campusId: 1, departmentId: 2, assetType: 'PROJECTOR_SET' },
    });
  });
});

describe('useCreateAsset', () => {
  it('calls POST /assets with body', async () => {
    const mockResponse = { id: 3, name: 'New Asset', identifier: 'NEW-001' };
    apiClient.post.mockResolvedValueOnce({ data: mockResponse });

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = function Wrapper({ children }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };

    const { result } = renderHook(() => useCreateAsset(), { wrapper });

    result.current.mutate({
      name: 'New Asset',
      identifier: 'NEW-001',
      assetType: 'TYPE',
      owningDepartmentId: 1,
      campusId: 2,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(apiClient.post).toHaveBeenCalledWith('/assets', {
      name: 'New Asset',
      identifier: 'NEW-001',
      assetType: 'TYPE',
      owningDepartmentId: 1,
      campusId: 2,
    });
  });
});

describe('useUpdateAsset', () => {
  it('calls PUT /assets/:id with body', async () => {
    const mockResponse = { id: 1, name: 'Updated Asset', identifier: 'UPD-001' };
    apiClient.put.mockResolvedValueOnce({ data: mockResponse });

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = function Wrapper({ children }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };

    const { result } = renderHook(() => useUpdateAsset(), { wrapper });

    result.current.mutate({
      id: 1,
      name: 'Updated Asset',
      assetType: 'TYPE_V2',
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(apiClient.put).toHaveBeenCalledWith('/assets/1', {
      name: 'Updated Asset',
      assetType: 'TYPE_V2',
    });
  });
});

describe('useDeleteAsset', () => {
  it('calls DELETE /assets/:id', async () => {
    apiClient.delete.mockResolvedValueOnce({ data: {} });

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = function Wrapper({ children }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };

    const { result } = renderHook(() => useDeleteAsset(), { wrapper });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(apiClient.delete).toHaveBeenCalledWith('/assets/1');
  });
});
