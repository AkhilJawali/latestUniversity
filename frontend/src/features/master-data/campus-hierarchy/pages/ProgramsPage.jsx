import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useCreateProgram,
  useDeleteProgram,
  usePrograms,
  useUpdateProgram,
} from '../api/usePrograms';
import HierarchyBreadcrumb from '../components/HierarchyBreadcrumb';
import { DEGREE_TYPES } from '../constants/hierarchy-options';
import { programSchema } from '../schemas/hierarchy-schemas';

const CREATE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'code', label: 'Code', type: 'text', required: true },
  { name: 'degreeType', label: 'Degree Type', type: 'select', required: true, options: DEGREE_TYPES },
  { name: 'durationSemesters', label: 'Duration (semesters)', type: 'number', required: true },
];
// Code is immutable on edit.
const EDIT_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'degreeType', label: 'Degree Type', type: 'select', required: true, options: DEGREE_TYPES },
  { name: 'durationSemesters', label: 'Duration (semesters)', type: 'number', required: true },
];
const BLANK = { name: '', code: '', degreeType: '', durationSemesters: '' };

export default function ProgramsPage() {
  const { departmentId } = useParams();
  const departmentIdNum = Number(departmentId);
  const navigate = useNavigate();

  const list = usePrograms(departmentIdNum);
  const create = useCreateProgram(departmentIdNum);
  const update = useUpdateProgram(departmentIdNum);
  const remove = useDeleteProgram(departmentIdNum);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : { ...BLANK, departmentId: departmentIdNum }),
    [editing, departmentIdNum],
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
    { key: 'degreeType', header: 'Degree' },
    { key: 'durationSemesters', header: 'Semesters' },
    {
      key: '_open',
      header: '',
      render: (row) => (
        <button
          type="button"
          className="link-btn"
          onClick={() => navigate(`/master-data/campus-hierarchy/programs/${row.id}/batches`)}
        >
          View batches →
        </button>
      ),
    },
  ];

  return (
    <section>
      <HierarchyBreadcrumb
        items={[
          { label: 'Campuses', to: '/master-data/campus-hierarchy/campuses' },
          { label: 'Programs' },
        ]}
      />
      <h1 className="page-title">Programs</h1>
      <p className="page-subtitle">
        Programs under the selected department. Open a program to manage its batches.
      </p>

      <ConfigTable
        entityLabel="Programs"
        addLabel="Add program"
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
        title={editing ? 'Edit Program' : 'Add Program'}
        fields={editing ? EDIT_FIELDS : CREATE_FIELDS}
        schema={programSchema}
        initialValues={initialValues}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `program "${deleting.code}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </section>
  );
}
