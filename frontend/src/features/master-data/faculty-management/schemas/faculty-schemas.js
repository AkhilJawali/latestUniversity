import { z } from 'zod';

import { DESIGNATIONS } from '../constants/faculty-options';

// A4-420 §5.2 — Zod schemas mirroring the A4-4 Create/UpdateFacultyRequest bounds.
// The backend re-validates authoritatively; these give fast client-side feedback and
// block invalid submits (FR-2.2 / AC-5).

const idNum = z.coerce
  .number({ invalid_type_error: 'Required' })
  .int()
  .positive('Required');

// Optional load fields: blank/undefined is allowed; if present, enforce the bound.
const optionalLoad = (min, message) =>
  z
    .union([z.literal(''), z.coerce.number({ invalid_type_error: 'Enter a number' }).min(min, message)])
    .optional()
    .transform((v) => (v === '' ? undefined : v));

const baseFacultyShape = {
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  designation: z.enum(DESIGNATIONS, {
    errorMap: () => ({ message: 'Select a designation' }),
  }),
  qualification: z
    .string()
    .min(1, 'Qualification is required')
    .max(500, 'Must not exceed 500 characters'),
  homeDepartmentId: idNum,
  minWeeklyLoad: optionalLoad(0, 'Must be 0 or more'),
  maxWeeklyLoad: optionalLoad(0.1, 'Must be at least 0.1'),
};

// Cross-field: if both loads are present, min must be <= max (backend also enforces, 422).
const loadOrderRefine = (data, ctx) => {
  if (
    data.minWeeklyLoad != null &&
    data.maxWeeklyLoad != null &&
    data.minWeeklyLoad > data.maxWeeklyLoad
  ) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['maxWeeklyLoad'],
      message: 'Max weekly load must be greater than or equal to min',
    });
  }
};

// Create: full field set, including identifier + campusIds (immutable later, on edit)
// and optional competency course IDs.
export const facultyCreateSchema = z
  .object({
    ...baseFacultyShape,
    identifier: z
      .string()
      .min(1, 'Identifier is required')
      .max(50, 'Must not exceed 50 characters'),
    campusIds: z
      .array(z.coerce.number().int().positive())
      .min(1, 'Select at least one campus'),
    competencyCourseIds: z.array(z.coerce.number().int().positive()).optional(),
  })
  .superRefine(loadOrderRefine);

// Edit: A4-4 UpdateFacultyRequest has no identifier / campusIds / competencyCourseIds
// (identifier immutable; campuses + competencies managed via sub-resource panels).
export const facultyEditSchema = z.object(baseFacultyShape).superRefine(loadOrderRefine);

// FR-1.2/1.3 — build the query-param object for the faculty list from the active filters.
// Only non-empty values are included so the backend applies exactly the chosen filters.
export function buildFacultyListParams({ departmentId, designation, competencyCourseId, page, size }) {
  const params = {};
  if (departmentId) params.departmentId = Number(departmentId);
  if (designation) params.designation = designation;
  if (competencyCourseId) params.competencyCourseId = Number(competencyCourseId);
  if (page != null) params.page = page;
  if (size != null) params.size = size;
  return params;
}
