# A4-440 — Frontend: Academic Calendar Management (CRUD) — Design Document

| Field | Value |
|-------|-------|
| Story | A4-440 — Frontend — Academic Calendar Management (CRUD) |
| Design Subtask | A4-442 |
| Requirement Subtask | A4-441 (Approved) |
| Epic | A4 — UTMS |
| Role / Type | Frontend (React 18 + plain JSX, NO TypeScript) |
| Consumes backend | A4-9 — Academic Calendar Management (verified, no backend change) |
| Source requirement | docs/requirements/A4-440-frontend-academic-calendar-management-requirements.md |

---

## 1. Introduction

This document derives the concrete frontend design for the Academic Calendar admin UI from the approved A4-440 requirements. It defines the module layout, components, API hooks, Zod schemas, constants, routing, and state — consuming the already-implemented A4-9 REST endpoints under `/api/v1/academic-calendars`. No backend, schema, or API change is in scope. It follows the established master-data feature patterns used by A4-410/415/420/425/430/435 (feature folder with `api/components/constants/pages/schemas`, shared `ConfigTable` + `ConfirmDeleteDialog`, `validateWith`, `mapApiError`, TanStack Query, plain-CSS feature stylesheet).

---

## 2. Resolved Open Questions (from requirement §16)

All five OQs from the requirement were dispositioned by the lead (chat approval, 2026-09-04) and logged in `docs/open-questions-log.md` under **A4-440**. This design implements those dispositions:

| OQ | Requirement question | Design disposition (as built) |
|----|----------------------|-------------------------------|
| OQ-1 | Are "end ≥ start" / conditional-required rules backend-enforced or advisory? | **Advisory client-side** (Zod). Backend remains source of truth; all 4xx surfaced via `mapApiError`. |
| OQ-2 | Exact 4xx error shapes? | **Standard error envelope assumed.** `mapApiError` handles both `details[].field` (→ inline field errors) and `message` (→ toast). |
| OQ-3 | Edit/delete on sub-entities missing in backend. | **Descope in-place edit.** Calendar = create + delete. Holiday = add + remove. Exam window / orientation = **add-only** (no edit, no delete — no endpoint exists). Sub-entities are read from the calendar `GET /{id}` children arrays. |
| OQ-4 | Institution-wide holiday fan-out? | **No UI fan-out.** Display each holiday by its `scope` field (CAMPUS_SPECIFIC / INSTITUTION_WIDE) with a badge; no cross-campus propagation computed client-side. |
| OQ-5 | Is `GET /year/{academicYear}` needed? | **No.** Campus-scoped listing only. Year-view endpoint left unused. Campus name resolved via A4-1 `useCampuses`. |

---

## 3. API Contract Consumed (verified A4-9, base `/api/v1/academic-calendars`)

All success responses are wrapped `{ data: ... }`; lists are plain `{ data: [...] }` (no pagination/meta). `apiClient` baseURL is `/api/v1`, so the feature constant `BASE = '/academic-calendars'`.

| Operation | Method + path | Request body | Response |
|-----------|---------------|--------------|----------|
| Create calendar | `POST /academic-calendars` | `CreateAcademicCalendarRequest` (may nest holidays/examWindows/orientationPeriods) | 201 `{data: AcademicCalendarDto}` |
| Get calendar detail | `GET /academic-calendars/{id}` | — | `{data: AcademicCalendarDto}` (children arrays populated) |
| List by campus | `GET /academic-calendars/campus/{campusId}` | — | `{data: [AcademicCalendarDto]}` |
| Delete calendar | `DELETE /academic-calendars/{id}` | — | 204 (soft, cascades) |
| Add holiday | `POST /academic-calendars/{calendarId}/holidays` | `CreateHolidayRequest` | 201 `{data: CalendarHolidayDto}` |
| Remove holiday | `DELETE /academic-calendars/{calendarId}/holidays/{holidayId}` | — | 204 |
| Add exam window | `POST /academic-calendars/{calendarId}/exam-windows` | `CreateExamWindowRequest` | 201 `{data: CalendarExamWindowDto}` |
| Add orientation | `POST /academic-calendars/{calendarId}/orientation-periods` | `CreateOrientationPeriodRequest` | 201 `{data: CalendarOrientationPeriodDto}` |
| Get pattern by campus | `GET /academic-calendars/patterns/campus/{campusId}` | — | `{data: WorkingDayPatternDto}` **or 404 if none** |
| Create pattern | `POST /academic-calendars/patterns` | `CreateWorkingDayPatternRequest` (has `campusId`) | 201 `{data: WorkingDayPatternDto}` |
| Update pattern | `PUT /academic-calendars/patterns/campus/{campusId}` | `CreateWorkingDayPatternRequest` (body campusId ignored) | 200 `{data: WorkingDayPatternDto}` |

