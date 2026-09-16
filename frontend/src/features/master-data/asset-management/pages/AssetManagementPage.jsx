import { useMemo, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import { useAssets, useCreateAsset, useDeleteAsset, useUpdateAsset } from '../api/useAssets';
import AssetFormModal from '../components/AssetFormModal';
import '../asset-management.css';

// A4-435 §5.4 — Schedulable-asset management page. Server-side filters (campus,
// department, assetType) drive the paged /assets query (PD-3), with real pagination
// from meta (PD-6). Create/edit via the custom AssetFormModal (embedded windows editor,
// whole-set replacement), soft-delete via the reused confirm dialog.
const PAGE_SIZE = 20;

export default function AssetManagementPage() {
  const [campusId, setCampusId] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [assetType, setAssetType] = useState('');
  const [page, setPage] = useState(0);

  const params = useMemo(() => {
    const p = { page, size: PAGE_SIZE };
    if (campusId) p.campusId = Number(campusId);
    if (departmentId) p.departmentId = Number(departmentId);
    if (assetType.trim()) p.assetType = assetType.trim();
    return p;
  }, [campusId, departmentId, assetType, page]);

  const assets = useAssets(params);
  const createMut = useCreateAsset();
  const updateMut = useUpdateAsset();
  const deleteMut = useDeleteAsset();

  const campuses = useCampuses();
  const filterDepartments = useDepartments(campusId ? Number(campusId) : null);
  const campusRows = campuses.data?.data ?? [];
  const deptRows = filterDepartments.data?.data ?? [];

  const rows = assets.data?.data ?? [];
  const meta = assets.data?.meta ?? { page: 0, totalPages: 0, totalElements: 0 };

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'identifier', header: 'Identifier' },
    { key: 'assetType', header: 'Type' },
    { key: 'owningDepartmentName', header: 'Department' },
    { key: 'campusName', header: 'Campus' },
    {
      key: 'availabilityWindows',
      header: 'Windows',
      render: (r) => `${r.availabilityWindows?.length ?? 0} windows`,
    },
  ];

  const isEditing = Boolean(editing);
  const initialValues = useMemo(() => {
    if (editing) {
      return {
        identifier: editing.identifier ?? '',
        name: editing.name ?? '',
        assetType: editing.assetType ?? '',
        availabilityWindows: (editing.availabilityWindows ?? []).map((w) => ({
          dayOfWeek: w.dayOfWeek ?? '',
          startTime: (w.startTime ?? '').slice(0, 5),
          endTime: (w.endTime ?? '').slice(0, 5),
        })),
      };
    }
    return {
      name: '',
      identifier: '',
      assetType: '',
      campusId: '',
      owningDepartmentId: '',
      availabilityWindows: [],
    };
  }, [editing]);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };
  const openEdit = (row) => {
    setEditing(row);
    setModalOpen(true);
  };
  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
  };

  const submitForm = (data, handlers) => {
    if (editing) {
      updateMut.mutate({ id: editing.id, ...data }, { ...handlers, onSuccess: closeModal });
    } else {
      createMut.mutate(data, { ...handlers, onSuccess: closeModal });
    }
  };

  const confirmDelete = () => {
    setDeleteError(null);
    deleteMut.mutate(toDelete.id, {
      onSuccess: () => setToDelete(null),
      onError: (err) => setDeleteError(mapApiError(err).message ?? 'Could not delete this asset.'),
    });
  };
  const cancelDelete = () => {
    setToDelete(null);
    setDeleteError(null);
  };

  const onFilterChange = (setter) => (value) => {
    setter(value);
    setPage(0);
  };

  return (
    <section className="asset-management">
      <h1 className="page-title">Schedulable Asset Management</h1>
      <p className="page-subtitle">
        Manage non-room schedulable assets — owning department, campus, and per-asset availability windows.
      </p>

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="af-campus">Campus</label>
          <select
            id="af-campus"
            value={campusId}
            onChange={(e) => {
              onFilterChange(setCampusId)(e.target.value);
              setDepartmentId('');
            }}
          >
            <option value="">All campuses</option>
            {campusRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="af-dept">Department</label>
          <select
            id="af-dept"
            value={departmentId}
            onChange={(e) => onFilterChange(setDepartmentId)(e.target.value)}
            disabled={!campusId || filterDepartments.isLoading}
          >
            <option value="">All departments</option>
            {deptRows.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="af-type">Asset type</label>
          <input
            id="af-type"
            value={assetType}
            onChange={(e) => onFilterChange(setAssetType)(e.target.value)}
            placeholder="e.g. PROJECTOR_SET"
          />
        </div>
      </div>

      <ConfigTable
        entityLabel="Assets"
        addLabel="Add asset"
        columns={columns}
        rows={rows}
        isLoading={assets.isLoading}
        isError={assets.isError}
        onRetry={assets.refetch}
        onAdd={openCreate}
        onEdit={openEdit}
        onDelete={(row) => {
          setDeleteError(null);
          setToDelete(row);
        }}
        searchable={false}
      />

      {meta.totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="btn"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <span className="page-info">
            Page {meta.page + 1} of {meta.totalPages} ({meta.totalElements} assets)
          </span>
          <button
            type="button"
            className="btn"
            disabled={page >= meta.totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}

      <AssetFormModal
        open={modalOpen}
        mode={isEditing ? 'edit' : 'create'}
        initialValues={initialValues}
        currentDepartmentName={editing?.owningDepartmentName ?? ''}
        currentCampusName={editing?.campusName ?? ''}
        isPending={createMut.isPending || updateMut.isPending}
        onSubmit={submitForm}
        onClose={closeModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={
          deleteError
            ? `asset ${toDelete?.identifier}. ${deleteError}`
            : toDelete
              ? `asset ${toDelete.identifier} — ${toDelete.name}`
              : 'this asset'
        }
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </section>
  );
}
