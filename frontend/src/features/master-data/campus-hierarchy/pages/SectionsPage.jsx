import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { z } from 'zod';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { useBatch } from '../api/useBatches';
import {
  useCreateSection,
  useDeleteSection,
  useSections,
  useUpdateSection,
} from '../api/useSections';
import HierarchyBreadcrumb from '../components/HierarchyBreadcrumb';

const FIELDS = [
  { name: 'sectionIdentifier', label: 'Section Identifier', type: 'text', required: true },
  { name: 'subStrength', label: 'Sub-strength', type: 'number', required: true },
];
const BLANK = { sectionIdentifier: '', subStrength: '' };

const COLUMNS = [
  { key: 'sectionIdentifier', header: 'Section' },
  { key: 'subStrength', header: 'Sub-strength' },
];

// Build a section schema whose sub-strength is capped so the TOTAL across all
// sections cannot exceed the batch strength (item #2). `remaining` is batch
// strength minus the sum of the OTHER sections (excludes the one being edited).
function buildSectionSchema(remaining) {
  return z.object({
    sectionIdentifier: z
      .string()
      .min(1, 'Section identifier is required')
      .max(20, 'Must not exceed 20 characters'),
    subStrength: z.coerce
      .number({ invalid_type_error: 'Enter a number' })
      .int('Must be a whole number')
      .min(1, 'Sub-strength must be at least 1')
      .max(
        Math.max(remaining, 0),
        `Only ${Math.max(remaining, 0)} seat(s) remaining in this batch`,
      ),
  });
}

export default function SectionsPage() {
  const { batchId } = useParams();
  const batchIdNum = Number(batchId);

  const batchQuery = useBatch(batchIdNum);
  const batch = batchQuery.data?.data ?? null;
  const batchStrength = batch?.strength ?? 0;

  const list = useSections(batchIdNum);
  const create = useCreateSection(batchIdNum);
  const update = useUpdateSection(batchIdNum);
  const remove = useDeleteSection(batchIdNum);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];

  const allocated = rows.reduce((sum, s) => sum + (s.subStrength ?? 0), 0);
  const remaining = batchStrength - allocated;

  // Seats already used by OTHER sections (exclude the one being edited).
  const otherAllocated = editing
    ? allocated - (editing.subStrength ?? 0)
    : allocated;
  const remainingForForm = batchStrength - otherAllocated;

  const schema = useMemo(() => buildSectionSchema(remainingForForm), [remainingForForm]);

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
    <section>
      <HierarchyBreadcrumb
        items={[
          { label: 'Campuses', to: '/master-data/campus-hierarchy/campuses' },
          { label: batch ? `Batch ${batch.yearIdentifier}` : 'Sections' },
        ]}
      />
      <h1 className="page-title">Sections</h1>
      <p className="page-subtitle">
        {batch
          ? `Batch ${batch.yearIdentifier} — strength ${batchStrength}. Allocated to sections: ${allocated}. Remaining: ${Math.max(remaining, 0)}.`
          : 'Sections in the selected batch.'}
      </p>

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
        schema={schema}
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
    </section>
  );
}
