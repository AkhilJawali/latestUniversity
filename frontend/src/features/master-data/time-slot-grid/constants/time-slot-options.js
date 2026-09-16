// A4-445 §4/§5.2 — option lists mirrored from the A4-10 backend enums (no list endpoint
// exists). SlotType {TEACHING, BREAK, LUNCH}; DayOfWeekEnum {MONDAY..SUNDAY}. A slot's
// applicableDay is null = "All days" (an all-days slot); a non-null value is a
// day-specific override. The ALL_DAYS sentinel represents that null choice in the UI.

export const SLOT_TYPES = [
  { value: 'TEACHING', label: 'Teaching' },
  { value: 'BREAK', label: 'Break' },
  { value: 'LUNCH', label: 'Lunch' },
];

// Sentinel for "applies to all days" (sent to the backend as null applicableDay).
export const ALL_DAYS = '';

export const DAY_OPTIONS = [
  { value: ALL_DAYS, label: 'All days' },
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

// Weekday options only (no "All days") — used by the per-day preview selector.
export const WEEKDAY_OPTIONS = DAY_OPTIONS.filter((d) => d.value !== ALL_DAYS);

// Label lookup helper used by table cells / badges.
export function labelOf(list, value) {
  return list.find((i) => i.value === value)?.label ?? value ?? '';
}

// Day scope label for a slot: null/empty applicableDay => "All days", else the weekday.
export function dayScopeLabel(applicableDay) {
  if (applicableDay == null || applicableDay === ALL_DAYS) return 'All days';
  return labelOf(DAY_OPTIONS, applicableDay);
}
