import { apiClient } from '@/lib/api-client';

// Backend list endpoints are paginated (Spring's default page size is 20). Screens and
// pick-lists that show a whole list without pagination controls must read every page,
// otherwise the 21st record is created but never shown. Resolves to the same
// { data, meta } envelope a single page returns, so callers keep reading `.data`.
export const LIST_PAGE_SIZE = 100;

export async function fetchAllPages(url, params = {}) {
  const items = [];
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const { data } = await apiClient.get(url, {
      params: { ...params, page, size: LIST_PAGE_SIZE },
    });
    items.push(...(data?.data ?? []));
    totalPages = data?.meta?.totalPages ?? 0;
    page += 1;
  }
  return {
    data: items,
    meta: {
      page: 0,
      size: items.length,
      totalElements: items.length,
      totalPages: items.length > 0 ? 1 : 0,
    },
  };
}
