import PropTypes from 'prop-types';
import { useState } from 'react';

import {
  useCreateSoftWeight,
  useDeleteSoftWeight,
  useSoftWeights,
  useUpdateSoftWeight,
} from '../api/useSoftWeights';
import { SOFT_CONSTRAINT_TYPES } from '../constants/config-options';
import { softWeightSchema } from '../schemas/config-schemas';
import ConfigFormModal from './ConfigFormModal';
import ConfigTable from './ConfigTable';
import ConfirmDeleteDialog from './ConfirmDeleteDialog';

const COLUMNS = [
  { key: 'constraintType', header: 'Constraint Type' },
  { key: 'weight', header: 'Weight' },
  { key: 'isActive', header: 'Active', render: (r) => (r.isActive ? 'Yes' : 'No') },
];

const FIELDS = [
  { name: 'constraintType', label: 'Constraint Type', type: 'select', required: true, options: SOFT_CONSTRAINT_TYPES },
  { name: 'weight', label: 'Weight', type: 'number', required: true },
  { name: 'isActive', label: 'Active', type: 'checkbox' },
];

const BLANK = { constraintType: '', weight: '', isActive: true };

export default function SoftWeightTab({ campusId }) {
  const list = useSoftWeights(campusId);
  const create = useCreateSoftWeight(campusId);
  const update = useUpdateSoftWeight(campusId);
  const remove = useDeleteSoftWeight(campusId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

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

  return (
    <>
      <ConfigTable
        entityLabel="Soft-Constraint Weights"
        addLabel="Add weight"
        columns={COLUMNS}
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
        title={editing ? 'Edit Soft-Constraint Weight' : 'Add Soft-Constraint Weight'}
        fields={FIELDS}
        schema={softWeightSchema}
        initialValues={editing ? { ...BLANK, ...editing } : BLANK}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `weight "${deleting.constraintType}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

SoftWeightTab.propTypes = {
  campusId: PropTypes.number.isRequired,
};
