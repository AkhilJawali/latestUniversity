import { z } from 'zod';

// A4-345 FR-1 / Section 8 — client-side, allowlist-based validation for the
// generate form. Mirrors the backend GenerateRequest contract: departmentId,
// semester, academicYear required; seed optional numeric. The departmentId comes
// from the department picker (chosen by code), so any invalid value means
// "nothing selected". UI-only fields (campusId) are stripped by z.object. The
// backend re-validates; this is a fast-feedback guard, not the source of truth.
const SELECT_DEPARTMENT = 'Please select a department';

export const generationSchema = z.object({
  departmentId: z.coerce
    .number({ invalid_type_error: SELECT_DEPARTMENT })
    .int(SELECT_DEPARTMENT)
    .positive(SELECT_DEPARTMENT),
  semester: z.string().trim().min(1, 'Semester is required'),
  academicYear: z.string().trim().min(1, 'Academic year is required'),
  // Optional seed. An empty / blank input means "not provided" and is
  // normalised to undefined BEFORE coercion — otherwise z.coerce.number()
  // would turn '' into 0 and submit an unintended fixed seed.
  seed: z.preprocess(
    (v) => (typeof v === 'string' && v.trim() === '' ? undefined : v),
    z.coerce.number().int('Seed must be a whole number').optional(),
  ),
});

/**
 * Validate raw form values. Returns { success, data } or { success:false, errors }
 * where errors is a { field: message } map for inline display.
 */
export function validateGenerationForm(values) {
  const result = generationSchema.safeParse(values);
  if (result.success) {
    const data = { ...result.data };
    if (data.seed === '' || data.seed == null) delete data.seed;
    return { success: true, data };
  }
  const errors = {};
  for (const issue of result.error.issues) {
    const key = issue.path[0];
    if (key != null && !errors[key]) errors[key] = issue.message;
  }
  return { success: false, errors };
}
