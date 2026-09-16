// A4-15 §5/KD-A15-1 — the conflict-type contract mirrored from A4-16's ConflictType enum.
// The UI renders ONLY what the engine returns; deferred types are labelled so that when
// the engine begins raising them, no UI change is needed. The UI never synthesizes a
// conflict of any type on its own.

export const CONFLICT_TYPE_LABELS = {
  FACULTY_DOUBLE_BOOKING: 'Faculty double-booking',
  ROOM_DOUBLE_BOOKING: 'Room double-booking',
  BATCH_CLASH: 'Batch clash',
  ROOM_CAPACITY: 'Room capacity exceeded',
  FACULTY_DAILY_HOURS: 'Faculty daily hours exceeded',
  FACULTY_WEEKLY_HOURS: 'Faculty weekly hours exceeded',
  FACULTY_CONSECUTIVE_HOURS: 'Faculty consecutive hours exceeded',
  ROOM_HARD_BLOCK: 'Room hard-block violation',
  FACULTY_HARD_BLOCK: 'Faculty hard-block violation',
  TRAVEL_TIME: 'Travel-time violation',
  PREREQUISITE_SEQUENCE: 'Prerequisite sequence conflict',
};

// Actively detected by A4-16 today (design §3).
export const DETECTED_TYPES = [
  'FACULTY_DOUBLE_BOOKING',
  'ROOM_DOUBLE_BOOKING',
  'BATCH_CLASH',
  'ROOM_CAPACITY',
  'FACULTY_DAILY_HOURS',
  'FACULTY_WEEKLY_HOURS',
  'FACULTY_CONSECUTIVE_HOURS',
];

// Defined in the contract but detection deferred (design §3). Listed for completeness only.
export const DEFERRED_TYPES = [
  'ROOM_HARD_BLOCK',
  'FACULTY_HARD_BLOCK',
  'TRAVEL_TIME',
  'PREREQUISITE_SEQUENCE',
];

export function conflictLabel(type) {
  return CONFLICT_TYPE_LABELS[type] ?? type ?? 'Conflict';
}

// Standard working-day axis fallback (PD-A15-7). The engine/sessions use these names.
export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

// Duration used for the placement check when the slot's own duration cannot be resolved
// from the campus grid (OQ-5 / PD-A15-6). The backend re-validates authoritatively.
export const DEFAULT_SLOT_DURATION_MINUTES = 60;

// Cap on how many empty cells the client probes when deriving alternatives (PD-A15-4),
// keeping the interaction responsive.
export const MAX_ALTERNATIVE_PROBES = 8;
