---
inclusion: fileMatch
fileMatchPattern: "**/*{store,state,query,hook,useQuery,useMutation}*"
---

# State Management Standards — UTMS (TanStack Query + Zustand)

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  React Components                │
├─────────────────────────────────────────────────┤
│  Server State (TanStack Query)  │  Client State (Zustand)  │
│  - API data                     │  - UI state               │
│  - Cache & sync                 │  - Sidebar open/close     │
│  - Background refetch           │  - Selected filters       │
│  - Optimistic updates           │  - Drag state             │
│  - Pagination state             │  - Modal visibility       │
└─────────────────────────────────┴───────────────────────────┘
```

## State Categories

| Category | Tool | Examples in UTMS |
|----------|------|------------------|
| Server state | TanStack Query | Courses list, faculty data, timetable sessions, conflicts |
| Client/UI state | Zustand | Sidebar toggle, selected campus filter, drag-in-progress, active tab |
| URL state | React Router | Current page, timetable ID in URL, filter params in query string |
| Form state | React Hook Form + Zod | Course creation form, faculty availability form |

## TanStack Query Patterns

### Query Key Convention
```javascript
// Pattern: [resource, scope?, identifier?, filters?]
const queryKeys = {
  courses: {
    all: ['courses'],
    list: (filters) => ['courses', 'list', filters],
    detail: (id) => ['courses', 'detail', id],
  },
  timetables: {
    all: ['timetables'],
    list: (deptId, semester) => ['timetables', 'list', deptId, semester],
    detail: (id) => ['timetables', 'detail', id],
    sessions: (timetableId) => ['timetables', 'sessions', timetableId],
    conflicts: (timetableId) => ['timetables', 'conflicts', timetableId],
  },
  faculty: {
    all: ['faculty'],
    detail: (id) => ['faculty', 'detail', id],
    workload: (id, semester) => ['faculty', 'workload', id, semester],
    availability: (id) => ['faculty', 'availability', id],
  },
  rooms: {
    all: ['rooms'],
    availability: (roomId, date) => ['rooms', 'availability', roomId, date],
    utilisation: (campusId) => ['rooms', 'utilisation', campusId],
  },
};
```

### Query Hook Pattern
```javascript
// src/features/master-data/courses/api/useCourses.js
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

export function useCourses(filters) {
  return useQuery({
    queryKey: queryKeys.courses.list(filters),
    queryFn: () => apiClient.get('/courses', { params: filters }),
    staleTime: 5 * 60 * 1000, // 5 min — master data changes infrequently
  });
}

export function useCreateCourse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => apiClient.post('/courses', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.courses.all });
    },
  });
}
```

### Stale Time Guidelines
| Data Type | Stale Time | Rationale |
|-----------|-----------|-----------|
| Master data (campuses, rooms, courses) | 5 min | Rarely changes during a session |
| Timetable sessions | 30 sec | May change during collaborative editing |
| Conflicts | 0 (always fresh) | Must reflect real-time state |
| Faculty workload | 1 min | Changes when sessions are reassigned |
| Approval status | 30 sec | Approvers may act concurrently |

### Optimistic Updates (Timetable Drag-Drop)
```javascript
export function useMoveSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => apiClient.patch(`/sessions/${data.sessionId}`, data),
    onMutate: async (data) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.timetables.sessions(data.timetableId) });
      const previous = queryClient.getQueryData(queryKeys.timetables.sessions(data.timetableId));
      // Optimistically update the session position
      queryClient.setQueryData(queryKeys.timetables.sessions(data.timetableId), (old) =>
        updateSessionInCache(old, data)
      );
      return { previous };
    },
    onError: (_err, data, context) => {
      // Rollback on failure
      queryClient.setQueryData(queryKeys.timetables.sessions(data.timetableId), context?.previous);
    },
    onSettled: (_data, _err, variables) => {
      // Refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: queryKeys.timetables.sessions(variables.timetableId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.timetables.conflicts(variables.timetableId) });
    },
  });
}
```

## Zustand Patterns

### Store Structure
```javascript
// src/stores/timetableEditorStore.js
import { create } from 'zustand';

export const useTimetableEditorStore = create((set) => ({
  // State
  selectedCampusId: null,
  selectedDepartmentId: null,
  isDragging: false,
  draggedSessionId: null,
  highlightedConflicts: [],
  sidebarOpen: true,

  // Actions
  selectCampus: (id) => set({ selectedCampusId: id, selectedDepartmentId: null }),
  selectDepartment: (id) => set({ selectedDepartmentId: id }),
  startDrag: (sessionId) => set({ isDragging: true, draggedSessionId: sessionId }),
  endDrag: () => set({ isDragging: false, draggedSessionId: null }),
  highlightConflicts: (ids) => set({ highlightedConflicts: ids }),
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  reset: () => set({ selectedCampusId: null, selectedDepartmentId: null, isDragging: false, draggedSessionId: null, highlightedConflicts: [], sidebarOpen: true }),
}));
```

### Zustand Rules
- One store per concern (not one giant global store)
- Stores hold UI/client state only — never duplicate server data from TanStack Query
- Use selectors to subscribe to specific slices: `const isDragging = useTimetableEditorStore((s) => s.isDragging)`
- Keep actions simple and synchronous — async work belongs in TanStack Query mutations
- Name stores: `use<Domain>Store` (e.g., `useTimetableEditorStore`, `useFilterStore`)

### UTMS Stores
| Store | Purpose |
|-------|---------|
| `useTimetableEditorStore` | Drag state, selected session, conflict highlights |
| `useFilterStore` | Global filter selections (campus, department, semester) |
| `useSidebarStore` | Navigation sidebar open/collapsed state |
| `useApprovalStore` | Current approval step, selected items for bulk action |

## URL State (React Router)
- Use URL params for resource identity: `/timetables/:timetableId`
- Use search params for filters: `?campus=1&department=3&semester=odd-2025`
- Sync filter store with URL params on mount (URL is source of truth for shareable views)
- Use `useSearchParams()` hook, not manual `window.location` parsing

## Form State (React Hook Form + Zod)
- Use React Hook Form for all forms (course creation, faculty availability, session editing)
- Define Zod schemas matching backend validation rules
- Schemas live in the feature's `validators/` folder
- Show inline validation errors immediately on blur, summary on submit
- Disable submit button while `isSubmitting` is true
- Clear form state on successful mutation (`reset()` after `onSuccess`)