**No endpoints for:** calendar edit, holiday edit, exam-window edit/delete, orientation edit/delete, pattern delete (422). Design honors these gaps (OQ-3).

### DTO field shapes (display / request)
- `AcademicCalendarDto`: `{id, campusId(Long), academicYear, semesterIdentifier, semesterStartDate, semesterEndDate, holidays[], examWindows[], orientationPeriods[], createdAt, updatedAt}` — **no campus name/object, no semester "name".**
- `CalendarHolidayDto`: `{id, calendarId, startDate, endDate, description, scope}` (label = `description`, not name).
- `CalendarExamWindowDto`: `{id, calendarId, startDate, endDate, examType, description}`.
- `CalendarOrientationPeriodDto`: `{id, calendarId, startDate, endDate, description}`.
- `WorkingDayPatternDto`: `{id, campusId, patternType, workingSaturdays(String "1,3"), customDefinition(String), createdAt, updatedAt}`.

### Validation → HTTP (backend authority)
- Semester `end < start` → 422. Holiday/exam/orientation `end < start` → 422 (`start == end` allowed).
- Exam + orientation must be within semester (HC-CAL-6) → 422. **Holidays NOT range-checked against semester.**
- Calendar unique per (campus, year, semester) → 409. Pattern unique per campus → 409.
- Bean validation → 400 with `details[]`. Overlap NOT enforced anywhere.

---

## 4. Module Layout

```
frontend/src/features/master-data/academic-calendar/
├── api/
│   ├── useAcademicCalendars.js     # list-by-campus, detail, create, delete, add/remove sub-entities
│   └── useWorkingDayPattern.js     # get-by-campus, create, update (upsert)
├── components/
│   ├── CalendarFormModal.jsx       # create calendar (semester dates + identifiers)
│   ├── CalendarDetailPanel.jsx     # semester header + 3 sub-lists (holidays/exam/orientation)
│   ├── HolidayFormModal.jsx        # add holiday (dates, description, scope)
│   ├── ExamWindowFormModal.jsx     # add exam window (dates, type, description)
│   ├── OrientationFormModal.jsx    # add orientation period (dates, description)
│   └── WorkingDayPatternEditor.jsx # per-campus pattern create-or-edit
├── constants/
│   └── calendar-constants.js       # HOLIDAY_SCOPES, EXAM_TYPES, PATTERN_TYPES (+ labels)
├── schemas/
│   └── calendar-schemas.js         # calendarCreateSchema, holidaySchema, examWindowSchema,
│                                   #   orientationSchema, patternSchema (conditional required)
├── pages/
│   └── AcademicCalendarPage.jsx    # campus selector + calendar list + detail + pattern editor
└── academic-calendar.css           # feature stylesheet (mirrors asset-management.css)
```

Reused (no new copies):
- `@/features/scheduling-config/components/ConfigTable` (has `searchable` prop + per-column `filterable` flag + built-in search/filter).
- `@/features/scheduling-config/components/ConfirmDeleteDialog` (prop is `label`).
- `@/features/scheduling-config/schemas/config-schemas` → `validateWith(schema, values)` → `{success, data}` | `{success:false, errors:{field:msg}}`.
- `@/lib/api-error` → `mapApiError(err)` → `{fields}` | `{message}`.
- `@/features/master-data/campus-hierarchy/api/useCampuses` → campus selector options (resolves campus name for the header, OQ-5).
- `@/lib/api-client` `apiClient`, TanStack Query, `AppShell`, toast utility (same as prior stories).

