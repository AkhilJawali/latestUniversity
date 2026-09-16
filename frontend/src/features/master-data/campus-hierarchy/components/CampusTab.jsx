import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useCampuses,
  useCreateCampus,
  useDeleteCampus,
  useUpdateCampus,
} from '../api/useCampuses';
import { campusSchema } from '../schemas/hierarchy-schemas';

// A4-410 §5.2 — Campus tab (top level). Selecting a row drives the department
// drill-down (onSelect). Code is immutable on edit (FR-4.2).
const COLUMNS = [
  { key: 'name', header: 'Name' },
  { key: 'code', header: 'Code' },
  { key: 'location', header: 'Location' },
];

const CREATE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'code', label: 'Code', type: 'text', required: true },
  { name: 'location', label: 'Location', type: 'text', required: true },
];

// On edit, code is read-only (backend campus update covers name + location only).
const EDIT_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'location', label: 'Location', type: 'text', required: true },
];

const BLANK = { name: '', code: '', location: '' };

export default function CampusTab({ selectedId, onSelect }) {
  const list = useCampuses();
  const create = useCreateCampus();
  const update = useUpdateCampus();
  const remove = useDeleteCampus();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

  // Stable identity so ConfigFormModal doesn't reset the form on parent
  // re-renders (e.g. isPending toggling on submit) — A4-410 code review #1.
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : BLANK),
    [editing],
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
          {selectedId === row.id ? 'Selected' : 'View departments'}
        </button>
      ),
    },
  ];

  return (
    <>
      <ConfigTable
        entityLabel="Campuses"
        addLabel="Add campus"
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
        title={editing ? 'Edit Campus' : 'Add Campus'}
        fields={editing ? EDIT_FIELDS : CREATE_FIELDS}
        schema={campusSchema}
        initialValues={initialValues}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `campus "${deleting.name}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

CampusTab.propTypes = {
  selectedId: PropTypes.number,
  onSelect: PropTypes.func.isRequired,
};
