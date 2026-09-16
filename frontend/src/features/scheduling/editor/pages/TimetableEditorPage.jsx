import { useMemo, useReducer, useState } from 'react';
import { useParams } from 'react-router-dom';

import { useDraft, useSessions } from '@/features/scheduling/generation/api/useGeneration';
import {
  useConflictCheck,
  useDraftConflicts,
  useLockSession,
  useUnlockSession,
} from '../api/useConflictCheck';
import AlternativesPopover from '../components/AlternativesPopover';
import ConflictPanel from '../components/ConflictPanel';
import EditorGrid from '../components/EditorGrid';
import {
  buildDayAxis,
  buildGrid,
  buildSlotAxis,
  cellKey,
  emptyCells,
} from '../lib/grid-model';
import { deriveAlternatives, toPlacement } from '../lib/placement';
import '../timetable-editor.css';

// A4-15 §8 — editor orchestrator. Loads the draft + its sessions (A4-345 hooks), builds the
// day×slot grid with staged (unsaved) moves overlaid (OQ-1), runs the A4-16 conflict check
// on drop/keyboard-drop, and surfaces conflicts + client-derived alternatives. Concurrency
// is guarded by advisory lock/unlock on pick-up/drop (OQ-2). Nothing is persisted this
// story — "Save layout" is disabled and clearly explained (UC-2).

// Fetch a large first page of sessions; a draft's session set is bounded per department.
const SESSIONS_PAGE_SIZE = 200;

const initialState = {
  stagedMoves: {}, // { [sessionId]: { dayOfWeek, slotDefinitionId } }
  selectedSessionId: null,
  pendingCellKey: null,
  conflictCellKey: null,
  lastPlacementConflicts: [],
  notice: null, // { kind, text }
};

function reducer(state, action) {
  switch (action.type) {
    case 'select':
      return { ...state, selectedSessionId: action.sessionId };
    case 'clearSelect':
      return { ...state, selectedSessionId: null };
    case 'pending':
      return { ...state, pendingCellKey: action.key, conflictCellKey: null };
    case 'placed':
      return {
        ...state,
        stagedMoves: { ...state.stagedMoves, [action.sessionId]: action.target },
        pendingCellKey: null,
        conflictCellKey: null,
        lastPlacementConflicts: [],
        selectedSessionId: null,
        notice: { kind: 'success', text: 'Placement is conflict-free (staged, not yet saved).' },
      };
    case 'conflict':
      return {
        ...state,
        pendingCellKey: null,
        conflictCellKey: action.key,
        lastPlacementConflicts: action.conflicts,
        notice: { kind: 'error', text: `${action.conflicts.length} conflict(s) at the target slot.` },
      };
    case 'checkError':
      return {
        ...state,
        pendingCellKey: null,
        notice: { kind: 'error', text: action.text },
      };
    case 'notice':
      return { ...state, notice: action.notice };
    default:
      return state;
  }
}