---

## 5. Provisional / Key Design Decisions

Provisional Decisions (PD) continue the project-wide sequence. (Prior stories ended at the asset story; these are numbered fresh for this doc and reconciled at approval.)

- **PD-A440-1 — Custom modals over generic ConfigFormModal.** The generic config form modal has no date inputs and no nested-list support. Calendar creation needs date fields; sub-entity adds need date + enum fields; the pattern editor needs conditional fields. Decision: purpose-built modals (`CalendarFormModal`, `HolidayFormModal`, `ExamWindowFormModal`, `OrientationFormModal`, `WorkingDayPatternEditor`). Rejected: extending the generic modal (would bloat it for one consumer).
- **PD-A440-2 — Reuse ConfigTable for all lists.** Calendar list and the three sub-lists render via the shared `ConfigTable` for a consistent look (columns, empty state, skeleton). Calendar list uses `searchable` on academic year / semester identifier. Sub-lists are small, read-only tables (add via toolbar button; holiday rows get a delete action, exam/orientation rows do not per OQ-3).
- **PD-A440-3 — Master-detail page, campus-scoped.** `AcademicCalendarPage` = campus selector (top) → calendar list (left/main) → selected-calendar detail panel (holidays, exam windows, orientation) → separate "Working-day pattern" section for the campus. No calendar operation issues without a selected campus (UC-2). Detail is fetched fresh via `GET /{id}` on selection so children arrays are current.
- **PD-A440-4 — Pattern editor is create-or-edit (upsert) driven by 404.** `useWorkingDayPattern(campusId)` issues `GET /patterns/campus/{campusId}`; a 404 is treated as "no pattern yet" (query returns `null`, not an error toast). Save → `POST /patterns` when none exists, `PUT /patterns/campus/{campusId}` when one exists (UC-3, FR-7.2). Pattern is enum + strings (`patternType`, `workingSaturdays` e.g. `"1,3"`, `customDefinition`) — mirrored as `PATTERN_TYPES` constant, **not** weekday checkboxes (no such backend field). Delete is not offered (backend 422).
- **PD-A440-5 — No in-place edit anywhere (OQ-3).** Calendar: create + delete only. Holiday: add + remove only. Exam window / orientation: add-only. The UI shows no edit buttons for these; the requirement's "edit" scope is explicitly not coverable and is recorded as a backend follow-up (OQ-3, req §14).
- **PD-A440-6 — Holiday scope shown as a badge (OQ-4).** Each holiday row renders a scope badge (`Campus-specific` / `Institution-wide`) from the DTO `scope`. No client-side cross-campus fan-out; the UI reflects only what the API returns for the selected campus's calendars.
- **PD-A440-7 — Advisory client validation (OQ-1/UC-1).** Zod schemas mirror A4-9 request bounds (required, max-length, end ≥ start, conditional pattern fields). Client validation blocks submit for fast feedback, but every mutation still handles backend 4xx via `mapApiError` → inline field errors (400 with `details`) or toast (409/422/500). Dates use native `<input type="date">` (ISO `YYYY-MM-DD`) — the format A4-9 expects.
- **PD-A440-8 — Date-range compare helper.** A small `endNotBeforeStart(start, end)` helper (string ISO compare, safe for `YYYY-MM-DD`) powers the Zod `superRefine` on every date-pair. `start == end` is allowed (matches backend `validateDateRange`).
- **KD-A440-1 — Semester-bound advisory for exam/orientation.** Since the backend enforces exam/orientation within the semester (HC-CAL-6) but holidays are not bound, the exam/orientation modals receive the selected calendar's `semesterStartDate`/`semesterEndDate` and add an *advisory* Zod check (within semester). Holidays get no such check (matches backend). Backend still re-validates (422 handled).

