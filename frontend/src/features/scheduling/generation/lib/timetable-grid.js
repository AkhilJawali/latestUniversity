// Pure helpers for the weekly timetable view of a draft's placed sessions:
// rows = working days, columns = the campus time slots, cells = sessions.

export const WEEK_DAYS = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

// Same mapping the scheduling engine uses to decide working days:
// SIX_DAY / ALTERNATE_SATURDAY -> Mon–Sat; FIVE_DAY / CUSTOM / no pattern -> Mon–Fri.
const SIX_DAY_PATTERNS = ['SIX_DAY', 'ALTERNATE_SATURDAY'];

export const SESSION_TYPE_LABELS = {
  LECTURE: 'Lecture',
  TUTORIAL: 'Tutorial',
  PRACTICAL: 'Practical',
};

/** "09:00:00" -> "09:00". */
export const formatTime = (t) => (t == null ? '' : String(t).slice(0, 5));

/** "MONDAY" -> "Monday". */
export const dayLabel = (day) => (day ? day.charAt(0) + day.slice(1).toLowerCase() : '');

/**
 * Rows of the timetable, in weekday order. Any day that already holds a session is
 * included even if the pattern would not list it, so no placed session is hidden.
 */
export function workingDays(patternType, sessions = []) {
  const count = SIX_DAY_PATTERNS.includes(patternType) ? 6 : 5;
  const days = new Set(WEEK_DAYS.slice(0, count));
  sessions.forEach((s) => {
    if (WEEK_DAYS.includes(s.dayOfWeek)) days.add(s.dayOfWeek);
  });
  return WEEK_DAYS.filter((d) => days.has(d));
}

/** Columns of the timetable: every slot of the grid ordered by start, then end time. */
export function sortSlots(slots = []) {
  return [...slots].sort(
    (a, b) =>
      formatTime(a.startTime).localeCompare(formatTime(b.startTime)) ||
      formatTime(a.endTime).localeCompare(formatTime(b.endTime)),
  );
}

/** A slot with no applicable day runs every day; otherwise only on its own day. */
export const slotAppliesTo = (slot, day) => slot.applicableDay == null || slot.applicableDay === day;

export const cellKey = (day, slotDefinitionId) => `${day}|${slotDefinitionId}`;

/** Map of "DAY|slotDefinitionId" -> sessions placed in that box. */
export function groupSessionsByCell(sessions = []) {
  const cells = new Map();
  sessions.forEach((s) => {
    const key = cellKey(s.dayOfWeek, s.slotDefinitionId);
    if (!cells.has(key)) cells.set(key, []);
    cells.get(key).push(s);
  });
  return cells;
}

/** Sessions whose slot is no longer part of the campus grid (cannot be drawn). */
export function countOutsideGrid(sessions = [], slots = []) {
  const slotIds = new Set(slots.map((s) => s.id));
  return sessions.filter((s) => !slotIds.has(s.slotDefinitionId)).length;
}

/** Distinct, non-null values of one id field across sessions (sorted for stable query order). */
export function uniqueIds(sessions = [], field) {
  return [...new Set(sessions.map((s) => s[field]).filter((v) => v != null))].sort((a, b) => a - b);
}
