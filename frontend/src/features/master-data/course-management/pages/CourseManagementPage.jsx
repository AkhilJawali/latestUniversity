import { useMemo, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import {
  useCourses,
  useCreateCourse,
  useDeleteCourse,
  useUpdateCourse,
} from '../api/useCourses';
import CrossListingPanel from '../components/CrossListingPanel';
import PrerequisitePanel from '../components/PrerequisitePanel';
import { COURSE_TYPES } from '../constants/course-options';
import { courseCreateSchema, courseEditSchema, filterCourses } from '../schemas/course-schemas';
import '../course-management.css';

// A4-415 §5.1 — Course management page. A flat table of all courses (global list;
// PD-1 client-side type + search filter) with create/edit/delete via the reused
// scheduling-config modal + confirm dialog (PD-4). A campus selector scopes the
// department picker used when creating a course (PD-3), since the generic modal
// takes a single `departmentId` select and departments are fetched per campus.
// Selecting a course reveals the prerequisite and cross-listing panels for it (PD-2).
export default function CourseManagementPage() {
  const courses = useCourses();
  const createMut = useCreateCourse();
  const updateMut = useUpdateCourse();
  const deleteMut = useDeleteCourse();

  // Campus scope drives the department options offered in the create modal.
  const campuses = useCampuses();
  const [scopeCampusId, setScopeCampusId] = useState('');
  const departments = useDepartments(scopeCampusId ? Number(scopeCampusId) : null);

  // Client-side filter controls (FR-1.2).
  const [typeFilter, setTypeFilter] = useState('');
  const [search, setSearch] = useState('');

  // Modal / dialog UI state.
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // course row when editing, null when creating
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [selected, setSelected] = useState(null); // course whose panels are shown

  const allCourses = courses.data?.data ?? [];
  const visibleRows = useMemo(
    () => filterCourses(allCourses, { type: typeFilter, search }),
    [allCourses, typeFilter, search],
  );

  const campusRows = campuses.data?.data ?? [];
  const deptRows = departments.data?.data ?? [];
  const deptOptions = deptRows.map((d) => ({ value: d.id, label: d.name }));

  const typeOptions = COURSE_TYPES.map((t) => ({ value: t, label: t }));

  const columns = [
    { key: 'code', header: 'Code' },
    { key: 'name', header: 'Name' },
    { key: 'departmentName', header: 'Department' },
    {
      key: 'ltp',
      header: 'L-T-P',
      render: (r) => `${r.lectureHours}-${r.tutorialHours}-${r.practicalHours}`,
    },
    { key: 'credits', header: 'Credits' },
    { key: 'courseType', header: 'Type', filterable: true },
    {
      key: 'isCrossListed',
      header: 'Cross-listed',
      filterable: true,
      render: (r) => (r.isCrossListed ? 'Yes' : 'No'),
    },
  ];

  // Create modal needs code + department; edit modal omits both (immutable in A4-3).
  const isEditing = Boolean(editing);
  const fields = isEditing
    ? [
        { name: 'name', label: 'Name', required: true },
        { name: 'lectureHours', label: 'Lecture hours (L)', type: 'number', required: true },
        { name: 'tutorialHours', label: 'Tutorial hours (T)', type: 'number', required: true },
        { name: 'practicalHours', label: 'Practical hours (P)', type: 'number', required: true },
        { name: 'credits', label: 'Credits', type: 'number', required: true },
        { name: 'courseType', label: 'Type', type: 'select', required: true, options: typeOptions },
      ]
    : [
        { name: 'name', label: 'Name', required: true },
        { name: 'code', label: 'Code', required: true },
        {
          name: 'departmentId',
          label: 'Department',
          type: 'select',
          required: true,
          options: deptOptions,
        },
        { name: 'lectureHours', label: 'Lecture hours (L)', type: 'number', required: true },
        { name: 'tutorialHours', label: 'Tutorial hours (T)', type: 'number', required: true },
        { name: 'practicalHours', label: 'Practical hours (P)', type: 'number', required: true },
        { name: 'credits', label: 'Credits', type: 'number', required: true },
        { name: 'courseType', label: 'Type', type: 'select', required: true, options: typeOptions },
      ];

  // A4-410 code-review fix: memoize initialValues so ConfigFormModal's reset effect
  // only fires when the target actually changes (not on every render).
  const initialValues = useMemo(() => {
    if (editing) {
      return {
        name: editing.name ?? '',
        lectureHours: editing.lectureHours ?? 0,
        tutorialHours: editing.tutorialHours ?? 0,
        practicalHours: editing.practicalHours ?? 0,
        credits: editing.credits ?? '',
        courseType: editing.courseType ?? '',
      };
    }
    return {
      name: '',
      code: '',
      departmentId: '',
      lectureHours: 0,
      tutorialHours: 0,
      practicalHours: 0,
      credits: '',
      courseType: '',
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
      onError: (err) => {
        const mapped = mapApiError(err);
        setDeleteError(mapped.message ?? 'Could not delete this course.');
      },
    });
  };

  const cancelDelete = () => {
    setToDelete(null);
    setDeleteError(null);
  };

  // Keep the selected course reference fresh after list refetches.
  const selectedCourse = selected ? allCourses.find((c) => c.id === selected.id) ?? null : null;

  return (
    <section className="course-management">
      <h1 className="page-title">Course Management</h1>
      <p className="page-subtitle">
        Create and manage courses with their L-T-P split, credits, type, prerequisites, and
        cross-listings.
      </p>

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="filter-type">Type</label>
          <select
            id="filter-type"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          >
            <option value="">All types</option>
            {COURSE_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="filter-search">Search</label>
          <input
            id="filter-search"
            type="search"
            placeholder="Name or code…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="scope-campus">Campus (for new course department)</label>
          <select
            id="scope-campus"
            value={scopeCampusId}
            onChange={(e) => setScopeCampusId(e.target.value)}
          >
            <option value="">Select campus…</option>
            {campusRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <ConfigTable
        entityLabel="Courses"
        addLabel="Add course"
        columns={columns}
        rows={visibleRows}
        isLoading={courses.isLoading}
        isError={courses.isError}
        onRetry={courses.refetch}
        onAdd={openCreate}
        onEdit={openEdit}
        onDelete={(row) => setToDelete(row)}
      />

      <div className="table-hint">
        <label htmlFor="manage-course">Manage prerequisites / cross-listings for</label>
        <select
          id="manage-course"
          value={selected?.id ?? ''}
          onChange={(e) => {
            const id = Number(e.target.value);
            setSelected(allCourses.find((c) => c.id === id) ?? null);
          }}
        >
          <option value="">Select a course…</option>
          {visibleRows.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} — {c.name}
            </option>
          ))}
        </select>
      </div>

      {selectedCourse && (
        <div className="course-panels">
          <PrerequisitePanel
            key={`prereq-${selectedCourse.id}`}
            course={selectedCourse}
            allCourses={allCourses}
          />
          <CrossListingPanel key={`xl-${selectedCourse.id}`} course={selectedCourse} />
        </div>
      )}

      <ConfigFormModal
        open={modalOpen}
        title={isEditing ? 'Edit course' : 'Add course'}
        fields={fields}
        schema={isEditing ? courseEditSchema : courseCreateSchema}
        initialValues={initialValues}
        isPending={createMut.isPending || updateMut.isPending}
        onSubmit={submitForm}
        onClose={closeModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={
          deleteError
            ? `course ${toDelete?.code} — ${toDelete?.name}. ${deleteError}`
            : toDelete
              ? `course ${toDelete.code} — ${toDelete.name}`
              : 'this course'
        }
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </section>
  );
}
