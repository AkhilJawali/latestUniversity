// Turns a failed "start generation" request into a plain-language message.
// For the 422 precondition failure the backend lists each failed check as
// { check, message }; its messages mention internal IDs, so known checks are shown
// with friendly wording instead (the backend message is only a fallback).

export const PRECONDITION_LABELS = {
  ACADEMIC_CALENDAR:
    'No academic calendar on this campus matches this academic year and semester. They must match exactly (for example ODD is not the same as Odd).',
  TIME_SLOT_GRID: 'This campus has no time-slot grid.',
  WORKING_DAY_PATTERN: 'This campus has no working-day pattern.',
  DERIVATION_RULES: 'This campus has no session derivation rules (Lecture, Tutorial, Practical).',
  FACULTY_ASSIGNMENT:
    'No course of this department has a teacher marked as able to teach it (faculty competency).',
  ROOMS: 'This campus has no rooms.',
};

const GENERIC = 'Could not start generation. Please check your inputs and try again.';

/** Returns { message, reasons: [{ check, text }] } for display. */
export function describeGenerationError(error) {
  const status = error?.response?.status;
  const body = error?.response?.data;

  if (status === 503) {
    return { message: 'The scheduling engine is busy right now. Please try again in a moment.', reasons: [] };
  }
  if (status === 409) {
    return {
      message: 'A generation for this department and semester is already running. Wait for it to finish.',
      reasons: [],
    };
  }
  if (status === 404) {
    return { message: 'The selected department was not found. Refresh the page and try again.', reasons: [] };
  }
  if (status === 422 && Array.isArray(body?.details) && body.details.length > 0) {
    return {
      message: 'Generation cannot start until these are fixed:',
      reasons: body.details.map((d) => ({
        check: d.check,
        text: PRECONDITION_LABELS[d.check] ?? d.message ?? d.check,
      })),
    };
  }
  if (status === 422 && body?.message) {
    return { message: body.message, reasons: [] };
  }
  return { message: GENERIC, reasons: [] };
}