---

## 6. API Hooks (design)

Mirrors the `useAssets.js` convention (module `BASE`, `keys` factory, `staleTime`, `invalidateQueries` on mutation success).

### `api/useAcademicCalendars.js`
```js
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

const BASE = '/academic-calendars';

const keys = {
  byCampus: (campusId) => ['master-data', 'academic-calendars', 'campus', campusId],
  detail: (id) => ['master-data', 'academic-calendars', 'detail', id],
};

// FR-1.2 — list calendars for a campus (enabled only when a campus is chosen, UC-2)
export function useCalendarsByCampus(campusId) {
  return useQuery({
    queryKey: keys.byCampus(campusId),
    queryFn: async () => (await apiClient.get(`${BASE}/campus/${campusId}`)).data,
    enabled: campusId != null,
    staleTime: 5 * 60 * 1000,
  });
}

// FR-3.1 — full detail with children arrays
export function useCalendarDetail(id) {
  return useQuery({
    queryKey: keys.detail(id),
    queryFn: async () => (await apiClient.get(`${BASE}/${id}`)).data,
    enabled: id != null,
  });
}

// FR-2.3 — create calendar for a campus
export function useCreateCalendar(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-8.1 — delete calendar
export function useDeleteCalendar(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id) => (await apiClient.delete(`${BASE}/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-4.1 — add holiday; invalidates the detail so the sub-list refreshes
export function useAddHoliday(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(`${BASE}/${calendarId}/holidays`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}

// FR-4.3 — remove holiday
export function useRemoveHoliday(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (holidayId) =>
      (await apiClient.delete(`${BASE}/${calendarId}/holidays/${holidayId}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}

// FR-5.1 — add exam window (add-only, OQ-3)
export function useAddExamWindow(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) =>
      (await apiClient.post(`${BASE}/${calendarId}/exam-windows`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}

// FR-6.1 — add orientation period (add-only, OQ-3)
export function useAddOrientation(calendarId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) =>
      (await apiClient.post(`${BASE}/${calendarId}/orientation-periods`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.detail(calendarId) }),
  });
}
```

### `api/useWorkingDayPattern.js`
```js
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

const BASE = '/academic-calendars/patterns';

const keys = { byCampus: (campusId) => ['master-data', 'working-day-pattern', campusId] };

// FR-7.1 — load existing pattern; 404 => null (no pattern yet), not an error (PD-A440-4)
export function useWorkingDayPattern(campusId) {
  return useQuery({
    queryKey: keys.byCampus(campusId),
    enabled: campusId != null,
    queryFn: async () => {
      try {
        return (await apiClient.get(`${BASE}/campus/${campusId}`)).data;
      } catch (err) {
        if (err?.response?.status === 404) return { data: null };
        throw err;
      }
    },
  });
}

// FR-7.2 — create when none exists
export function useCreatePattern(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}

// FR-7.2 — update existing
export function useUpdatePattern(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.put(`${BASE}/campus/${campusId}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.byCampus(campusId) }),
  });
}
```

---

## 7. Zod Schemas (design) — `schemas/calendar-schemas.js`

Bounds mirror A4-9 request DTOs (§8 of requirement). All validation is advisory (PD-A440-7); backend re-validates.

```js
import { z } from 'zod';
import { HOLIDAY_SCOPES, EXAM_TYPES, PATTERN_TYPES } from '../constants/calendar-constants';

// PD-A440-8 — ISO YYYY-MM-DD string compare; end == start allowed (matches backend)
const endNotBeforeStart = (v, ctx) => {
  if (v.startDate && v.endDate && v.endDate < v.startDate) {
    ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'End date must be on or after start date' });
  }
};

export const calendarCreateSchema = z
  .object({
    academicYear: z.string().trim().min(1, 'Academic year is required').max(20, 'Max 20 characters'),
    semesterIdentifier: z.string().trim().min(1, 'Semester identifier is required').max(30, 'Max 30 characters'),
    semesterStartDate: z.string().min(1, 'Start date is required'),
    semesterEndDate: z.string().min(1, 'End date is required'),
  })
  .superRefine((v, ctx) => {
    if (v.semesterStartDate && v.semesterEndDate && v.semesterEndDate < v.semesterStartDate) {
      ctx.addIssue({ code: 'custom', path: ['semesterEndDate'], message: 'End date must be on or after start date' });
    }
  });

