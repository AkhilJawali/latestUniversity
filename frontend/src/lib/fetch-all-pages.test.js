import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from './api-client';
import { fetchAllPages, LIST_PAGE_SIZE } from './fetch-all-pages';

describe('fetchAllPages', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('reads every page so records beyond the first page are returned', async () => {
    const pages = [
      { data: [{ id: 1 }, { id: 2 }], meta: { page: 0, totalPages: 2, totalElements: 3 } },
      { data: [{ id: 3 }], meta: { page: 1, totalPages: 2, totalElements: 3 } },
    ];
    const get = vi
      .spyOn(apiClient, 'get')
      .mockImplementation((_url, { params }) => Promise.resolve({ data: pages[params.page] }));

    const result = await fetchAllPages('/courses', { campusId: 7 });

    expect(result.data.map((c) => c.id)).toEqual([1, 2, 3]);
    expect(result.meta.totalElements).toBe(3);
    expect(get).toHaveBeenCalledTimes(2);
    expect(get).toHaveBeenLastCalledWith('/courses', {
      params: { campusId: 7, page: 1, size: LIST_PAGE_SIZE },
    });
  });

  it('stops after one request when the list is empty or has no paging info', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [] } });

    const result = await fetchAllPages('/campuses');

    expect(result.data).toEqual([]);
    expect(get).toHaveBeenCalledTimes(1);
  });
});
