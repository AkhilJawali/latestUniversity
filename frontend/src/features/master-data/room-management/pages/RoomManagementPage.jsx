import { useMemo, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import { useCreateRoom, useDeleteRoom, useRooms, useUpdateRoom } from '../api/useRooms';
import RoomFormModal from '../components/RoomFormModal';
import { ROOM_TYPES } from '../constants/room-options';
import '../room-management.css';

// A4-430 §5.4 — Room management page. Server-side filters (campus, type, building,
// minCapacity, single equipment tag) drive the paged /rooms query (PD-3), with real
// pagination controls from meta (PD-6). Create/edit via the custom RoomFormModal,
// soft-delete via the reused confirm dialog.
const PAGE_SIZE = 20;

export default function RoomManagementPage() {
  // Filters.
  const [campusId, setCampusId] = useState('');
  const [roomType, setRoomType] = useState('');
  const [building, setBuilding] = useState('');
  const [minCapacity, setMinCapacity] = useState('');
  const [equipmentTag, setEquipmentTag] = useState('');
  const [page, setPage] = useState(0);

  const params = useMemo(() => {
    const p = { page, size: PAGE_SIZE };
    if (campusId) p.campusId = Number(campusId);
    if (roomType) p.roomType = roomType;
    if (building.trim()) p.building = building.trim();
    if (minCapacity) p.minCapacity = Number(minCapacity);
    if (equipmentTag.trim()) p.equipmentTag = equipmentTag.trim();
    return p;
  }, [campusId, roomType, building, minCapacity, equipmentTag, page]);

  const rooms = useRooms(params);
  const createMut = useCreateRoom();
  const updateMut = useUpdateRoom();
  const deleteMut = useDeleteRoom();

  const campuses = useCampuses();
  const campusRows = campuses.data?.data ?? [];

  const rows = rooms.data?.data ?? [];
  const meta = rooms.data?.meta ?? { page: 0, totalPages: 0, totalElements: 0 };

  // Modal / dialog state.
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'code', header: 'Code' },
    { key: 'campusName', header: 'Campus' },
    { key: 'capacity', header: 'Capacity' },
    { key: 'roomType', header: 'Type' },
    {
      key: 'equipmentTags',
      header: 'Equipment',
      render: (r) => (r.equipmentTags?.length ? r.equipmentTags.join(', ') : '—'),
    },
    { key: 'building', header: 'Building', render: (r) => r.building || '—' },
    { key: 'floor', header: 'Floor', render: (r) => r.floor || '—' },
  ];

  const isEditing = Boolean(editing);
  const initialValues = useMemo(() => {
    if (editing) {
      return {
        code: editing.code ?? '',
        name: editing.name ?? '',
        capacity: editing.capacity ?? '',
        roomType: editing.roomType ?? '',
        equipmentTags: editing.equipmentTags ?? [],
        building: editing.building ?? '',
        floor: editing.floor ?? '',
      };
    }
    return {
      name: '',
      code: '',
      campusId: '',
      capacity: '',
      roomType: '',
      equipmentTags: [],
      building: '',
      floor: '',
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
      onError: (err) => setDeleteError(mapApiError(err).message ?? 'Could not delete this room.'),
    });
  };
  const cancelDelete = () => {
    setToDelete(null);
    setDeleteError(null);
  };

  // Reset to page 0 whenever a filter changes.
  const onFilterChange = (setter) => (value) => {
    setter(value);
    setPage(0);
  };

  return (
    <section className="room-management">
      <h1 className="page-title">Room &amp; Lab Management</h1>
      <p className="page-subtitle">
        Manage rooms and labs — capacity, type, equipment tags, and campus/building/floor.
      </p>

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="rf-campus">Campus</label>
          <select
            id="rf-campus"
            value={campusId}
            onChange={(e) => onFilterChange(setCampusId)(e.target.value)}
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
          <label htmlFor="rf-type">Type</label>
          <select
            id="rf-type"
            value={roomType}
            onChange={(e) => onFilterChange(setRoomType)(e.target.value)}
          >
            <option value="">All types</option>
            {ROOM_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="rf-building">Building</label>
          <input
            id="rf-building"
            value={building}
            onChange={(e) => onFilterChange(setBuilding)(e.target.value)}
            placeholder="Exact building"
          />
        </div>

        <div className="filter-field">
          <label htmlFor="rf-mincap">Min capacity</label>
          <input
            id="rf-mincap"
            type="number"
            min="1"
            value={minCapacity}
            onChange={(e) => onFilterChange(setMinCapacity)(e.target.value)}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="rf-tag">Equipment tag</label>
          <input
            id="rf-tag"
            value={equipmentTag}
            onChange={(e) => onFilterChange(setEquipmentTag)(e.target.value)}
            placeholder="e.g. computer_lab"
          />
        </div>
      </div>

      <ConfigTable
        entityLabel="Rooms"
        addLabel="Add room"
        columns={columns}
        rows={rows}
        isLoading={rooms.isLoading}
        isError={rooms.isError}
        onRetry={rooms.refetch}
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
            Page {meta.page + 1} of {meta.totalPages} ({meta.totalElements} rooms)
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

      <RoomFormModal
        open={modalOpen}
        mode={isEditing ? 'edit' : 'create'}
        initialValues={initialValues}
        currentCampusName={editing?.campusName ?? ''}
        campuses={campusRows}
        isPending={createMut.isPending || updateMut.isPending}
        onSubmit={submitForm}
        onClose={closeModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={
          deleteError
            ? `room ${toDelete?.code}. ${deleteError}`
            : toDelete
              ? `room ${toDelete.code} — ${toDelete.name}`
              : 'this room'
        }
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
    </section>
  );
}