export const holidaySchema = z
  .object({
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
    description: z.string().trim().min(1, 'Description is required').max(200, 'Max 200 characters'),
    scope: z.enum(HOLIDAY_SCOPES.map((s) => s.value)),
  })
  .superRefine(endNotBeforeStart);

// examWindowSchema / orientationSchema accept semester bounds for the advisory within-semester
// check (KD-A440-1). They are schema factories so the modal can pass the selected calendar's
// semester start/end.
export const makeExamWindowSchema = ({ semesterStartDate, semesterEndDate } = {}) =>
  z
    .object({
      startDate: z.string().min(1, 'Start date is required'),
      endDate: z.string().min(1, 'End date is required'),
      examType: z.enum(EXAM_TYPES.map((t) => t.value)),
      description: z.string().trim().max(200, 'Max 200 characters').optional().or(z.literal('')),
    })
    .superRefine((v, ctx) => {
      endNotBeforeStart(v, ctx);
      if (semesterStartDate && v.startDate && v.startDate < semesterStartDate) {
        ctx.addIssue({ code: 'custom', path: ['startDate'], message: 'Must be within the semester' });
      }
      if (semesterEndDate && v.endDate && v.endDate > semesterEndDate) {
        ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'Must be within the semester' });
      }
    });

export const makeOrientationSchema = ({ semesterStartDate, semesterEndDate } = {}) =>
  z
    .object({
      startDate: z.string().min(1, 'Start date is required'),
      endDate: z.string().min(1, 'End date is required'),
      description: z.string().trim().max(200, 'Max 200 characters').optional().or(z.literal('')),
    })
    .superRefine((v, ctx) => {
      endNotBeforeStart(v, ctx);
      if (semesterStartDate && v.startDate && v.startDate < semesterStartDate) {
        ctx.addIssue({ code: 'custom', path: ['startDate'], message: 'Must be within the semester' });
      }
      if (semesterEndDate && v.endDate && v.endDate > semesterEndDate) {
        ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'Must be within the semester' });
      }
    });

// FR-7.3 — conditional required: workingSaturdays iff ALTERNATE_SATURDAY, customDefinition iff CUSTOM
export const patternSchema = z
  .object({
    patternType: z.enum(PATTERN_TYPES.map((p) => p.value)),
    workingSaturdays: z.string().trim().max(100, 'Max 100 characters').optional().or(z.literal('')),
    customDefinition: z.string().trim().optional().or(z.literal('')),
  })
  .superRefine((v, ctx) => {
    if (v.patternType === 'ALTERNATE_SATURDAY' && !v.workingSaturdays) {
      ctx.addIssue({ code: 'custom', path: ['workingSaturdays'], message: 'Required for Alternate Saturdays (e.g. 1,3)' });
    }
    if (v.patternType === 'CUSTOM' && !v.customDefinition) {
      ctx.addIssue({ code: 'custom', path: ['customDefinition'], message: 'Required for a custom pattern' });
    }
  });
```

The page uses the shared `validateWith(schema, values)` wrapper (returns `{success, data}` or `{success:false, errors:{field:msg}}`) so error handling matches every other master-data page.

---

## 8. Constants — `constants/calendar-constants.js`

Enums mirrored from the backend (no list endpoint exists; these match `HolidayScope`, `ExamType`, `PatternType`). Each item has a `value` (sent to API) and a `label` (shown to user).

```js
export const HOLIDAY_SCOPES = [
  { value: 'CAMPUS_SPECIFIC', label: 'Campus-specific' },
  { value: 'INSTITUTION_WIDE', label: 'Institution-wide' },
];

