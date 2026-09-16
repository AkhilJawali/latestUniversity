import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useCreateDepartment,
  useDeleteDepartment,
  useDepartments,
  useUpdateDepartment,
} from '../api/useDepartments';
import { departmentSchema } from '../schemas/hierarchy-schemas';

// A4-410 §5.2 — Department tab, filtered by the selected campus (PD-1).
const COLUMNS = [
  { key: 'name', header: 'Name' },
  { key: 'code', header: 'Code' },
];

const CREATE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'code', label: 'Code', type: 'text', required: true },
];

// Backend department update covers name only; code read-only on edit.
const EDIT_FIELDS = [{ name: 'name', label: 'Name', type: 'text', required: true }];

const BLANK = { name: '', code: '' };

export default function DepartmentTab({ campusId, selectedId, onSelect }) {
  const list = useDepartments(campusId);
  const create = useCreateDepartment(campusId);
  const update = useUpdateDepartment(campusId);
  const remove = useDeleteDepartment(campusId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

  // Stable identity so ConfigFormModal doesn't reset the form on parent
  // re-renders — A4-410 code review #1. campusId is seeded so the schema
  // (which requires it) validates even though it's not a visible field.
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : { ...BLANK, campusId }),
    [editing, campusId],
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
    // campusId is injected by the create hook; on create we send name+code, backend needs campusId.
    if (editing) update.mutate({ id: editing.id, ...data }, { onSuccess: () => setFormOpen(false), ...opts });
    else create.mutate(data, { onSuccess: () => setFormOpen(false), ...opts });
  };

  const columnsWithSelect = [
    ...COLUMNS,
    {
      key: '_select',
      header: '',
      render: (row) => (
        <button
          type="button"
          className={`link-btn ${selectedId === row.id ? 'link-btn--active' : ''}`}
          onClick={() => onSelect(row)}
        >
          {selectedId === row.id ? 'Selected' : 'View programs'}
        </button>
      ),
    },
  ];

  return (
    <>
      <ConfigTable
        entityLabel="Departments"
        addLabel="Add department"
        columns={columnsWithSelect}
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
        label={deleting ? `department "${deleting.name}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

DepartmentTab.propTypes = {
  campusId: PropTypes.number.isRequired,
  selectedId: PropTypes.number,
  onSelect: PropTypes.func.isRequired,
};
