import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useCreateProgram,
  useDeleteProgram,
  usePrograms,
  useUpdateProgram,
} from '../api/usePrograms';
import { DEGREE_TYPES } from '../constants/hierarchy-options';
import { programSchema } from '../schemas/hierarchy-schemas';

// A4-410 §5.2 — Program tab, filtered by the selected department (PD-1).
const COLUMNS = [
  { key: 'name', header: 'Name' },
  { key: 'code', header: 'Code' },
  { key: 'degreeType', header: 'Degree' },
  { key: 'durationSemesters', header: 'Semesters' },
];

const CREATE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'code', label: 'Code', type: 'text', required: true },
  { name: 'degreeType', label: 'Degree Type', type: 'select', required: true, options: DEGREE_TYPES },
  { name: 'durationSemesters', label: 'Duration (semesters)', type: 'number', required: true },
];

// Backend program update covers details; code read-only on edit.
const EDIT_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'degreeType', label: 'Degree Type', type: 'select', required: true, options: DEGREE_TYPES },
  { name: 'durationSemesters', label: 'Duration (semesters)', type: 'number', required: true },
];

const BLANK = { name: '', code: '', degreeType: '', durationSemesters: '' };

export default function ProgramTab({ departmentId, selectedId, onSelect }) {
  const list = usePrograms(departmentId);
  const create = useCreateProgram(departmentId);
  const update = useUpdateProgram(departmentId);
  const remove = useDeleteProgram(departmentId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

  // Stable identity so ConfigFormModal doesn't reset the form on parent
  // re-renders — A4-410 code review #1. departmentId seeded for schema validation.
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : { ...BLANK, departmentId }),
    [editing, departmentId],
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
          {selectedId === row.id ? 'Selected' : 'View batches'}
        </button>
      ),
    },
  ];

  return (
    <>
      <ConfigTable
        entityLabel="Programs"
        addLabel="Add program"
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
        label={deleting ? `program "${deleting.name}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

ProgramTab.propTypes = {
  departmentId: PropTypes.number.isRequired,
  selectedId: PropTypes.number,
  onSelect: PropTypes.func.isRequired,
};
