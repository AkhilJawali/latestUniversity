import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';

import AppShell from '@/components/layout/AppShell';
import DashboardPage from '@/features/dashboard/DashboardPage';

// A4-345 — lazy-load the generation viewer for route-based code splitting (NFR-5).
const GenerationPage = lazy(() => import('@/features/scheduling/generation/GenerationPage'));
// A4-15 — lazy-load the drag-and-drop timetable editor (draft-scoped route).
const TimetableEditorPage = lazy(() =>
  import('@/features/scheduling/editor/pages/TimetableEditorPage'),
);
// Approvals — submit a draft, approve/reject per level, publish.
const ApprovalsPage = lazy(() => import('@/features/scheduling/approval/ApprovalsPage'));
// A4-340 — lazy-load the config admin panel.
const SchedulingConfigPage = lazy(() =>
  import('@/features/scheduling-config/pages/SchedulingConfigPage'),
);
// A4-410 — campus hierarchy master-data admin, now split into routed per-entity pages.
const CampusesPage = lazy(() =>
  import('@/features/master-data/campus-hierarchy/pages/CampusesPage'),
);
const DepartmentsPage = lazy(() =>
  import('@/features/master-data/campus-hierarchy/pages/DepartmentsPage'),
);
const ProgramsPage = lazy(() =>
  import('@/features/master-data/campus-hierarchy/pages/ProgramsPage'),
);
const BatchesPage = lazy(() =>
  import('@/features/master-data/campus-hierarchy/pages/BatchesPage'),
);
const SectionsPage = lazy(() =>
  import('@/features/master-data/campus-hierarchy/pages/SectionsPage'),
);
// A4-415 — lazy-load the course management master-data admin.
const CourseManagementPage = lazy(() =>
  import('@/features/master-data/course-management/pages/CourseManagementPage'),
);
// A4-420 — lazy-load the faculty profile management master-data admin.
const FacultyManagementPage = lazy(() =>
  import('@/features/master-data/faculty-management/pages/FacultyManagementPage'),
);
// A4-425 — lazy-load the faculty availability & preferences master-data admin.
const FacultyAvailabilityPage = lazy(() =>
  import('@/features/master-data/faculty-availability/pages/FacultyAvailabilityPage'),
);
// A4-430 — lazy-load the room & lab management master-data admin.
const RoomManagementPage = lazy(() =>
  import('@/features/master-data/room-management/pages/RoomManagementPage'),
);
// A4-435 — lazy-load the schedulable-asset management master-data admin.
const AssetManagementPage = lazy(() =>
  import('@/features/master-data/asset-management/pages/AssetManagementPage'),
);
// A4-440 — lazy-load the academic-calendar management master-data admin.
const AcademicCalendarPage = lazy(() =>
  import('@/features/master-data/academic-calendar/pages/AcademicCalendarPage'),
);
// A4-445 — lazy-load the time-slot grid configuration master-data admin.
const TimeSlotGridPage = lazy(() =>
  import('@/features/master-data/time-slot-grid/pages/TimeSlotGridPage'),
);

// Route table. Feature pages (config CRUD, generation viewer) are added by their
// own stories; placeholders keep the nav navigable until those land.
export const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <AppShell>
        <DashboardPage />
      </AppShell>
    ),
  },
  {
    path: '/scheduling/config',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <SchedulingConfigPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/scheduling/generation',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <GenerationPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/scheduling/approvals',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <ApprovalsPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/scheduling/editor/:draftId',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <TimetableEditorPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/campus-hierarchy',
    element: <Navigate to="/master-data/campus-hierarchy/campuses" replace />,
  },
  {
    path: '/master-data/campus-hierarchy/campuses',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <CampusesPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/campus-hierarchy/campuses/:campusId/departments',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <DepartmentsPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/campus-hierarchy/departments/:departmentId/programs',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <ProgramsPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/campus-hierarchy/programs/:programId/batches',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <BatchesPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/campus-hierarchy/batches/:batchId/sections',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <SectionsPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/courses',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <CourseManagementPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/faculty',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <FacultyManagementPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/faculty-availability',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <FacultyAvailabilityPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/rooms',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <RoomManagementPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/assets',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <AssetManagementPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/academic-calendar',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <AcademicCalendarPage />
        </Suspense>
      </AppShell>
    ),
  },
  {
    path: '/master-data/time-slot-grid',
    element: (
      <AppShell>
        <Suspense fallback={<div className="empty-state">Loading…</div>}>
          <TimeSlotGridPage />
        </Suspense>
      </AppShell>
    ),
  },
]);
