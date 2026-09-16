// A4-420 §5.2 / FR-7 — designation options. The A4-4 backend validates designation
// against a fixed server-side allowlist (BusinessRuleViolationException / 422 on a bad
// value) with no lookup endpoint (OQ-4), so the frontend mirrors those exact 7 strings.
export const DESIGNATIONS = [
  'Professor',
  'Associate Professor',
  'Assistant Professor',
  'Lecturer',
  'Senior Lecturer',
  'Lab Instructor',
  'Visiting Faculty',
];