export const EXAM_TYPES = [
  { value: 'MID_SEMESTER', label: 'Mid-semester' },
  { value: 'END_SEMESTER', label: 'End-semester' },
  { value: 'SUPPLEMENTARY', label: 'Supplementary' },
];

export const PATTERN_TYPES = [
  { value: 'FIVE_DAY', label: '5-day week' },
  { value: 'SIX_DAY', label: '6-day week' },
  { value: 'ALTERNATE_SATURDAY', label: 'Alternate Saturdays' },
  { value: 'CUSTOM', label: 'Custom' },
];

// label lookup helpers (used by table cells / badges)
export const labelOf = (list, value) => list.find((i) => i.value === value)?.label ?? value;
```

---

## 9. Components (design)

All modals: focus-trapped, Escape to close, `aria-describedby` linking each field to its error message, labels above inputs, required marked with an asterisk, submit disabled + spinner while the mutation is pending (FR-9.4, NFR-3). No `dangerouslySetInnerHTML` (NFR-4).

### `AcademicCalendarPage.jsx` (page composition)
- Campus selector (`<select>` from `useCampuses()`), stored in local `useState` (client-only; Zustand not needed). Header shows the resolved campus name.
- On campus chosen → `useCalendarsByCampus(campusId)`; renders skeleton while loading, empty state (+"New Calendar" CTA) when none, else `ConfigTable` (columns: Academic Year, Semester, Start, End; row actions: View, Delete). `searchable` on year + semester.
- Selecting a row sets `selectedCalendarId` → `useCalendarDetail(id)` → renders `CalendarDetailPanel` with `key={selectedCalendarId}` (remount on change, per prior code-review lesson).
- Separate "Working-day pattern" section bound to the campus (`WorkingDayPatternEditor`).
- "New Calendar" opens `CalendarFormModal`.
- Delete calendar uses `ConfirmDeleteDialog` with `label` = calendar year+semester; `deleteError` surfaced in the dialog; `cancelDelete` clears it; opening a new delete target resets `deleteError` (prior code-review lessons).
- Toasts on every create/delete outcome (FR-9.3).

### `CalendarFormModal.jsx`
- Fields: academic year, semester identifier, semester start date, semester end date (all required). Optional: none in v1 (nested sub-entities are added from the detail panel after creation — simpler UX; the create endpoint's nested arrays remain available but are not surfaced as a form to keep the modal focused, PD-A440-1).
- `validateWith(calendarCreateSchema, values)` on submit; inline errors; on success `useCreateCalendar(campusId)`; memoized `initialValues`.
- `mapApiError` on failure → field errors (409 duplicate typically surfaces as a message toast since it's not field-scoped).

### `CalendarDetailPanel.jsx`
- Header: semester dates (trimmed display), campus name.
- Three `ConfigTable` sub-lists:
  - **Holidays**: columns Start, End, Description, Scope (badge via `labelOf(HOLIDAY_SCOPES, scope)`); row action Delete (guarded by `ConfirmDeleteDialog`). Toolbar "Add Holiday" → `HolidayFormModal`.
  - **Exam windows**: columns Start, End, Type (`labelOf(EXAM_TYPES,…)`), Description; **no row actions** (add-only, OQ-3). Toolbar "Add Exam Window" → `ExamWindowFormModal` (passes semester bounds).
  - **Orientation periods**: columns Start, End, Description; **no row actions**. Toolbar "Add Orientation" → `OrientationFormModal` (passes semester bounds).
- Each add uses the calendar's `id`; success invalidates the detail query so the sub-list refreshes.

### `HolidayFormModal.jsx`
- Fields: start date, end date, description (required), scope (select from `HOLIDAY_SCOPES`).
- `validateWith(holidaySchema, values)`; `useAddHoliday(calendarId)`; `mapApiError`.

### `ExamWindowFormModal.jsx`
- Fields: start date, end date, exam type (select `EXAM_TYPES`), description (optional).
- Schema via `makeExamWindowSchema({semesterStartDate, semesterEndDate})`; `useAddExamWindow(calendarId)`.

### `OrientationFormModal.jsx`
- Fields: start date, end date, description (optional).
- Schema via `makeOrientationSchema({...})`; `useAddOrientation(calendarId)`.

### `WorkingDayPatternEditor.jsx`
- `useWorkingDayPattern(campusId)` → if `data` present, prefill (`patternType`, `workingSaturdays`, `customDefinition`); if `null`, blank create form.
- Pattern-type select from `PATTERN_TYPES`. Conditional fields: `workingSaturdays` shown/required only for `ALTERNATE_SATURDAY`; `customDefinition` shown/required only for `CUSTOM` (FR-7.3).
- Save: `useUpdatePattern(campusId)` when a pattern existed, else `useCreatePattern(campusId)` (PD-A440-4). Body always includes `campusId` (required by create; ignored by update). Success toast; no delete button.

---

## 10. Routing & Navigation

- Route: `/master-data/academic-calendar` — lazy-loaded page, wrapped in `AppShell` + `Suspense` (NFR-5), added to `app/router.jsx` following the existing pattern:
  ```js
  const AcademicCalendarPage = lazy(() =>
    import('@/features/master-data/academic-calendar/pages/AcademicCalendarPage'),
  );
  // route entry mirrors the /master-data/assets block
  ```
- Nav: add an "Academic Calendar" item to the master-data section of the sidebar (same list where "Assets", "Rooms", etc. live), pointing to `/master-data/academic-calendar`.

---

## 11. State Management

- **Server state:** TanStack Query only (hooks in §6). Query keys are campus/calendar scoped; mutations invalidate the precise key (list-by-campus for calendar create/delete; detail for sub-entity add/remove; pattern-by-campus for pattern upsert).
- **Client state:** local `useState` in the page for `selectedCampusId`, `selectedCalendarId`, modal open flags, and delete-target/`deleteError`. No Zustand store needed (nothing shared across routes).
- **Cache freshness:** `staleTime` 5 min on the campus list (read-mostly); detail refetched on selection. After a sub-entity add, the detail query refetch repopulates the children arrays (the only read path for sub-entities, since there are no per-sub-entity GETs).

---

## 12. Accessibility & Security

- Keyboard: all modals focus-trapped, Escape closes, focus returns to the invoking control. Selects and date inputs are native (fully accessible). Icon-only actions have `aria-label`.
- Field errors: `aria-describedby` on each input references its error node; `aria-invalid` set when errored.
- Contrast: scope/type badges use theme tokens meeting WCAG AA (no hardcoded hex).
- Security: Zod validation before submit; `mapApiError` never renders raw stack traces; no `dangerouslySetInnerHTML`; dates constrained to ISO by the native picker.

---

## 13. Testing Strategy (design-level; execution gated separately by lead)

- **Schema unit tests:** required fields, max lengths, end≥start (incl. `start==start` allowed), pattern conditional-required (ALTERNATE_SATURDAY→workingSaturdays, CUSTOM→customDefinition), exam/orientation within-semester advisory.
- **Hook tests:** correct URLs/methods/bodies; 404 on pattern GET maps to `{data:null}` (not thrown); mutation success invalidates the right query key.
- **Component tests:** CalendarFormModal blocks submit on invalid range (AC-5); delete flow shows confirm then issues DELETE only on confirm (AC-6); holiday scope badge renders campus-specific vs institution-wide (AC-2/AC-3); pattern editor prefills existing and toggles conditional fields (AC-4); backend 4xx surfaces via mapApiError and preserves input (AC-7).
- Test runner: `node node_modules/vitest/vitest.mjs run <path>` in `d:\BL_UNI\frontend`. (Whether unit tests/coverage are performed this story is the lead's call, per the established per-story flow.)

---

## 14. Decision Interaction / Consistency Notes (P6)

- **No-edit (PD-A440-5) × "create/edit/delete" requirement scope:** reconciled — requirement §14 already flagged the mismatch and OQ-3 descopes edit. Design surfaces zero edit affordances so the UI never implies an unsupported operation.
- **Pattern 404 (PD-A440-4) × global error handling:** the pattern GET swallows 404 into `{data:null}` locally so the app-level query error path (which would toast) is not triggered for the expected "no pattern yet" case. All other statuses re-throw and surface normally.
- **Semester-bound advisory (KD-A440-1) × holidays:** deliberately *not* applied to holidays (backend does not bind holidays to the semester); applied only to exam/orientation, matching HC-CAL-6. No contradiction between the advisory checks and the backend.
- **Campus name absence × display:** `AcademicCalendarDto` has only `campusId`; the page resolves the display name from the loaded `useCampuses` list (OQ-5). No dependency on a backend campus-name field.
- **Add-only sub-entities × cache:** since exam/orientation have no GET/delete, the only way they appear/refresh is via the calendar detail's children arrays; every add invalidates that detail query. Consistent, no dead storage — the detail read path consumes them.
- **Router/nav consistency:** new route + nav entry mirror the existing `/master-data/assets` block exactly; no change to `AppShell` or other routes.

---

## 15. Requirement Traceability

| Requirement (A4-440) | Design element |
|----------------------|----------------|
| FR-1 (campus context + listing) | `AcademicCalendarPage` campus selector + `useCalendarsByCampus`; skeleton/empty state |
| FR-2 (create calendar) | `CalendarFormModal` + `calendarCreateSchema` + `useCreateCalendar`; `mapApiError` (FR-2.4) |
| FR-3 (calendar detail) | `useCalendarDetail` + `CalendarDetailPanel` (three sub-lists) |
| FR-4 (holidays add/remove + scope) | `HolidayFormModal`, `useAddHoliday`/`useRemoveHoliday`, scope badge (OQ-4) |
| FR-5 (exam window add) | `ExamWindowFormModal` + `makeExamWindowSchema` + `useAddExamWindow` (add-only, OQ-3) |
| FR-6 (orientation add) | `OrientationFormModal` + `makeOrientationSchema` + `useAddOrientation` (add-only, OQ-3) |
| FR-7 (working-day pattern editor) | `WorkingDayPatternEditor` + `useWorkingDayPattern`/`useCreatePattern`/`useUpdatePattern` + `patternSchema` |
| FR-8 (delete calendar) | `ConfirmDeleteDialog` + `useDeleteCalendar` |
| FR-9 (cross-cutting: confirm, skeleton, empty, toast, pending) | `ConfigTable` (skeleton/empty), `ConfirmDeleteDialog`, toast utility, disabled+spinner states |
| UC-1 advisory validation | Zod schemas + backend 4xx handling (PD-A440-7) |
| UC-2 campus scoping | selector-gated queries (`enabled: campusId != null`) |
| UC-3 single pattern/campus | pattern upsert (PD-A440-4) |
| NFR-1 no TypeScript | all `.jsx`/`.js`, PropTypes + Zod |
| NFR-2 conventions | feature layout + shared components/hooks reuse |
| NFR-3 a11y | focus trap, aria-describedby, labels, native inputs |
| NFR-4 security | Zod, no dangerouslySetInnerHTML, no raw errors |
| NFR-5 performance | lazy route + skeletons + query caching |
| AC-1..AC-7 | §13 testing maps each AC to a component/schema behavior |
| "edit" portion of story scope | **Not covered — OQ-3 (backend gap), see PD-A440-5** |

---

## 16. Open Questions (design)

All requirement OQs are resolved (§2). No new open questions introduced by the design. The only carried-forward item is the **backend edit/delete gap (OQ-3)** — logged in `docs/open-questions-log.md` as a backend follow-up for A4-9; it does not block this frontend story.

---

## 17. Out of Scope

- Any backend/API/schema change (owned by A4-9), including edit/delete endpoints for sub-entities and calendar edit.
- `GET /year/{academicYear}` cross-campus year view (OQ-5).
- Time-slot grid UI (A4-445, next story).
- RBAC/login, scheduling/conflict/impact visualization, bulk import/export.
- Unit-test/coverage execution unless the lead elects to perform it for this story.
