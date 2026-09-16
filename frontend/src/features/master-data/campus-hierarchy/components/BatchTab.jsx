import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useBatches,
  useCreateBatch,
  useDeleteBatch,
  useUpdateBatch,
} from '../api/useBatches';
import { batchSchema } from '../schemas/hierarchy-schemas';

// A4-410 §5.2 — Batch tab, filtered by the selected program (PD-1).
const COLUMNS = [
  { key: 'yearIdentifier', header: 'Year' },
  { key: 'strength', header: 'Strength' },
  { key: 'electiveBasket', header: 'Elective Basket' },
];

const FIELDS = [
  { name: 'yearIdentifier', label: 'Year Identifier', type: 'text', required: true },
  { name: 'strength', label: 'Strength', type: 'number', required: true },
  { name: 'electiveBasket', label: 'Elective Basket', type: 'text' },
];

const BLANK = { yearIdentifier: '', strength: '', electiveBasket: '' };

export default function BatchTab({ programId, selectedId, onSelect }) {
  const list = useBatches(programId);
  const create = useCreateBatch(programId);
  const update = useUpdateBatch(programId);
  const remove = useDeleteBatch(programId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

  // Stable identity so ConfigFormModal doesn't reset the form on parent
  // re-renders — A4-410 code review #1. programId seeded for schema validation.
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : { ...BLANK, programId }),
    [editing, programId],
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
          {selectedId === row.id ? 'Selected' : 'View sections'}
        </button>
      ),
    },
  ];

  return (
    <>
      <ConfigTable
        entityLabel="Batches"
        addLabel="Add batch"
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
        title={editing ? 'Edit Batch' : 'Add Batch'}
        fields={FIELDS}
        schema={batchSchema}
        initialValues={initialValues}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `batch "${deleting.yearIdentifier}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

BatchTab.propTypes = {
  programId: PropTypes.number.isRequired,
  selectedId: PropTypes.number,
  onSelect: PropTypes.func.isRequired,
};
