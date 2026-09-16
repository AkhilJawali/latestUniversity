import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import ConfigFormModal from '@/features/scheduling-config/components/ConfigFormModal';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import {
  useBatches,
  useCreateBatch,
  useDeleteBatch,
  useUpdateBatch,
} from '../api/useBatches';
import HierarchyBreadcrumb from '../components/HierarchyBreadcrumb';
import { batchSchema } from '../schemas/hierarchy-schemas';

const FIELDS = [
  { name: 'yearIdentifier', label: 'Year Identifier', type: 'text', required: true },
  { name: 'strength', label: 'Strength', type: 'number', required: true },
  { name: 'electiveBasket', label: 'Elective Basket', type: 'text' },
];
const BLANK = { yearIdentifier: '', strength: '', electiveBasket: '' };

export default function BatchesPage() {
  const { programId } = useParams();
  const programIdNum = Number(programId);
  const navigate = useNavigate();

  const list = useBatches(programIdNum);
  const create = useCreateBatch(programIdNum);
  const update = useUpdateBatch(programIdNum);
  const remove = useDeleteBatch(programIdNum);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];
  const initialValues = useMemo(
    () => (editing ? { ...BLANK, ...editing } : { ...BLANK, programId: programIdNum }),
    [editing, programIdNum],
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
    { key: 'yearIdentifier', header: 'Year' },
    { key: 'strength', header: 'Strength' },
    { key: 'electiveBasket', header: 'Elective Basket' },
    {
      key: '_open',
      header: '',
      render: (row) => (
        <button
          type="button"
          className="link-btn"
          onClick={() => navigate(`/master-data/campus-hierarchy/batches/${row.id}/sections`)}
        >
          View sections →
        </button>
      ),
    },
  ];

  return (
    <section>
      <HierarchyBreadcrumb
        items={[
          { label: 'Campuses', to: '/master-data/campus-hierarchy/campuses' },
          { label: 'Batches' },
        ]}
      />
      <h1 className="page-title">Batches</h1>
      <p className="page-subtitle">
        Batches under the selected program. Open a batch to manage its sections.
      </p>

      <ConfigTable
        entityLabel="Batches"
        addLabel="Add batch"
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
    </section>
  );
}
