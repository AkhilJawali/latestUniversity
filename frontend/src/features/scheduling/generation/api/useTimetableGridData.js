import { useQueries, useQuery } from '@tanstack/react-query';

import { useWorkingDayPattern } from '@/features/master-data/academic-calendar/api/useWorkingDayPattern';
import { useDepartment } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import { useGridByCampus } from '@/features/master-data/time-slot-grid/api/useTimeSlotGrid';
import { uniqueIds } from '@/features/scheduling/generation/lib/timetable-grid';
import { apiClient } from '@/lib/api-client';

// Data for the weekly timetable view of a draft. Placed sessions carry only IDs, so
// course / teacher / room names are resolved through the master-data "get by id"
// endpoints, and the rows/columns come from the draft's campus working-day pattern and
// time-slot grid (campus = the draft department's campus).

const LOOKUP_STALE_MS = 5 * 60 * 1000;
const SESSIONS_PAGE_SIZE = 500;

/** Every placed session of the draft (walks all pages). */
function useAllDraftSessions(draftId) {
  return useQuery({
    // Shares the ['generation', 'sessions', draftId] prefix, so lock/unlock
    // invalidations refresh this view too.
    queryKey: ['generation', 'sessions', draftId, 'all'],
    enabled: draftId != null,
    queryFn: async () => {
      const all = [];
      let page = 0;
      let totalPages = 1;
      while (page < totalPages) {
        const { data } = await apiClient.get(`/timetables/${draftId}/sessions`, {
          params: { page, size: SESSIONS_PAGE_SIZE },
        });
        all.push(...(data?.content ?? []));
        totalPages = data?.totalPages ?? 0;
        page += 1;
      }
      return all;
    },
  });
}

/** Resolves ids of one master-data resource to their DTOs: Map<id, dto>. */
function useLookup(resource, ids) {
  const results = useQueries({
    queries: ids.map((id) => ({
      queryKey: ['generation', 'lookup', resource, id],
      staleTime: LOOKUP_STALE_MS,
      queryFn: async () => (await apiClient.get(`/${resource}/${id}`)).data?.data ?? null,
    })),
  });
  const byId = new Map();
  results.forEach((r, i) => {
    if (r.data) byId.set(ids[i], r.data);
  });
  return { byId, isLoading: results.some((r) => r.isLoading) };
}

export function useTimetableGridData(draftId, departmentId) {
  const sessionsQuery = useAllDraftSessions(draftId);
  const departmentQuery = useDepartment(departmentId);
  const campusId = departmentQuery.data?.data?.campusId ?? null;
  const gridQuery = useGridByCampus(campusId);
  const patternQuery = useWorkingDayPattern(campusId);

  const sessions = sessionsQuery.data ?? [];
  const courses = useLookup('courses', uniqueIds(sessions, 'courseId'));
  const faculty = useLookup('faculty', uniqueIds(sessions, 'facultyId'));
  const rooms = useLookup('rooms', uniqueIds(sessions, 'roomId'));

  const queries = [sessionsQuery, departmentQuery, gridQuery, patternQuery];

  return {
    sessions,
    grid: gridQuery.data?.data ?? null,
    patternType: patternQuery.data?.data?.patternType ?? null,
    lookups: { courses: courses.byId, faculty: faculty.byId, rooms: rooms.byId },
    isLoading: queries.some((q) => q.isLoading),
    isError: queries.some((q) => q.isError),
    namesLoading: courses.isLoading || faculty.isLoading || rooms.isLoading,
  };
}
