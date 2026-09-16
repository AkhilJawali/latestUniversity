import PropTypes from 'prop-types';
import { useState } from 'react';

import {
  useCreateDerivationRule,
  useDeleteDerivationRule,
  useDerivationRules,
  useUpdateDerivationRule,
} from '../api/useDerivationRules';
import { COMPONENT_TYPES } from '../constants/config-options';
import { derivationRuleSchema } from '../schemas/config-schemas';
import ConfigFormModal from './ConfigFormModal';
import ConfigTable from './ConfigTable';
import ConfirmDeleteDialog from './ConfirmDeleteDialog';

const COLUMNS = [
  { key: 'componentType', header: 'Component Type' },
  { key: 'slotDurationMinutes', header: 'Slot Duration (min)' },
  { key: 'hoursPerSession', header: 'Hours / Session' },
  { key: 'description', header: 'Description' },
  { key: 'isActive', header: 'Active', render: (r) => (r.isActive ? 'Yes' : 'No') },
];

const FIELDS = [
  { name: 'componentType', label: 'Component Type', type: 'select', required: true, options: COMPONENT_TYPES },
  { name: 'slotDurationMinutes', label: 'Slot Duration (minutes)', type: 'number', required: true },
  { name: 'hoursPerSession', label: 'Hours per Session', type: 'number', required: true },
  { name: 'description', label: 'Description', type: 'text' },
  { name: 'isActive', label: 'Active', type: 'checkbox' },
];

const BLANK = {
  componentType: '',
  slotDurationMinutes: '',
  hoursPerSession: '',
  description: '',
  isActive: true,
};

export default function DerivationRuleTab({ campusId }) {
  const list = useDerivationRules(campusId);
  const create = useCreateDerivationRule(campusId);
  const update = useUpdateDerivationRule(campusId);
  const remove = useDeleteDerivationRule(campusId);

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
        entityLabel="Derivation Rules"
        addLabel="Add rule"
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
        title={editing ? 'Edit Derivation Rule' : 'Add Derivation Rule'}
        fields={FIELDS}
        schema={derivationRuleSchema}
        initialValues={editing ? { ...BLANK, ...editing } : BLANK}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `rule "${deleting.componentType}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

DerivationRuleTab.propTypes = {
  campusId: PropTypes.number.isRequired,
};
