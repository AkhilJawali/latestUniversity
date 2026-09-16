import { z } from 'zod';

import { EXAM_TYPES, HOLIDAY_SCOPES, PATTERN_TYPES } from '../constants/calendar-constants';

// A4-440 §7 — Zod schemas mirroring the A4-9 request bounds (§8 of the requirement).
// All validation is advisory (PD-A440-7); the backend re-validates and remains the
// authority. Dates are ISO YYYY-MM-DD strings from native <input type="date">.

const scopeValues = HOLIDAY_SCOPES.map((s) => s.value);
const examTypeValues = EXAM_TYPES.map((t) => t.value);
const patternTypeValues = PATTERN_TYPES.map((p) => p.value);

// PD-A440-8 — ISO string compare is safe for YYYY-MM-DD; end == start is allowed
// (matches the backend validateDateRange, which only rejects end < start).
function endNotBeforeStart(v, ctx) {
  if (v.startDate && v.endDate && v.endDate < v.startDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['endDate'],
      message: 'End date must be on or after start date',
    });
  }
}

// Advisory within-semester check for exam windows / orientation (KD-A440-1). Not applied
// to holidays — the backend does not bind holidays to the semester.
function withinSemester(v, ctx, semesterStartDate, semesterEndDate) {
  if (semesterStartDate && v.startDate && v.startDate < semesterStartDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['startDate'],
      message: 'Must be within the semester',
    });
  }
  if (semesterEndDate && v.endDate && v.endDate > semesterEndDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['endDate'],
      message: 'Must be within the semester',
    });
  }
}

export const calendarCreateSchema = z
  .object({
    academicYear: z
      .string()
      .trim()
      .min(1, 'Academic year is required')
      .max(20, 'Must not exceed 20 characters'),
    semesterIdentifier: z
      .string()
      .trim()
      .min(1, 'Semester identifier is required')
      .max(30, 'Must not exceed 30 characters'),
    semesterStartDate: z.string().min(1, 'Start date is required'),
    semesterEndDate: z.string().min(1, 'End date is required'),
  })
  .superRefine((v, ctx) => {
    if (v.semesterStartDate && v.semesterEndDate && v.semesterEndDate < v.semesterStartDate) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['semesterEndDate'],
        message: 'End date must be on or after start date',
      });
    }
  });

export const holidaySchema = z
  .object({
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
    description: z
      .string()
      .trim()
      .min(1, 'Description is required')
      .max(200, 'Must not exceed 200 characters'),
    scope: z.enum(scopeValues, { errorMap: () => ({ message: 'Select a scope' }) }),
  })
  .superRefine(endNotBeforeStart);

// Schema factories so the modal can pass the selected calendar's semester bounds for the
// advisory within-semester check (KD-A440-1).
export function makeExamWindowSchema({ semesterStartDate, semesterEndDate } = {}) {
  return z
    .object({
      startDate: z.string().min(1, 'Start date is required'),
      endDate: z.string().min(1, 'End date is required'),
      examType: z.enum(examTypeValues, { errorMap: () => ({ message: 'Select an exam type' }) }),
      description: z
        .string()
        .trim()
        .max(200, 'Must not exceed 200 characters')
        .optional()
        .or(z.literal('')),
    })
    .superRefine((v, ctx) => {
      endNotBeforeStart(v, ctx);
      withinSemester(v, ctx, semesterStartDate, semesterEndDate);
    });
}

export function makeOrientationSchema({ semesterStartDate, semesterEndDate } = {}) {
  return z
    .object({
      startDate: z.string().min(1, 'Start date is required'),
      endDate: z.string().min(1, 'End date is required'),
      description: z
        .string()
        .trim()
        .max(200, 'Must not exceed 200 characters')
        .optional()
        .or(z.literal('')),
    })
    .superRefine((v, ctx) => {
      endNotBeforeStart(v, ctx);
      withinSemester(v, ctx, semesterStartDate, semesterEndDate);
    });
}

// FR-7.3 — conditional required: workingSaturdays iff ALTERNATE_SATURDAY,
// customDefinition iff CUSTOM.
export const patternSchema = z
  .object({
    patternType: z.enum(patternTypeValues, {
      errorMap: () => ({ message: 'Select a pattern type' }),
    }),
    workingSaturdays: z
      .string()
      .trim()
      .max(100, 'Must not exceed 100 characters')
      .optional()
      .or(z.literal('')),
    customDefinition: z.string().trim().optional().or(z.literal('')),
  })
  .superRefine((v, ctx) => {
    if (v.patternType === 'ALTERNATE_SATURDAY' && !v.workingSaturdays) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['workingSaturdays'],
        message: 'Required for Alternate Saturdays (e.g. 1,3)',
      });
    }
    if (v.patternType === 'CUSTOM' && !v.customDefinition) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['customDefinition'],
        message: 'Required for a custom pattern',
      });
    }
  });
