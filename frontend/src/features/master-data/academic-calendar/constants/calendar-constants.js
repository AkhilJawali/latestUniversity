// A4-440 §8 — enums mirrored from the A4-9 backend (no list endpoint exists; these
// match HolidayScope, ExamType, PatternType). Each item has a `value` (sent to the API)
// and a `label` (shown to the user).

export const HOLIDAY_SCOPES = [
  { value: 'CAMPUS_SPECIFIC', label: 'Campus-specific' },
  { value: 'INSTITUTION_WIDE', label: 'Institution-wide' },
];

export const EXAM_TYPES = [
  { value: 'MID_SEMESTER', label: 'Mid-semester' },
  { value: 'END_SEMESTER', label: 'End-semester' },
  { value: 'SUPPLEMENTARY', label: 'Supplementary' },
];

export const PATTERN_TYPES = [
  { value: 'FIVE_DAY', label: '5-day week' },
  { value: 'SIX_DAY', label: '6-day week' },
  { value: 'ALTERNATE_SATURDAY', label: 'Alternate Saturdays' },
  { value: 'CUSTOM', label: 'Custom' },
];

// Label lookup helper used by table cells / badges.
export function labelOf(list, value) {
  return list.find((i) => i.value === value)?.label ?? value ?? '';
}
