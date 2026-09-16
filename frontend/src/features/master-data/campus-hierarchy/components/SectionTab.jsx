import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useCreateSection,
  useDeleteSection,
  useSections,
  useUpdateSection,
} from '../api/useSections';
import { sectionSchema } from '../schemas/hierarchy-schemas';

// A4-410 §5.3 / PD-2 — Section tab. Sections are the leaf level: nested under a
// batch, non-paginated, and have no further drill-down.
const COLUMNS = [
  { key: 'sectionIdentifier', header: 'Section' },
  { key: 'subStrength', header: 'Sub-strength' },
];

const FIELDS = [
  { name: 'sectionIdentifier', label: 'Section Identifier', type: 'text', required: true },
  { name: 'subStrength', label: 'Sub-strength', type: 'number', required: true },
];

const BLANK = { sectionIdentifier: '', subStrength: '' };

export default function SectionTab({ batchId }) {
  const list = useSections(batchId);
  const create = useCreateSection(batchId);
  const update = useUpdateSection(batchId);
  const remove = useDeleteSection(batchId);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

  // Stable identity so ConfigFormModal doesn't reset the form on parent
  // re-renders — A4-410 code review #1.
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

  return (
    <>
      <ConfigTable
        entityLabel="Sections"
        addLabel="Add section"
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
        title={editing ? 'Edit Section' : 'Add Section'}
        fields={FIELDS}
        schema={sectionSchema}
        initialValues={initialValues}
        isPending={create.isPending || update.isPending}
        onSubmit={handleSubmit}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDeleteDialog
        open={deleting != null}
        label={deleting ? `section "${deleting.sectionIdentifier}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </>
  );
}

SectionTab.propTypes = {
  batchId: PropTypes.number.isRequired,
};
