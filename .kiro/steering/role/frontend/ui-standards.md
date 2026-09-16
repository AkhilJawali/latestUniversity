---
inclusion: fileMatch
fileMatchPattern: "**/*.{tsx,jsx,css,scss,module.css}"
---

# UI Standards — UTMS Frontend

## Design System
- **Component library:** Shadcn/ui (Radix primitives + Tailwind CSS)
- **Icon set:** Lucide React
- **Charts:** Recharts (for reporting/analytics dashboards)
- **Timetable grid:** FullCalendar React (or react-big-calendar) with custom event rendering
- **Drag and drop:** @dnd-kit (for timetable session reordering and slot assignment)

## Spacing & Layout
- Use Tailwind spacing scale (4px base): `p-1` (4px), `p-2` (8px), `p-4` (16px), `p-6` (24px), `p-8` (32px)
- Page layout: CSS Grid with sidebar + main content area
- Feature content: max-width 1440px, centered
- Timetable grid: full-width within content area, horizontally scrollable on smaller screens
- Card-based layouts for master data CRUD (list → detail → edit)
- Consistent gap: `gap-4` (16px) between cards, `gap-6` (24px) between sections

## Layout Patterns

### Admin/Coordinator Views (Desktop-First)
```
┌──────────────────────────────────────────────────┐
│  Header (Logo, Campus Selector, User Menu)       │
├─────────┬────────────────────────────────────────┤
│ Sidebar │  Main Content                          │
│ (Nav)   │  ┌──────────────────────────────────┐  │
│         │  │ Page Header (Title, Actions)     │  │
│         │  ├──────────────────────────────────┤  │
│         │  │ Filters Bar                      │  │
│         │  ├──────────────────────────────────┤  │
│         │  │ Content (Table/Grid/Calendar)    │  │
│         │  └──────────────────────────────────┘  │
└─────────┴────────────────────────────────────────┘
```

### Student/Faculty Views (Mobile-Responsive)
```
┌──────────────────────┐
│ Header + Hamburger   │
├──────────────────────┤
│ My Timetable (Week)  │
│ ┌──────────────────┐ │
│ │ Day view / List  │ │
│ │ (swipeable)      │ │
│ └──────────────────┘ │
├──────────────────────┤
│ Notifications        │
└──────────────────────┘
```

## Typography
- Font family: Inter (system fallback: `-apple-system, BlinkMacSystemFont, 'Segoe UI'`)
- Scale (Tailwind):
  - `text-xs` (12px): Labels, badges, metadata
  - `text-sm` (14px): Body text, table cells, form labels
  - `text-base` (16px): Primary content
  - `text-lg` (18px): Section headings
  - `text-xl` (20px): Page titles
  - `text-2xl` (24px): Feature headings
- Font weights: 400 (normal), 500 (medium), 600 (semibold), 700 (bold)
- Line height: 1.5 for body, 1.25 for headings

## Colors & Theming
- Use CSS variables / Tailwind theme tokens — no hardcoded hex values
- Color semantics:
  | Purpose | Token | Usage |
  |---------|-------|-------|
  | Primary | `primary` | Buttons, active nav, links |
  | Destructive | `destructive` | Delete actions, conflict alerts |
  | Warning | `warning` | Soft constraint violations, near-capacity warnings |
  | Success | `success` | Approved status, resolved conflicts |
  | Muted | `muted` | Disabled states, secondary text |
- Timetable session colors by type:
  - Lecture: blue shades
  - Tutorial: green shades
  - Practical/Lab: orange shades
  - Exam: red shades
  - Locked/Fixed sessions: grey with lock icon
- Conflict highlight: red border + red background overlay with 20% opacity
- Dark mode: support via Tailwind `dark:` variants (optional for Phase 1, required for Phase 2)

## Timetable Grid UI

