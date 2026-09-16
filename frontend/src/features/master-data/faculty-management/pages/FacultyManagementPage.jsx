import { useMemo, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import { useCourses } from '@/features/master-data/course-management/api/useCourses';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import {
  useCreateFaculty,
  useDeleteFaculty,
  useFacultyList,
  useUpdateFaculty,
} from '../api/useFaculty';
import CampusPanel from '../components/CampusPanel';
import CompetencyPanel from '../components/CompetencyPanel';
import FacultyFormModal from '../components/FacultyFormModal';
import { DESIGNATIONS } from '../constants/faculty-options';
import { buildFacultyListParams } from '../schemas/faculty-schemas';
import '../faculty-management.css';

// A4-420 §5.4 — Faculty management page. Server-paginated table with department /
// designation / competency-course filters (PD-OQ2), create/edit via the custom
// FacultyFormModal, soft-delete via the reused confirm dialog, and per-faculty
// competency + campus panels (PD-4/PD-5).
export default function FacultyManagementPage() {
  // Filters.
  const [filterCampusId, setFilterCampusId] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [designation, setDesignation] = useState('');
  const [competencyCourseId, setCompetencyCourseId] = useState('');

  const listParams = useMemo(
    () => buildFacultyListParams({ departmentId, designation, competencyCourseId }),
    [departmentId, designation, competencyCourseId],
  );
  const faculty = useFacultyList(listParams);

  const createMut = useCreateFaculty();
  const updateMut = useUpdateFaculty();
  const deleteMut = useDeleteFaculty();

  // Reference data.
  const campuses = useCampuses();
  const filterDepartments = useDepartments(filterCampusId ? Number(filterCampusId) : null);
  const courses = useCourses();
  const campusRows = campuses.data?.data ?? [];
  const filterDeptRows = filterDepartments.data?.data ?? [];
  const courseRows = courses.data?.data ?? [];

  // Modal / dialog state.
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [selected, setSelected] = useState(null);

  const rows = faculty.data?.data ?? [];

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'identifier', header: 'Identifier' },
    { key: 'designation', header: 'Designation', filterable: true },
    { key: 'homeDepartmentName', header: 'Department', filterable: true },
    { key: 'minWeeklyLoad', header: 'Min load', render: (r) => r.minWeeklyLoad ?? '—' },
    { key: 'maxWeeklyLoad', header: 'Max load', render: (r) => r.maxWeeklyLoad ?? '—' },
    { key: 'isActive', header: 'Active', filterable: true, render: (r) => (r.isActive ? 'Yes' : 'No') },
  ];

  const isEditing = Boolean(editing);
  const initialValues = useMemo(() => {
    if (editing) {
      return {
        name: editing.name ?? '',
        designation: editing.designation ?? '',
        qualification: editing.qualification ?? '',
        homeDepartmentId: editing.homeDepartmentId ?? '',
        minWeeklyLoad: editing.minWeeklyLoad ?? '',
        maxWeeklyLoad: editing.maxWeeklyLoad ?? '',
      };
    }
    return {
      name: '',
      identifier: '',
      designation: '',
      qualification: '',
      homeDepartmentId: '',
      minWeeklyLoad: '',
      maxWeeklyLoad: '',
      campusIds: [],
      competencyCourseIds: [],
    };
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
      onSuccess: () => {
        if (selected?.id === toDelete.id) setSelected(null);
        setToDelete(null);
      },
      onError: (err) => setDeleteError(mapApiError(err).message ?? 'Could not delete this faculty.'),
    });
  };
  const cancelDelete = () => {
    setToDelete(null);
    setDeleteError(null);
  };

  const selectedFaculty = selected ? rows.find((r) => r.id === selected.id) ?? selected : null;

  return (
    <section className="faculty-management">
      <h1 className="page-title">Faculty Profile Management</h1>
      <p className="page-subtitle">
        Manage faculty profiles, subject competencies, and multi-campus associations.
      </p>

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="f-campus">Campus (for dept filter)</label>
          <select
            id="f-campus"
            value={filterCampusId}
            onChange={(e) => {
              setFilterCampusId(e.target.value);
              setDepartmentId('');
            }}
          >
            <option value="">Select campus…</option>
            {campusRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="f-dept">Department</label>
          <select
            id="f-dept"
            value={departmentId}
            onChange={(e) => setDepartmentId(e.target.value)}
            disabled={!filterCampusId || filterDepartments.isLoading}
          >
            <option value="">All departments</option>
            {filterDeptRows.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="f-designation">Designation</label>
          <select
            id="f-designation"
            value={designation}
            onChange={(e) => setDesignation(e.target.value)}
          >
            <option value="">All designations</option>
            {DESIGNATIONS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="f-competency">Competent in course</label>
          <select
            id="f-competency"
            value={competencyCourseId}
            onChange={(e) => setCompetencyCourseId(e.target.value)}
          >
            <option value="">Any</option>
            {courseRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.code} — {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <ConfigTable
        entityLabel="Faculty"
        addLabel="Add faculty"
        columns={columns}
        rows={rows}
        isLoading={faculty.isLoading}
        isError={faculty.isError}
        onRetry={faculty.refetch}
        onAdd={openCreate}
        onEdit={openEdit}
        onDelete={(row) => setToDelete(row)}
      />

      <div className="table-hint">
        <label htmlFor="manage-faculty">Manage competencies / campuses for</label>
        <select
          id="manage-faculty"
          value={selected?.id ?? ''}
          onChange={(e) => {
            const id = Number(e.target.value);
            setSelected(rows.find((r) => r.id === id) ?? null);
          }}
        >
          <option value="">Select a faculty…</option>
          {rows.map((r) => (
            <option key={r.id} value={r.id}>
              {r.name} ({r.identifier})
            </option>
          ))}
        </select>
      </div>

      {selectedFaculty && (
        <div className="faculty-panels">
          <CompetencyPanel
            key={`comp-${selectedFaculty.id}`}
            faculty={selectedFaculty}
            allCourses={courseRows}
          />
          <CampusPanel key={`camp-${selectedFaculty.id}`} faculty={selectedFaculty} />
        </div>
      )}

      <FacultyFormModal
        open={modalOpen}
        mode={isEditing ? 'edit' : 'create'}
        initialValues={initialValues}
        currentDepartment={
          editing
            ? { id: editing.homeDepartmentId, name: editing.homeDepartmentName }
            : null
        }
        courses={courseRows}
        isPending={createMut.isPending || updateMut.isPending}
        onSubmit={submitForm}
        onClose={closeModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={
          deleteError
            ? `faculty ${toDelete?.name}. ${deleteError}`
            : toDelete
              ? `faculty ${toDelete.name} (${toDelete.identifier})`
              : 'this faculty'
        }
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </section>
  );
}
