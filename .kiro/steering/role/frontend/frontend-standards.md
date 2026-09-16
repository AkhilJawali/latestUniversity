---
inclusion: fileMatch
fileMatchPattern: "**/*.{jsx,js,css,scss}"
---

# Frontend Development Standards — UTMS (React + JavaScript)

## CRITICAL RULE — NO TYPESCRIPT

**TypeScript is NOT allowed in this project. This is non-negotiable.**

- All frontend files MUST use `.jsx` and `.js` extensions
- NEVER create `.tsx` or `.ts` files
- NEVER add `typescript`, `@types/*` packages to dependencies
- NEVER generate TypeScript interfaces, type annotations, or generics
- Use PropTypes for prop validation
- Use Zod for runtime validation (not compile-time types)
- Use JSDoc comments for documentation where needed
- Design documents must use plain JavaScript examples, not TypeScript
- If Kiro generates any TypeScript, it must be immediately corrected to JavaScript

This rule applies to: code, design documents, code examples in docs, and any generated output.

## Project Structure
```
frontend/
├── public/                     # Static assets
├── src/
│   ├── app/                    # App root, providers, router setup
│   │   ├── App.jsx
│   │   ├── router.jsx          # React Router v6 route definitions
│   │   └── providers.jsx       # QueryClient, Auth, Theme providers
│   ├── components/             # Shared/reusable UI components
│   │   ├── ui/                 # Primitive UI (Button, Input, Modal, Table)
│   │   ├── layout/             # Shell, Sidebar, Header, PageWrapper
│   │   └── feedback/           # Toast, Alert, LoadingSpinner, ErrorBoundary
│   ├── features/               # Feature modules (domain-driven)
│   │   ├── master-data/
│   │   │   ├── campuses/
│   │   │   ├── departments/
│   │   │   ├── courses/
│   │   │   ├── faculty/
│   │   │   ├── rooms/
│   │   │   └── batches/
│   │   ├── timetable/
│   │   │   ├── components/     # TimetableGrid, SessionCard, DragDropEditor
│   │   │   ├── hooks/          # useTimetable, useConflicts
│   │   │   ├── api/            # TanStack Query hooks for timetable endpoints
│   │   │   └── pages/          # TimetableListPage, TimetableEditorPage
│   │   ├── scheduling/
│   │   ├── workload/
│   │   ├── approval/
│   │   └── reporting/
│   ├── hooks/                  # Shared custom hooks
│   ├── lib/                    # Utility functions, constants, helpers
│   │   ├── api-client.js       # Axios/fetch instance with auth interceptor
│   │   ├── validators.js       # Zod schemas for form validation
│   │   └── date-utils.js
│   ├── stores/                 # Zustand stores (client state only)
│   └── styles/                 # Global styles, theme, CSS variables
├── index.html
├── eslint.config.js
└── package.json
```

## Component Structure
```
ComponentName/
├── ComponentName.jsx           # Component implementation
├── ComponentName.test.jsx      # Tests (colocated)
├── ComponentName.module.css    # Scoped styles (CSS Modules)
└── index.js                    # Barrel export
```

## Component Rules
- Functional components only (no class components)
- One component per file (exception: small internal helper components)
- Use PropTypes for prop validation
- Keep components under 150 lines — extract hooks or sub-components if larger
- Colocate feature-specific components inside `features/<feature>/components/`

## Naming Conventions
| Item | Convention | Example |
|------|-----------|---------|
| Component files | PascalCase | `CourseForm.jsx` |
| Hook files | camelCase with `use` prefix | `useCourseList.js` |
| Utility files | camelCase (kebab-case for multi-word) | `date-utils.js` |
| API hook files | camelCase with `use` prefix | `useCourses.js` |
| Store files | camelCase with `Store` suffix | `sidebarStore.js` |
| CSS Modules | PascalCase matching component | `CourseForm.module.css` |
| Constants | UPPER_SNAKE_CASE | `MAX_SESSIONS_PER_DAY` |

## Import Order (enforced by ESLint)
1. React and React-related (`react`, `react-dom`, `react-router-dom`)
2. External libraries (`@tanstack/react-query`, `zustand`, `zod`)
3. Internal shared (`@/components`, `@/hooks`, `@/lib`)
4. Feature-local imports (`./components`, `./hooks`)
5. Styles and assets

Use path alias `@/` mapped to `src/` in bundler config.

## Security (Org Standards Compliance)
- Never use `dangerouslySetInnerHTML` — if rich text rendering is needed, sanitize with DOMPurify first
- All user-facing forms validated with Zod before submission
- API client includes CSRF token handling and JWT refresh logic
- No secrets or API keys in frontend code (use environment variables with `REACT_APP_` prefix)
- CSP-compatible: no inline scripts, no `eval()`

## Accessibility (WCAG AA)
- All interactive elements keyboard-accessible (Tab, Enter, Escape)
- Semantic HTML: `<nav>`, `<main>`, `<section>`, `<button>`, not `<div onClick>`
- ARIA labels on icon-only buttons and custom widgets
- Color contrast ratio minimum 4.5:1 for text, 3:1 for large text
- Focus indicators visible on all interactive elements
- Timetable grid must be navigable with keyboard (arrow keys between cells)
- Screen reader announcements for dynamic content (conflict alerts, drag-drop feedback)

## Performance
- Route-based code splitting with `React.lazy()` + `Suspense`
- Lazy load heavy components (FullCalendar, reporting charts)
- Use `React.memo` for pure display components rendered in lists
- Images: use appropriate formats (WebP), provide width/height to prevent layout shift
- Bundle analysis during CI

## Browser Support
- Chrome 90+, Firefox 90+, Edge 90+, Safari 15+
- No IE11 support
- Mobile: responsive down to 768px (coordinators use desktop; students/faculty use mobile view)

## Error Handling
- Global `ErrorBoundary` at app root with fallback UI
- Per-feature error boundaries for isolation
- TanStack Query `onError` callbacks for API failures
- User-friendly error messages (never show raw API errors or stack traces)
- Toast notifications for transient errors, inline messages for form validation
