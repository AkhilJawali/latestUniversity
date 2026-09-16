import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';

// A4-440 §6 — data layer for Academic Calendar management against the verified A4-9
// routes under /api/v1/academic-calendars (apiClient baseURL is /api/v1). Lists are
// plain { data: [...] } (no pagination). Calendar = create + delete; holidays =
// add + remove; exam windows / orientation = add-only (OQ-3). Sub-entities are read
// from the calendar detail's children arrays (the only read path).
const BASE = '/academic-calendars';

const keys = {
  byCampus: (campusId) => ['master-data', 'academic-calendars', 'campus', campusId],
  detail: (id) => ['master-data', 'academic-calendars', 'detail', id],
};

// FR-1.2 — list calendars for a campus (enabled only when a campus is chosen, UC-2).
export function useCalendarsByCampus(campusId) {
  return useQuery({
    queryKey: keys.byCampus(campusId),
    queryFn: async () => (await apiClient.get(`${BASE}/campus/${campusId}`)).data,
    enabled: campusId != null,
    staleTime: 5 * 60 * 1000,
  });
}

// FR-3.1 — full detail with children arrays (holidays, exam windows, orientation).
export function useCalendarDetail(id) {
  return useQuery({
    queryKey: keys.detail(id),
    queryFn: async () => (await apiClient.get(`${BASE}/${id}`)).data,
    enabled: id != null,
  });
}

// FR-2.3 — create calendar for a campus.
export function useCreateCalendar(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-8.1 — delete calendar (soft, cascades).
export function useDeleteCalendar(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-4.1 — add holiday; invalidates the detail so the sub-list refreshes.
export function useAddHoliday(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(`${BASE}/${calendarId}/holidays`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}

// FR-4.3 — remove holiday.
export function useRemoveHoliday(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (holidayId) =>
      (await apiClient.delete(`${BASE}/${calendarId}/holidays/${holidayId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}

// FR-5.1 — add exam window (add-only, OQ-3).
export function useAddExamWindow(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) =>
      (await apiClient.post(`${BASE}/${calendarId}/exam-windows`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}

// FR-6.1 — add orientation period (add-only, OQ-3).
export function useAddOrientation(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) =>
      (await apiClient.post(`${BASE}/${calendarId}/orientation-periods`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}
