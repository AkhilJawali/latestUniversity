/**
 * A4-430 §5.4 — Room management page.
 * Server-side filters (campus, type, building, minCapacity, equipmentTag) + pagination.
 */
import { useMemo, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import ConfigTable from '@/features/scheduling-config/components/ConfigTable';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import { useRooms, useCreateRoom, useUpdateRoom, useDeleteRoom } from '../api/useRooms';
import RoomFormModal from '../components/RoomFormModal';
import { ROOM_TYPES, ROOM_TYPE_LABELS } from '../constants/room-options';
import '../room-management.css';

const PAGE_SIZE = 20;

export default function RoomManagementPage() {
  // Filters
  const [campusId, setCampusId] = useState('');
  const [roomType, setRoomType] = useState('');
  const [building, setBuilding] = useState('');
  const [minCapacity, setMinCapacity] = useState('');
  const [equipmentTag, setEquipmentTag] = useState('');
  const [page, setPage] = useState(0);

  // Query params
  const params = useMemo(() => {
    const p = { page, size: PAGE_SIZE };
    if (campusId) p.campusId = campusId;
    if (roomType) p.roomType = roomType;
    if (building) p.building = building;
    if (minCapacity) p.minCapacity = Number(minCapacity);
    if (equipmentTag) p.equipmentTag = equipmentTag;
    return p;
  }, [campusId, roomType, building, minCapacity, equipmentTag, page]);

  const roomsQuery = useRooms(params);
  const createMut = useCreateRoom();
  const updateMut = useUpdateRoom();
  const deleteMut = useDeleteRoom();

  // Reference data
  const campusesQuery = useCampuses();
  const campusRows = campusesQuery.data?.data ?? [];

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const rows = roomsQuery.data?.data ?? [];
  const meta = roomsQuery.data?.meta;

  // Derive building options from current rows
  const buildingOptions = useMemo(() => {
    const set = new Set();
    rows.forEach((r) => r.building && set.add(r.building));
    return Array.from(set).sort();
  }, [rows]);

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'code', header: 'Code' },
    { key: 'campusName', header: 'Campus' },
    { key: 'capacity', header: 'Capacity' },
    {
      key: 'roomType',
      header: 'Type',
      filterable: true,
      render: (r) => ROOM_TYPE_LABELS[r.roomType] ?? r.roomType,
    },
    {
      key: 'equipmentTags',
      header: 'Equipment',
      render: (r) => (r.equipmentTags?.length ? r.equipmentTags.join(', ') : '—'),
    },
    { key: 'building', header: 'Building', render: (r) => r.building ?? '—' },
    { key: 'floor', header: 'Floor', render: (r) => r.floor ?? '—' },
  ];

  const isEditing = Boolean(editing);
  const initialValues = useMemo(() => {
    if (editing) {
      return {
        name: editing.name ?? '',
        code: editing.code ?? '',
        campusId: editing.campusId ?? '',
        campusName: editing.campusName ?? '',
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

  const isPending = createMut.isPending || updateMut.isPending;

  return (
    <section className="room-management">
      <h1 className="page-title">Room &amp; Lab Management</h1>
      <p className="page-subtitle">Manage rooms, labs, and their equipment tags.</p>

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="filter-campus">Campus</label>
          <select
            id="filter-campus"
            value={campusId}
            onChange={(e) => {
              setCampusId(e.target.value);
              setPage(0);
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
          <label htmlFor="filter-type">Room Type</label>
          <select
            id="filter-type"
            value={roomType}
            onChange={(e) => {
              setRoomType(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All types</option>
            {ROOM_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="filter-building">Building</label>
          <select
            id="filter-building"
            value={building}
            onChange={(e) => {
              setBuilding(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All buildings</option>
            {buildingOptions.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-field">
          <label htmlFor="filter-capacity">Min Capacity</label>
          <input
            id="filter-capacity"
            type="number"
            min="1"
            value={minCapacity}
            onChange={(e) => {
              setMinCapacity(e.target.value);
              setPage(0);
            }}
            placeholder="e.g., 30"
          />
        </div>

        <div className="filter-field">
          <label htmlFor="filter-tag">Equipment Tag</label>
          <input
            id="filter-tag"
            type="text"
            value={equipmentTag}
            onChange={(e) => {
              setEquipmentTag(e.target.value);
              setPage(0);
            }}
            placeholder="e.g., computer_lab"
          />
        </div>
      </div>

      <div className="table-actions">
        <button type="button" className="btn btn--primary" onClick={openCreate}>
          Add Room
        </button>
      </div>

      <ConfigTable
        columns={columns}
        rows={rows}
        isLoading={roomsQuery.isLoading}
        error={roomsQuery.error?.message}
        onEdit={openEdit}
        onDelete={(row) => setToDelete(row)}
      />

      {meta && meta.totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Previous
          </button>
          <span className="pagination-info">
            Page {page + 1} of {meta.totalPages} ({meta.totalElements} rooms)
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
        campuses={campusRows}
        isPending={isPending}
        onSubmit={submitForm}
        onClose={closeModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(toDelete)}
        label={toDelete?.name ?? 'this room'}
        isPending={deleteMut.isPending}
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
      {deleteError && (
        <p className="form-message form-message--error" role="alert">
          {deleteError}
        </p>
      )}
    </section>
  );
}
