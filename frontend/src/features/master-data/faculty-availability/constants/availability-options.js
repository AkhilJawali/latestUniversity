// A4-425 §5.2 — option lists mirroring the A4-5 backend allowlists (validated
// server-side; no lookup endpoints, so the frontend mirrors them — A425-OQ-3/OQ-4).

// Availability window day-of-week values (backend VALID_DAYS, uppercase full names).
export const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

// Preference: preferred time-of-day (backend VALID_TIME_OF_DAY — no EVENING, A425-OQ-4).
export const TIME_OF_DAY = ['MORNING', 'AFTERNOON', 'NO_PREFERENCE'];

// Preference: session distribution (backend VALID_DISTRIBUTION).
export const SESSION_DISTRIBUTION = ['CONSECUTIVE', 'SPREAD', 'NO_PREFERENCE'];
