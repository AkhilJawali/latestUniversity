import { z } from 'zod';

// A4-410 §5.5 — Zod schemas mirroring the backend (A4-2) validation rules.
// Bounds taken from the A4-2 Create*Request DTOs (e.g., CreateCampusRequest,
// CreateBatchRequest). The backend re-validates authoritatively; these give
// fast client-side feedback and block invalid submits (FR-3.2 / AC-5).

const CODE_REGEX = /^[A-Za-z0-9_-]+$/;

export const campusSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  code: z
    .string()
    .min(1, 'Code is required')
    .max(20, 'Must not exceed 20 characters')
    .regex(CODE_REGEX, 'Code must be alphanumeric (hyphens and underscores allowed)'),
  location: z.string().min(1, 'Location is required').max(500, 'Must not exceed 500 characters'),
});

export const departmentSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  code: z
    .string()
    .min(1, 'Code is required')
    .max(20, 'Must not exceed 20 characters')
    .regex(CODE_REGEX, 'Code must be alphanumeric (hyphens and underscores allowed)'),
  campusId: z.coerce.number({ invalid_type_error: 'Campus is required' }).int().positive('Campus is required'),
});

export const programSchema = z.object({
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
  durationSemesters: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .int('Must be a whole number')
    .min(1, 'Must be at least 1'),
  degreeType: z.string().min(1, 'Degree type is required').max(50, 'Must not exceed 50 characters'),
});

export const batchSchema = z.object({
  yearIdentifier: z
    .string()
    .min(1, 'Year identifier is required')
    .max(20, 'Must not exceed 20 characters'),
  strength: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .int('Must be a whole number')
    .min(1, 'Strength must be at least 1'),
  programId: z.coerce
    .number({ invalid_type_error: 'Program is required' })
    .int()
    .positive('Program is required'),
  electiveBasket: z
    .string()
    .max(200, 'Must not exceed 200 characters')
    .optional()
    .or(z.literal('')),
});

export const sectionSchema = z.object({
  sectionIdentifier: z
    .string()
    .min(1, 'Section identifier is required')
    .max(20, 'Must not exceed 20 characters'),
  subStrength: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .int('Must be a whole number')
    .min(1, 'Sub-strength must be at least 1'),
});
