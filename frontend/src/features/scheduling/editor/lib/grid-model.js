// A4-15 §7 (PD-A15-1/PD-A15-7) — grid model helpers. Builds a (day × slot) index of the
// draft's sessions with staged (unsaved) moves overlaid, derives the day/slot axes, and
// enumerates empty cells for alternative-slot probing. Pure functions — no I/O.

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

export const cellKey = (day, slotId) => `${day}|${slotId}`;

// A staged move overrides a session's day/slot without persisting (OQ-1). stagedMoves is a
// plain object { [sessionId]: { dayOfWeek, slotDefinitionId } }.
export function effectivePosition(session, stagedMoves) {
  const staged = stagedMoves?.[session.id];
  return {
    dayOfWeek: staged?.dayOfWeek ?? session.dayOfWeek,
    slotDefinitionId: staged?.slotDefinitionId ?? session.slotDefinitionId,
  };
}

// Day axis: standard working days plus any extra day present in the sessions, preserving
// the standard order first (PD-A15-7).
export function buildDayAxis(sessions) {
  const present = new Set((sessions ?? []).map((s) => s.dayOfWeek).filter(Boolean));
  const extra = [...present].filter((d) => !DAYS.includes(d)).sort();
  return [...DAYS, ...extra];
}

// Slot axis: distinct slotDefinitionIds across sessions, numeric-ascending (best-effort
// ordering when the campus grid's start times are not available — PD-A15-7).
export function buildSlotAxis(sessions) {
  const ids = new Set((sessions ?? []).map((s) => s.slotDefinitionId).filter((v) => v != null));
  return [...ids].sort((a, b) => Number(a) - Number(b));
}

// Map of cellKey -> session, applying staged overrides over the fetched positions.
export function buildGrid(sessions, stagedMoves) {
  const grid = new Map();
  (sessions ?? []).forEach((s) => {
    const pos = effectivePosition(s, stagedMoves);
    if (pos.dayOfWeek != null && pos.slotDefinitionId != null) {
      grid.set(cellKey(pos.dayOfWeek, pos.slotDefinitionId), s);
    }
  });
  return grid;
}

// Empty (day, slot) cells given the axes and the current occupied grid.
export function emptyCells(dayAxis, slotAxis, grid) {
  const out = [];
  dayAxis.forEach((day) => {
    slotAxis.forEach((slotId) => {
      if (!grid.has(cellKey(day, slotId))) out.push({ dayOfWeek: day, slotDefinitionId: slotId });
    });
  });
  return out;
}
