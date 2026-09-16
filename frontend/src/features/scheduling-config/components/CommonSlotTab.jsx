import PropTypes from 'prop-types';
import { useState } from 'react';

import {
  useCommonSlots,
  useCreateCommonSlot,
  useDeleteCommonSlot,
  useUpdateCommonSlot,
} from '../api/useCommonSlots';
import { DAYS_OF_WEEK } from '../constants/config-options';
import { commonSlotSchema } from '../schemas/config-schemas';
import ConfigFormModal from './ConfigFormModal';
import ConfigTable from './ConfigTable';
import ConfirmDeleteDialog from './ConfirmDeleteDialog';

const COLUMNS = [
  { key: 'name', header: 'Name' },
  { key: 'dayOfWeek', header: 'Day' },
  { key: 'slotDefinitionId', header: 'Slot Def. ID' },
  {
    key: 'appliesToAllBatches',
    header: 'All Batches',
    render: (r) => (r.appliesToAllBatches ? 'Yes' : 'No'),
  },
  { key: 'isActive', header: 'Active', render: (r) => (r.isActive ? 'Yes' : 'No') },
];

// slotDefinitionId is a plain number input here (the design's useSlotDefinitions
// lookup dropdown is a follow-up; the generation feature likewise takes raw IDs
// until a master-data lookup lands).
const FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'dayOfWeek', label: 'Day of Week', type: 'select', required: true, options: DAYS_OF_WEEK },
  { name: 'slotDefinitionId', label: 'Slot Definition ID', type: 'number', required: true },
  { name: 'appliesToAllBatches', label: 'Applies to all batches', type: 'checkbox' },
  { name: 'isActive', label: 'Active', type: 'checkbox' },
];

const BLANK = {
  name: '',
  dayOfWeek: '',
  slotDefinitionId: '',
  appliesToAllBatches: true,
  isActive: true,
};

export default function CommonSlotTab({ campusId }) {
  const list = useCommonSlots(campusId);
  const create = useCreateCommonSlot(campusId);
  const update = useUpdateCommonSlot(campusId);
  const remove = useDeleteCommonSlot(campusId);

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
        entityLabel="Common Slots"
        addLabel="Add slot"
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
        title={editing ? 'Edit Common Slot' : 'Add Common Slot'}
        fields={FIELDS}
        schema={commonSlotSchema}
        initialValues={editing ? { ...BLANK, ...editing } : BLANK}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `slot "${deleting.name}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

CommonSlotTab.propTypes = {
  campusId: PropTypes.number.isRequired,
};