### Grid Layout
- Columns: days of the week (Mon–Sat, configurable)
- Rows: time slots (from campus slot grid config — e.g., 8:00 AM to 6:00 PM, 1-hour intervals)
- Sessions rendered as positioned cards within the grid
- Support for multi-slot sessions (labs spanning 2–3 rows)

### Session Card
```
┌───────────────────────────┐
│ CS101 - Data Structures   │ ← Course code + name
│ Prof. Sharma              │ ← Faculty
│ Room: LH-204 (Cap: 60)   │ ← Room + capacity
│ Batch: CSE-A (Sem 3)     │ ← Batch info
│ [🔒] [⚠️]                │ ← Lock status, conflict indicator
└───────────────────────────┘
```

### Interactions
- **Drag-and-drop:** Move sessions between slots (real-time conflict check on drop)
- **Click:** Open session detail panel (edit faculty, room, batch assignment)
- **Right-click / long-press:** Context menu (lock, unlock, delete, duplicate, swap)
- **Hover:** Show conflict details tooltip if session has conflicts
- **Visual feedback:** Ghost card while dragging, red overlay on invalid drop targets, green on valid

### Conflict Indicators
- Red border on session card = active conflict
- Warning badge with count on timetable header
- Conflict panel (side drawer) with list of all conflicts + suggested resolutions
- Clicking a conflict highlights both involved sessions

## Responsive Breakpoints
| Breakpoint | Target | Timetable Behavior |
|-----------|--------|-------------------|
| ≥ 1280px (xl) | Desktop — coordinators | Full week grid, drag-and-drop enabled |
| 768–1279px (md/lg) | Tablet | 3-day view, horizontal scroll for full week |
| < 768px (sm) | Mobile — students/faculty | Day view (one day at a time), list format, swipe between days |

## Tables (Master Data CRUD)
- Use data tables with: sorting, filtering, pagination, row selection
- Column headers: sticky on scroll
- Actions column: icon buttons (edit, delete, view) — not text links
- Empty state: illustration + message + CTA button
- Loading state: skeleton rows (not spinner)
- Bulk actions toolbar appears when rows are selected

## Forms
- Label above input (not inline/floating labels)
- Required fields marked with red asterisk
- Inline validation errors below the field (appear on blur)
- Group related fields with section headings and dividers
- Submit button at bottom-right; Cancel/Reset at bottom-left
- Confirmation dialog before destructive actions (delete, discard changes)

## Modals & Dialogs
- Use for: confirmations, quick edits, session detail view
- Do NOT use for: complex multi-step forms (use full pages instead)
- Always provide close button (X) and Escape key dismissal
- Trap focus within modal while open
- Overlay dims background content

## Toast Notifications
- Position: top-right
- Auto-dismiss after 5 seconds (error toasts stay until manually dismissed)
- Types: success (green), error (red), warning (yellow), info (blue)
- Use for: save confirmations, conflict alerts, approval status changes

## Animation & Motion
- Transitions: 150ms for UI feedback (hover, focus), 300ms for layout changes
- Drag-and-drop: spring animation on drop (subtle bounce)
- Page transitions: fade (200ms)
- Respect `prefers-reduced-motion`: disable all non-essential animation
- No animation on initial data load — use skeleton placeholders instead

## Loading States
- **Initial page load:** Full-page skeleton matching final layout shape
- **Data fetching:** Skeleton rows/cards in content area, header remains interactive
- **Action pending:** Button shows spinner + disabled state; form fields disabled
- **Background refetch:** No visible loading indicator (stale data shown, refreshed silently)

## Empty States
- Show illustration + descriptive message + primary action CTA
- Examples:
  - "No timetable generated yet" → [Generate Timetable] button
  - "No courses added" → [Add Course] button
  - "No conflicts detected" → success illustration + "All clear!" message

## Touch Targets
- Minimum 44x44px for all interactive elements on mobile
- Adequate spacing between tap targets (minimum 8px gap)
- Timetable sessions on mobile: tap to expand detail (no drag on mobile)