export default function TimetableEditorPage() {
  const { draftId: draftIdParam } = useParams();
  const draftId = draftIdParam != null ? Number(draftIdParam) : null;

  const draftQuery = useDraft(draftId);
  const sessionsQuery = useSessions(draftId, { page: 0, size: SESSIONS_PAGE_SIZE });
  const draftConflicts = useDraftConflicts(draftId);

  const checkMut = useConflictCheck(draftId);
  const lockMut = useLockSession(draftId);
  const unlockMut = useUnlockSession(draftId);

  const [state, dispatch] = useReducer(reducer, initialState);
  const [altState, setAltState] = useState({ open: false, loading: false, items: [] });

  // The sessions payload is a Spring page ({ content, ... }) or a bare array depending on
  // the endpoint; normalize to an array.
  const sessions = useMemo(() => {
    const d = sessionsQuery.data;
    if (Array.isArray(d)) return d;
    return d?.content ?? d?.data ?? [];
  }, [sessionsQuery.data]);

  const dayAxis = useMemo(() => buildDayAxis(sessions), [sessions]);
  const slotAxis = useMemo(() => buildSlotAxis(sessions), [sessions]);
  const grid = useMemo(() => buildGrid(sessions, state.stagedMoves), [sessions, state.stagedMoves]);

  const selectedSession = sessions.find((s) => String(s.id) === String(state.selectedSessionId));
  const hasStaged = Object.keys(state.stagedMoves).length > 0;
  const draftVersion = draftQuery.data?.version;

  const cardState = (session, key) => {
    if (key === state.conflictCellKey) return 'conflict';
    if (key === state.pendingCellKey) return 'pending';
    if (state.stagedMoves[session.id]) return 'unsaved';
    if (session.isLocked) return 'locked';
    return 'normal';
  };

  const runCheck = async (session, targetCell) => {
    const built = toPlacement(session, targetCell);
    if (!built.ok) {
      dispatch({ type: 'checkError', text: built.error });
      return;
    }
    const key = cellKey(targetCell.dayOfWeek, targetCell.slotDefinitionId);
    dispatch({ type: 'pending', key });
    try {
      const conflicts = await checkMut.mutateAsync(built.request);
      if (Array.isArray(conflicts) && conflicts.length === 0) {
        dispatch({ type: 'placed', sessionId: session.id, target: targetCell });
      } else {
        dispatch({ type: 'conflict', key, conflicts: conflicts ?? [] });
      }
    } catch (err) {
      dispatch({ type: 'checkError', text: 'Conflict check failed. The move was not applied.' });
    } finally {
      if (session.id != null) unlockMut.mutate(session.id);
    }
  };

  const onPickUp = (session) => {
    dispatch({ type: 'select', sessionId: session.id });
    if (session.id != null) lockMut.mutate(session.id);
  };
  const onDragStartSession = (session) => {
    if (session.id != null) lockMut.mutate(session.id);
  };
  const onDropOnCell = (targetCell) => {
    if (!selectedSession) return;
    runCheck(selectedSession, targetCell);
  };

  const findConflicts = async () => {
    if (!selectedSession) return;
    setAltState({ open: true, loading: true, items: [] });
    const empties = emptyCells(dayAxis, slotAxis, grid);
    const origin = {
      dayOfWeek: selectedSession.dayOfWeek,
      slotDefinitionId: selectedSession.slotDefinitionId,
    };
    try {
      const items = await deriveAlternatives({
        session: selectedSession,
        origin,
        emptyList: empties,
        dayAxis,
        slotAxis,
        checkFn: (req) => checkMut.mutateAsync(req),
      });
      setAltState({ open: true, loading: false, items });
    } catch {
      setAltState({ open: true, loading: false, items: [] });
    }
  };

  const focusCell = (key) => {
    const el = document.querySelector(`[data-cell="${key}"]`);
    if (el) el.focus();
  };

  if (draftId == null) {
    return <p className="empty-state">No draft selected.</p>;
  }

  return (
    <div className="timetable-editor">
      <h1 className="page-title">Timetable Editor — Draft {draftId}</h1>
      <p className="page-subtitle">
        Drag a session (or select it and press Enter on a target cell) to check placement for
        conflicts in real time. Moves are staged locally.
      </p>

      {hasStaged && (
        <p className="editor-banner editor-banner--unsaved" role="status">
          You have unsaved staged moves. Saving to the draft requires a backend endpoint that
          is not available yet — moves are kept in this view only.
        </p>
      )}

      {state.notice && (
        <p className={`inline-notice inline-notice--${state.notice.kind}`} role="status">
          {state.notice.text}
        </p>
      )}

      <div className="editor-toolbar">
        <span className="editor-toolbar__version">Draft version: {draftVersion ?? '—'}</span>
        <button
          type="button"
          className="btn"
          onClick={findConflicts}
          disabled={!selectedSession || state.lastPlacementConflicts.length === 0}
        >
          Suggest alternatives
        </button>
        <button
          type="button"
          className="btn btn--primary"
          disabled
          title="Saving needs a backend session-move endpoint (not available yet)."
        >
          Save layout
        </button>
      </div>

      {sessionsQuery.isLoading && <p className="empty-state">Loading sessions…</p>}
      {sessionsQuery.isError && (
        <p className="form-error" role="alert">
          Could not load the draft sessions.{' '}
          <button type="button" className="link-btn" onClick={sessionsQuery.refetch}>
            Retry
          </button>
        </p>
      )}

      {!sessionsQuery.isLoading && !sessionsQuery.isError && (
        <div className="editor-layout">
          <EditorGrid
            dayAxis={dayAxis}
            slotAxis={slotAxis}
            grid={grid}
            selectedSessionId={state.selectedSessionId}
            conflictCellKey={state.conflictCellKey}
            pendingCellKey={state.pendingCellKey}
            cardState={cardState}
            onPickUp={onPickUp}
            onDragStartSession={onDragStartSession}
            onDropOnCell={onDropOnCell}
          />

          <ConflictPanel
            lastPlacementConflicts={state.lastPlacementConflicts}
            draftConflicts={draftConflicts.data}
            isLoading={draftConflicts.isLoading}
            onFocusCell={focusCell}
          />
        </div>
      )}

      <AlternativesPopover
        open={altState.open}
        loading={altState.loading}
        alternatives={altState.items}
        onPick={(alt) => {
          setAltState({ open: false, loading: false, items: [] });
          if (selectedSession) runCheck(selectedSession, alt);
        }}
        onClose={() => setAltState({ open: false, loading: false, items: [] })}
      />
    </div>
  );
}
