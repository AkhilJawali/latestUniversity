import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { useCampuses } from '../api/useCampuses';
import {
  useCreateDepartment,
  useDeleteDepartment,
  useDepartments,
  useUpdateDepartment,
} from '../api/useDepartments';
import HierarchyBreadcrumb from '../components/HierarchyBreadcrumb';
import { departmentSchema } from '../schemas/hierarchy-schemas';

const CREATE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'code', label: 'Code', type: 'text', required: true },
];
// Backend department update covers name only; code is immutable on edit.
const EDIT_FIELDS = [{ name: 'name', label: 'Name', type: 'text', required: true }];
const BLANK = { name: '', code: '' };

export default function DepartmentsPage() {
  const { campusId } = useParams();
  const campusIdNum = Number(campusId);
  const navigate = useNavigate();

  // Resolve the parent campus code for the breadcrumb from the campuses list.
  const campusesQuery = useCampuses();
  const campus = (campusesQuery.data?.data ?? []).find((c) => c.id === campusIdNum) ?? null;

  const list = useDepartments(campusIdNum);
  const create = useCreateDepartment(campusIdNum);
  const update = useUpdateDepartment(campusIdNum);
  const remove = useDeleteDepartment(campusIdNum);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];
  // campusId seeded so the schema (which requires it) validates on create.
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : { ...BLANK, campusId: campusIdNum }),
    [editing, campusIdNum],
  );

  const openAdd = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const openEdit = (row) => {
    setEditing(row);
    setFormOpen(true);
  };
  const handleSubmit = (data, opts) => {
    if (editing) update.mutate({ id: editing.id, ...data }, { onSuccess: () => setFormOpen(false), ...opts });
    else create.mutate(data, { onSuccess: () => setFormOpen(false), ...opts });
  };

  const columns = [
    { key: 'code', header: 'Code' },
    { key: 'name', header: 'Name' },
    {
      key: '_open',
      header: '',
      render: (row) => (
        <button
          type="button"
          className="link-btn"
          onClick={() =>
            navigate(`/master-data/campus-hierarchy/departments/${row.id}/programs`)
          }
        >
          View programs →
        </button>
      ),
    },
  ];

  return (
    <section>
      <HierarchyBreadcrumb
        items={[
          { label: 'Campuses', to: '/master-data/campus-hierarchy/campuses' },
          { label: campus ? campus.code : `Campus #${campusIdNum}` },
        ]}
      />
      <h1 className="page-title">Departments</h1>
      <p className="page-subtitle">
        Departments in campus {campus ? `${campus.code} — ${campus.name}` : campusIdNum}. Open a
        department to manage its programs.
      </p>

      <ConfigTable
        entityLabel="Departments"
        addLabel="Add department"
        columns={columns}
        rows={rows}
        isLoading={list.isLoading}
        isError={list.isError}
        onRetry={list.refetch}
        onAdd={openAdd}
        onEdit={openEdit}
        onDelete={setDeleting}
      />

      <ConfigFormModal
        open={formOpen}
        title={editing ? 'Edit Department' : 'Add Department'}
        fields={editing ? EDIT_FIELDS : CREATE_FIELDS}
        schema={departmentSchema}
        initialValues={initialValues}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `department "${deleting.code}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </section>
  );
}
