import { useMemo, useState } from 'react';

import { useFacultyList } from '@/features/master-data/faculty-management/api/useFaculty';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import {
  useAvailabilityWindows,
  useCreateWindow,
  useDeleteWindow,
  useUpdateWindow,
} from '../api/useAvailabilityWindows';
import AvailabilityWindowModal from '../components/AvailabilityWindowModal';
import PreferencesForm from '../components/PreferencesForm';
import '../faculty-availability.css';

// A4-425 §5.5 — Faculty availability & preferences page. Scoped to a selected faculty
// (all A4-5 endpoints are per-faculty). Two distinct sections: availability windows
// (hard / unavailability) and preferences (soft).
const hhmm = (t) => (t ? String(t).slice(0, 5) : '');
const titleCase = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : '');

export default function FacultyAvailabilityPage() {
  const faculty = useFacultyList({});
  const [facultyId, setFacultyId] = useState('');
  const selectedId = facultyId ? Number(facultyId) : null;

  const windows = useAvailabilityWindows(selectedId);
  const createMut = useCreateWindow(selectedId);
  const updateMut = useUpdateWindow(selectedId);
  const deleteMut = useDeleteWindow(selectedId);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const facultyRows = faculty.data?.data ?? [];
  const rows = windows.data?.data ?? [];

  const columns = [
    { key: 'dayOfWeek', header: 'Day', filterable: true, render: (r) => titleCase(r.dayOfWeek) },
    { key: 'startTime', header: 'Start', render: (r) => hhmm(r.startTime) },
    { key: 'endTime', header: 'End', render: (r) => hhmm(r.endTime) },
    { key: 'reasonCode', header: 'Reason', filterable: true },
    { key: 'reasonNote', header: 'Note', render: (r) => r.reasonNote ?? '—' },
  ];

  const isEditing = Boolean(editing);
  const initialValues = useMemo(() => {
    if (editing) {
      return {
        dayOfWeek: editing.dayOfWeek ?? '',
        startTime: hhmm(editing.startTime),
        endTime: hhmm(editing.endTime),
        reasonCode: editing.reasonCode ?? '',
        reasonNote: editing.reasonNote ?? '',
      };
    }
    return { dayOfWeek: '', startTime: '', endTime: '', reasonCode: '', reasonNote: '' };
  }, [editing]);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };
  const openEdit = (row) => {
    setEditing(row);
    setModalOpen(true);
  };
  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
  };

  const submitForm = (data, handlers) => {
    if (editing) {
      updateMut.mutate({ id: editing.id, ...data }, { ...handlers, onSuccess: closeModal });
    } else {
      createMut.mutate(data, { ...handlers, onSuccess: closeModal });
    }
  };

  const confirmDelete = () => {
    setDeleteError(null);
    deleteMut.mutate(toDelete.id, {
      onSuccess: () => setToDelete(null),
      onError: (err) => setDeleteError(mapApiError(err).message ?? 'Could not delete this window.'),
    });
  };
  const cancelDelete = () => {
    setToDelete(null);
    setDeleteError(null);
  };

  return (
    <section className="faculty-availability">
      <h1 className="page-title">Faculty Availability &amp; Preferences</h1>
      <p className="page-subtitle">
        Declare unavailability windows (hard) and scheduling preferences (soft) for a faculty member.
      </p>

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="fa-faculty">Faculty</label>
          <select id="fa-faculty" value={facultyId} onChange={(e) => setFacultyId(e.target.value)}>
            <option value="">Select a faculty…</option>
            {facultyRows.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name} ({f.identifier})
              </option>
            ))}
          </select>
        </div>
      </div>

      {!selectedId ? (
        <p className="empty-state">Select a faculty to manage their availability and preferences.</p>
      ) : (
        <div className="availability-layout">
          <div className="windows-section">
            <ConfigTable
              entityLabel="Unavailability windows"
              addLabel="Add window"
              columns={columns}
              rows={rows}
              isLoading={windows.isLoading}
              isError={windows.isError}
              onRetry={windows.refetch}
              onAdd={openCreate}
              onEdit={openEdit}
              onDelete={(row) => {
                setDeleteError(null);
                setToDelete(row);
              }}
            />
          </div>

          <PreferencesForm key={selectedId} facultyId={selectedId} />
        </div>
      )}

      <AvailabilityWindowModal
        open={modalOpen}
        mode={isEditing ? 'edit' : 'create'}
        initialValues={initialValues}
        isPending={createMut.isPending || updateMut.isPending}
        onSubmit={submitForm}
        onClose={closeModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={
          deleteError
            ? `this window. ${deleteError}`
            : toDelete
              ? `the ${toDelete.dayOfWeek?.toLowerCase()} ${hhmm(toDelete.startTime)}–${hhmm(toDelete.endTime)} window`
              : 'this window'
        }
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </section>
  );
}
