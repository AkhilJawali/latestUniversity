import { z } from 'zod';

import { COURSE_TYPES } from '../constants/course-options';

// A4-415 §5.5 — Zod schemas mirroring the A4-3 Create/UpdateCourseRequest bounds.
// The backend re-validates authoritatively; these give fast client-side feedback
// and block invalid submits (FR-2.2 / AC-6).

const CODE_REGEX = /^[A-Za-z0-9_-]+$/;

const ltp = z.coerce
  .number({ invalid_type_error: 'Enter a number' })
  .int('Must be a whole number')
  .min(0, 'Must be 0 or more');

// Create: full field set including code + departmentId (immutable later, on edit).
export const courseCreateSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  code: z
    .string()
    .min(1, 'Code is required')
    .max(20, 'Must not exceed 20 characters')
    .regex(CODE_REGEX, 'Code must be alphanumeric (hyphens and underscores allowed)'),
  departmentId: z.coerce
    .number({ invalid_type_error: 'Department is required' })
    .int()
    .positive('Department is required'),
  lectureHours: ltp,
  tutorialHours: ltp,
  practicalHours: ltp,
  credits: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .min(0.1, 'Credits must be at least 0.1'),
  courseType: z.enum(COURSE_TYPES, { errorMap: () => ({ message: 'Select a course type' }) }),
});

// Edit: A4-3 UpdateCourseRequest has neither code nor departmentId (both immutable).
export const courseEditSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  lectureHours: ltp,
  tutorialHours: ltp,
  practicalHours: ltp,
  credits: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .min(0.1, 'Credits must be at least 0.1'),
  courseType: z.enum(COURSE_TYPES, { errorMap: () => ({ message: 'Select a course type' }) }),
});

// FR-1.2 — client-side filter helper: by course type and a name/code search.
export function filterCourses(rows, { type, search }) {
  const q = (search ?? '').trim().toLowerCase();
  return rows.filter((r) => {
    const typeOk = !type || r.courseType === type;
    const searchOk =
      !q ||
      (r.name ?? '').toLowerCase().includes(q) ||
      (r.code ?? '').toLowerCase().includes(q);
    return typeOk && searchOk;
  });
}
