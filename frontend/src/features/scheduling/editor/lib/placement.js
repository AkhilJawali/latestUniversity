import { z } from 'zod';

import { cellKey } from './grid-model';

// Constants previously in conflict-types.js
const DEFAULT_SLOT_DURATION_MINUTES = 60;
const MAX_ALTERNATIVE_PROBES = 10;

// A4-15 §7 — placement helpers. Turn a (session, target cell) into the A4-16
// ProposedPlacementRequest, validate it before any request (AC-5), resolve the slot
// duration (OQ-5/PD-A15-6 fallback), and derive candidate alternative slots by probing
// empty cells with the same conflict-check (OQ-3/PD-A15-4).

// Mirrors ProposedPlacementRequest bounds (required/positive; §8 of the requirement).
export const placementSchema = z.object({
  facultyId: z.number({ invalid_type_error: 'facultyId required' }).int().positive(),
  roomId: z.number().int().positive(),
  batchId: z.number().int().positive(),
  sectionId: z.number().int().positive().nullable().optional(),
  dayOfWeek: z.string().min(1),
  slotDefinitionId: z.number().int().positive(),
  durationMinutes: z.number().int().positive(),
  sessionId: z.number().int().positive().nullable().optional(),
});

const num = (v) => (v == null || v === '' ? null : Number(v));

// Resolve the slot duration from the campus grid slot map when available, else the default
// (PD-A15-6). slotDurations is an optional { [slotDefinitionId]: minutes } lookup.
export function resolveDuration(slotDefinitionId, slotDurations) {
  const d = slotDurations?.[slotDefinitionId];
  return d != null && Number(d) > 0 ? Number(d) : DEFAULT_SLOT_DURATION_MINUTES;
}

// Build a ProposedPlacementRequest from a session moved onto targetCell. The moved
// session's id is always sent as sessionId so it is self-excluded from occupancy
// (PD-A15-3). Returns { ok, request } | { ok:false, error }.
export function toPlacement(session, targetCell, slotDurations) {
  const request = {
    facultyId: num(session.facultyId),
    roomId: num(session.roomId),
    batchId: num(session.batchId),
    sectionId: num(session.sectionId),
    dayOfWeek: targetCell.dayOfWeek,
    slotDefinitionId: num(targetCell.slotDefinitionId),
    durationMinutes: resolveDuration(targetCell.slotDefinitionId, slotDurations),
    sessionId: num(session.id),
  };
  const parsed = placementSchema.safeParse(request);
  if (!parsed.success) {
    const first = parsed.error.issues[0];
    return { ok: false, error: first ? `${first.path.join('.')}: ${first.message}` : 'Invalid placement' };
  }
  return { ok: true, request: parsed.data };
}

// Manhattan-style proximity between two cells within the axes (same-day distance is 0 on
// the day dimension), used to order alternatives nearest-first (PD-A15-4).
function proximity(from, to, dayAxis, slotAxis) {
  const dDay = Math.abs(dayAxis.indexOf(from.dayOfWeek) - dayAxis.indexOf(to.dayOfWeek));
  const dSlot = Math.abs(slotAxis.indexOf(from.slotDefinitionId) - slotAxis.indexOf(to.slotDefinitionId));
  return dDay * 100 + dSlot; // day distance dominates
}

// Derive candidate alternative slots for a conflicted session. Probes empty cells
// (nearest first, capped at MAX_ALTERNATIVE_PROBES) with checkFn (the same conflict-check)
// and returns those that come back with no conflicts. Not engine-ranked (labelled
// "suggested" in the UI). checkFn: (request) => Promise<ConflictDto[]>.
export async function deriveAlternatives({
  session,
  origin,
  emptyList,
  dayAxis,
  slotAxis,
  slotDurations,
  checkFn,
}) {
  const ordered = [...emptyList]
    .sort((a, b) => proximity(origin, a, dayAxis, slotAxis) - proximity(origin, b, dayAxis, slotAxis))
    .slice(0, MAX_ALTERNATIVE_PROBES);

  const results = [];
  for (const cell of ordered) {
    const built = toPlacement(session, cell, slotDurations);
    if (!built.ok) continue;
    // eslint-disable-next-line no-await-in-loop
    const conflicts = await checkFn(built.request);
    if (Array.isArray(conflicts) && conflicts.length === 0) {
      results.push({ ...cell, key: cellKey(cell.dayOfWeek, cell.slotDefinitionId) });
    }
  }
  return results;
}
