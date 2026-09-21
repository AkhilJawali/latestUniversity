import { describe, expect, it, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { useAssets, useCreateAsset, useUpdateAsset, useDeleteAsset } from './useAssets';

// A4-435 — API hooks unit tests for schedulable assets CRUD operations.

// Mock the apiClient
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPut = vi.fn();
const mockDelete = vi.fn();

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: (...args) => mockGet(...args),
    post: (...args) => mockPost(...args),
    put: (...args) => mockPut(...args),
    delete: (...args) => mockDelete(...args),
  },
}));

function createWrapper() {
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
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useAssets', () => {
  it('fetches assets with pagination params', async () => {
    const mockData = {
      data: [
        { id: 1, name: 'Projector A', identifier: 'PROJ-001' },
        { id: 2, name: 'Projector B', identifier: 'PROJ-002' },
      ],
      meta: { page: 0, totalPages: 1, totalElements: 2 },
    };
    mockGet.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useAssets({ page: 0, size: 20 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockGet).toHaveBeenCalledWith('/assets', { params: { page: 0, size: 20 } });
    expect(result.current.data).toEqual(mockData);
  });

  it('fetches assets with campus filter', async () => {
    const mockData = { data: [], meta: { page: 0, totalPages: 0, totalElements: 0 } };
    mockGet.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useAssets({ campusId: 1 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockGet).toHaveBeenCalledWith('/assets', { params: { campusId: 1 } });
  });

  it('fetches assets with department filter', async () => {
    const mockData = { data: [], meta: { page: 0, totalPages: 0, totalElements: 0 } };
    mockGet.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useAssets({ departmentId: 10 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockGet).toHaveBeenCalledWith('/assets', { params: { departmentId: 10 } });
  });

  it('fetches assets with assetType filter', async () => {
    const mockData = { data: [], meta: { page: 0, totalPages: 0, totalElements: 0 } };
    mockGet.mockResolvedValueOnce({ data: mockData });

    const { result } = renderHook(() => useAssets({ assetType: 'PROJECTOR_SET' }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockGet).toHaveBeenCalledWith('/assets', { params: { assetType: 'PROJECTOR_SET' } });
  });

  it('handles fetch error', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useAssets({}), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeInstanceOf(Error);
  });
});

describe('useCreateAsset', () => {
  it('creates an asset successfully', async () => {
    const newAsset = { name: 'New Projector', identifier: 'PROJ-003', assetType: 'PROJECTOR_SET' };
    const createdAsset = { id: 3, ...newAsset };
    mockPost.mockResolvedValueOnce({ data: createdAsset });

    const { result } = renderHook(() => useCreateAsset(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(newAsset);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockPost).toHaveBeenCalledWith('/assets', newAsset);
    expect(result.current.data).toEqual(createdAsset);
  });

  it('handles create error', async () => {
    mockPost.mockRejectedValueOnce(new Error('Validation failed'));

    const { result } = renderHook(() => useCreateAsset(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ name: 'Test' });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});

describe('useUpdateAsset', () => {
  it('updates an asset successfully', async () => {
    const updatedAsset = { id: 1, name: 'Updated Projector', assetType: 'PROJECTOR_SET' };
    mockPut.mockResolvedValueOnce({ data: updatedAsset });

    const { result } = renderHook(() => useUpdateAsset(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: 1, name: 'Updated Projector', assetType: 'PROJECTOR_SET' });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockPut).toHaveBeenCalledWith('/assets/1', {
      name: 'Updated Projector',
      assetType: 'PROJECTOR_SET',
    });
    expect(result.current.data).toEqual(updatedAsset);
  });

  it('handles update error', async () => {
    mockPut.mockRejectedValueOnce(new Error('Update failed'));

    const { result } = renderHook(() => useUpdateAsset(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: 1, name: 'Test' });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});

describe('useDeleteAsset', () => {
  it('deletes an asset successfully', async () => {
    mockDelete.mockResolvedValueOnce({ data: {} });

    const { result } = renderHook(() => useDeleteAsset(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(1);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockDelete).toHaveBeenCalledWith('/assets/1');
  });

  it('handles delete error', async () => {
    mockDelete.mockRejectedValueOnce(new Error('Delete failed'));

    const { result } = renderHook(() => useDeleteAsset(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(1);

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
