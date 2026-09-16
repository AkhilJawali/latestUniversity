// A4-435 §5.2 / OQ-7 — availability-window day-of-week values. The A4-7 backend
// validates dayOfWeek against a regex of the 7 uppercase day names (no lookup endpoint),
// so the frontend mirrors that list. assetType is intentionally NOT enumerated here —
// per A435-OQ-1 it is a free-text field (the backend accepts any non-blank string ≤50).
export const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];
