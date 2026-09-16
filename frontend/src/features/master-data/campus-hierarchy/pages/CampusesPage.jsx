import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

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

// Routed top-level page: lists campuses; each row links to its departments page.
// Code (not numeric id) is the visible identifier per the UX requirement.
const CREATE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'code', label: 'Code', type: 'text', required: true },
  { name: 'location', label: 'Location', type: 'text', required: true },
];

// Code is immutable on edit (backend campus update covers name + location only).
const EDIT_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'location', label: 'Location', type: 'text', required: true },
];

const BLANK = { name: '', code: '', location: '' };

export default function CampusesPage() {
  const navigate = useNavigate();
  const list = useCampuses();
  const create = useCreateCampus();
  const update = useUpdateCampus();
  const remove = useDeleteCampus();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const rows = list.data?.data ?? [];
  const initialValues = useMemo(() => (editing ? { ...BLANK, ...editing } : BLANK), [editing]);

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
    { key: 'code', header: 'Code' },
    { key: 'name', header: 'Name' },
    { key: 'location', header: 'Location' },
    {
      key: '_open',
      header: '',
      render: (row) => (
        <button
          type="button"
          className="link-btn"
          onClick={() => navigate(`/master-data/campus-hierarchy/campuses/${row.id}/departments`)}
        >
          View departments →
        </button>
      ),
    },
  ];

  return (
    <section>
      <h1 className="page-title">Campuses</h1>
      <p className="page-subtitle">
        Top level of the institution hierarchy. Open a campus to manage its departments.
      </p>

      <ConfigTable
        entityLabel="Campuses"
        addLabel="Add campus"
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
        label={deleting ? `campus "${deleting.code}"` : ''}
        isPending={remove.isPending}
        onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    </section>
  );
}
