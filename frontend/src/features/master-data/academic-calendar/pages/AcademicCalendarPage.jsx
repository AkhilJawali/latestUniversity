import { useEffect, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import {
  useCalendarDetail,
  useCalendarsByCampus,
  useCreateCalendar,
  useDeleteCalendar,
} from '../api/useAcademicCalendars';
import CalendarDetailPanel from '../components/CalendarDetailPanel';
import CalendarFormModal from '../components/CalendarFormModal';
import WorkingDayPatternEditor from '../components/WorkingDayPatternEditor';
import '../academic-calendar.css';

// A4-440 §9–§11 — Academic Calendar admin page. Campus-scoped (UC-2): all calendar and
// pattern operations require a selected campus. Master-detail: campus selector → calendar
// list → selected-calendar detail (holidays / exam windows / orientation) + a per-campus
// working-day pattern section. Calendar = create + delete only (OQ-3). A lightweight
// inline status banner (role="status") stands in for toasts (FR-9.3) since the app has no
// global toast system yet.
export default function AcademicCalendarPage() {
  const [campusId, setCampusId] = useState('');
  const [selectedCalendarId, setSelectedCalendarId] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [notice, setNotice] = useState(null); // { kind: 'success'|'error', text }

  const numericCampusId = campusId ? Number(campusId) : null;

  const campuses = useCampuses();
  const campusRows = campuses.data?.data ?? [];

  const calendars = useCalendarsByCampus(numericCampusId);
  const rows = calendars.data?.data ?? [];

  const detail = useCalendarDetail(selectedCalendarId);
  const selectedCalendar = detail.data?.data ?? null;

  const createMut = useCreateCalendar(numericCampusId);
  const deleteMut = useDeleteCalendar(numericCampusId);

  // Auto-clear success notices after 5s (errors persist until replaced — ui-standards).
  useEffect(() => {
    if (notice?.kind === 'success') {
      const t = setTimeout(() => setNotice(null), 5000);
      return () => clearTimeout(t);
    }
    return undefined;
  }, [notice]);

  const pushToast = (kind, text) => setNotice({ kind, text });

  const columns = [
    { key: 'academicYear', header: 'Academic Year' },
    { key: 'semesterIdentifier', header: 'Semester' },
    { key: 'semesterStartDate', header: 'Start' },
    { key: 'semesterEndDate', header: 'End' },
  ];

  const changeCampus = (value) => {
    setCampusId(value);
    setSelectedCalendarId(null);
    setToDelete(null);
    setDeleteError(null);
  };

  const submitCreate = (data, handlers) =>
    createMut.mutate(
      { campusId: numericCampusId, ...data },
      {
        ...handlers,
        onSuccess: () => {
          setModalOpen(false);
          pushToast('success', 'Calendar created.');
        },
      },
    );

  const confirmDelete = () => {
    setDeleteError(null);
    deleteMut.mutate(toDelete.id, {
      onSuccess: () => {
        if (String(toDelete.id) === String(selectedCalendarId)) setSelectedCalendarId(null);
        setToDelete(null);
        pushToast('success', 'Calendar deleted.');
      },
      onError: (err) =>
        setDeleteError(mapApiError(err).message ?? 'Could not delete this calendar.'),
    });
  };
  const cancelDelete = () => {
    setToDelete(null);
    setDeleteError(null);
  };

  return (
    <section className="academic-calendar">
      <h1 className="page-title">Academic Calendar Management</h1>
      <p className="page-subtitle">
        Manage per-campus academic calendars — semester dates, holidays, exam windows,
        orientation periods, and the working-day pattern.
      </p>

      {notice && (
        <p className={`inline-notice inline-notice--${notice.kind}`} role="status">
          {notice.text}
        </p>
      )}

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="cal-campus">Campus</label>
          <select id="cal-campus" value={campusId} onChange={(e) => changeCampus(e.target.value)}>
            <option value="">Select a campus…</option>
            {campusRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {!campusId && (
        <p className="empty-state">Select a campus to view and manage its calendars.</p>
      )}

      {campusId && (
        <>
          <ConfigTable
            entityLabel="Calendars"
            addLabel="New calendar"
            columns={columns}
            rows={rows}
            isLoading={calendars.isLoading}
            isError={calendars.isError}
            onRetry={calendars.refetch}
            onAdd={() => setModalOpen(true)}
            onEdit={(row) => setSelectedCalendarId(row.id)}
            onDelete={(row) => {
              setDeleteError(null);
              setToDelete(row);
            }}
            searchable
          />

          {selectedCalendarId && detail.isLoading && (
            <p className="empty-state">Loading calendar…</p>
          )}
          {selectedCalendar && (
            <CalendarDetailPanel
              key={selectedCalendarId}
              calendar={selectedCalendar}
              onToast={pushToast}
            />
          )}

          <WorkingDayPatternEditor campusId={campusId} onToast={pushToast} />
        </>
      )}

      <CalendarFormModal
        open={modalOpen}
        isPending={createMut.isPending}
        onSubmit={submitCreate}
        onClose={() => setModalOpen(false)}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={
          deleteError
            ? `calendar. ${deleteError}`
            : toDelete
              ? `calendar ${toDelete.academicYear} — ${toDelete.semesterIdentifier} (removes all its entries)`
              : 'this calendar'
        }
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </section>
  );
}
