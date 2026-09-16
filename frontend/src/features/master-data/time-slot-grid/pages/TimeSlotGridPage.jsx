import { useEffect, useMemo, useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { mapApiError } from '@/lib/api-error';
import { useEffectiveSlots, useRemoveSlot } from '../api/useSlotDefinitions';
import {
  useCreateGrid,
  useDeleteGrid,
  useGridByCampus,
  useRenameGrid,
} from '../api/useTimeSlotGrid';
import GridCreateModal from '../components/GridCreateModal';
import SlotDefinitionModal from '../components/SlotDefinitionModal';
import SlotTimeline from '../components/SlotTimeline';
import { ALL_DAYS, WEEKDAY_OPTIONS } from '../constants/time-slot-options';
import { gridRenameSchema } from '../schemas/time-slot-schemas';
import '../time-slot-grid.css';

// A4-445 §5.6 — Time-Slot Grid admin page. Campus-scoped: pick a campus, then manage its
// single grid. No grid (404→null) => empty state + Create CTA; grid present => header with
// inline rename + delete, an Add slot button, a per-day preview selector, and the slot
// timeline. Slot edit = remove-then-add via SlotDefinitionModal (OQ-1). Inline status
// banner (role="status") stands in for toasts (OQ-6). Create is hidden when a grid exists
// (FR-2.6).
export default function TimeSlotGridPage() {
  const [campusId, setCampusId] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [slotModal, setSlotModal] = useState({ open: false, mode: 'add', initialValues: null });
  const [slotToRemove, setSlotToRemove] = useState(null);
  const [removeError, setRemoveError] = useState(null);
  const [gridDeleteOpen, setGridDeleteOpen] = useState(false);
  const [gridDeleteError, setGridDeleteError] = useState(null);
  const [renaming, setRenaming] = useState(false);
  const [renameValue, setRenameValue] = useState('');
  const [renameError, setRenameError] = useState(null);
  const [previewDay, setPreviewDay] = useState(ALL_DAYS);
  const [notice, setNotice] = useState(null);

  const numericCampusId = campusId ? Number(campusId) : null;

  const campuses = useCampuses();
  const campusRows = campuses.data?.data ?? [];

  const gridQuery = useGridByCampus(numericCampusId);
  const grid = gridQuery.data?.data ?? null;
  const gridId = grid?.id ?? null;

  const createMut = useCreateGrid(numericCampusId);
  const renameMut = useRenameGrid(numericCampusId);
  const deleteGridMut = useDeleteGrid(numericCampusId);
  const removeSlotMut = useRemoveSlot(gridId, numericCampusId);

  const previewActive = Boolean(previewDay);
  const effective = useEffectiveSlots(gridId, previewDay);
  const timelineSlots = previewActive ? effective.data?.data ?? [] : grid?.slots ?? [];

  useEffect(() => {
    if (notice?.kind === 'success') {
      const t = setTimeout(() => setNotice(null), 5000);
      return () => clearTimeout(t);
    }
    return undefined;
  }, [notice]);

  const pushToast = (kind, text) => setNotice({ kind, text });

  const changeCampus = (value) => {
    setCampusId(value);
    setRenaming(false);
    setPreviewDay(ALL_DAYS);
    setSlotToRemove(null);
    setRemoveError(null);
  };

  const submitCreate = (body, handlers) =>
    createMut.mutate(body, {
      ...handlers,
      onSuccess: () => {
        setCreateOpen(false);
        pushToast('success', 'Grid created.');
      },
    });

  const openAddSlot = () => setSlotModal({ open: true, mode: 'add', initialValues: null });
  const openEditSlot = (slot) =>
    setSlotModal({
      open: true,
      mode: 'edit',
      initialValues: {
        id: slot.id,
        startTime: String(slot.startTime ?? '').slice(0, 5),
        endTime: String(slot.endTime ?? '').slice(0, 5),
        slotType: slot.slotType ?? 'TEACHING',
        applicableDay: slot.applicableDay ?? '',
        label: slot.label ?? '',
      },
    });
  const closeSlotModal = () => setSlotModal((m) => ({ ...m, open: false }));

  const startRename = () => {
    setRenameValue(grid.gridName ?? '');
    setRenameError(null);
    setRenaming(true);
  };
  const submitRename = (e) => {
    e.preventDefault();
    const result = validateWith(gridRenameSchema, { gridName: renameValue });
    if (!result.success) {
      setRenameError(result.errors.gridName);
      return;
    }
    renameMut.mutate(
      { id: gridId, gridName: result.data.gridName },
      {
        onSuccess: () => {
          setRenaming(false);
          pushToast('success', 'Grid renamed.');
        },
        onError: (err) => setRenameError(mapApiError(err).message ?? 'Could not rename the grid.'),
      },
    );
  };

  const confirmRemoveSlot = () => {
    setRemoveError(null);
    removeSlotMut.mutate(slotToRemove.id, {
      onSuccess: () => {
        setSlotToRemove(null);
        pushToast('success', 'Slot removed.');
      },
      onError: (err) => setRemoveError(mapApiError(err).message ?? 'Could not remove this slot.'),
    });
  };
  const cancelRemoveSlot = () => {
    setSlotToRemove(null);
    setRemoveError(null);
  };

  const confirmDeleteGrid = () => {
    setGridDeleteError(null);
    deleteGridMut.mutate(gridId, {
      onSuccess: () => {
        setGridDeleteOpen(false);
        pushToast('success', 'Grid deleted.');
      },
      onError: (err) =>
        setGridDeleteError(mapApiError(err).message ?? 'Could not delete the grid.'),
    });
  };

  const dayLabel = useMemo(
    () => WEEKDAY_OPTIONS.find((d) => d.value === previewDay)?.label ?? '',
    [previewDay],
  );

  return (
    <section className="time-slot-grid">
      <h1 className="page-title">Time-Slot Grid Configuration</h1>
      <p className="page-subtitle">
        Configure the per-campus daily time-slot grid — teaching, break, and lunch slots with
        mixed durations and optional day overrides.
      </p>

      {notice && (
        <p className={`inline-notice inline-notice--${notice.kind}`} role="status">
          {notice.text}
        </p>
      )}

      <div className="filters-bar">
        <div className="filter-field">
          <label htmlFor="tsg-campus">Campus</label>
          <select id="tsg-campus" value={campusId} onChange={(e) => changeCampus(e.target.value)}>
            <option value="">Select a campus…</option>
            {campusRows.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {!campusId && <p className="empty-state">Select a campus to configure its time-slot grid.</p>}

      {campusId && gridQuery.isLoading && <p className="empty-state">Loading grid…</p>}

      {campusId && !gridQuery.isLoading && grid === null && (
        <div className="card">
          <p className="empty-state">
            This campus has no time-slot grid yet.{' '}
            <button type="button" className="link-btn" onClick={() => setCreateOpen(true)}>
              Create grid
            </button>
          </p>
        </div>
      )}

      {campusId && grid && (
        <section className="card" aria-label="Time-slot grid">
          <div className="section-head grid-header">
            {renaming ? (
              <form className="rename-form" onSubmit={submitRename}>
                <label htmlFor="grid-rename" className="sr-only">
                  Grid name
                </label>
                <input
                  id="grid-rename"
                  value={renameValue}
                  onChange={(e) => setRenameValue(e.target.value)}
                  aria-invalid={Boolean(renameError)}
                />
                <button type="submit" className="btn btn--primary" disabled={renameMut.isPending}>
                  {renameMut.isPending ? 'Saving…' : 'Save'}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => setRenaming(false)}
                  disabled={renameMut.isPending}
                >
                  Cancel
                </button>
                {renameError && <p className="field-error">{renameError}</p>}
              </form>
            ) : (
              <h2>{grid.gridName}</h2>
            )}
            <div className="grid-actions">
              {!renaming && (
                <button type="button" className="btn" onClick={startRename}>
                  Rename
                </button>
              )}
              <button type="button" className="btn btn--primary" onClick={openAddSlot}>
                Add slot
              </button>
              <button
                type="button"
                className="btn btn--danger"
                onClick={() => {
                  setGridDeleteError(null);
                  setGridDeleteOpen(true);
                }}
              >
                Delete grid
              </button>
            </div>
          </div>

          <div className="preview-bar">
            <div className="filter-field">
              <label htmlFor="tsg-preview-day">View by day</label>
              <select
                id="tsg-preview-day"
                value={previewDay}
                onChange={(e) => setPreviewDay(e.target.value)}
              >
                <option value={ALL_DAYS}>All slots (editable)</option>
                {WEEKDAY_OPTIONS.map((d) => (
                  <option key={d.value} value={d.value}>
                    {d.label} (preview)
                  </option>
                ))}
              </select>
            </div>
            {previewActive && (
              <p className="preview-note">
                Previewing effective slots for {dayLabel}. Editing is disabled in preview — switch to
                “All slots” to add, edit, or remove.
              </p>
            )}
          </div>

          {previewActive && effective.isLoading ? (
            <p className="empty-state">Loading preview…</p>
          ) : (
            <SlotTimeline
              slots={timelineSlots}
              preview={previewActive}
              onEdit={openEditSlot}
              onRemove={(slot) => {
                setRemoveError(null);
                setSlotToRemove(slot);
              }}
            />
          )}
        </section>
      )}

      <GridCreateModal
        open={createOpen}
        campusId={numericCampusId}
        isPending={createMut.isPending}
        onSubmit={submitCreate}
        onClose={() => setCreateOpen(false)}
      />

      <SlotDefinitionModal
        open={slotModal.open}
        mode={slotModal.mode}
        gridId={gridId}
        campusId={numericCampusId}
        initialValues={slotModal.initialValues}
        onDone={(msg) => {
          closeSlotModal();
          pushToast('success', msg);
        }}
        onClose={closeSlotModal}
      />

      <ConfirmDeleteDialog
        open={Boolean(slotToRemove)}
        label={
          removeError
            ? `slot. ${removeError}`
            : slotToRemove
              ? `the ${String(slotToRemove.startTime ?? '').slice(0, 5)}–${String(slotToRemove.endTime ?? '').slice(0, 5)} slot`
              : 'this slot'
        }
        isPending={removeSlotMut.isPending}
        onConfirm={confirmRemoveSlot}
        onCancel={cancelRemoveSlot}
      />

      <ConfirmDeleteDialog
        open={gridDeleteOpen}
        label={
          gridDeleteError
            ? `grid. ${gridDeleteError}`
            : grid
              ? `grid "${grid.gridName}" and all its slots`
              : 'this grid'
        }
        isPending={deleteGridMut.isPending}
        onConfirm={confirmDeleteGrid}
        onCancel={() => {
          setGridDeleteOpen(false);
          setGridDeleteError(null);
        }}
      />
    </section>
  );
}
