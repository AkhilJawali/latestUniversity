import { z } from 'zod';

import { DAYS_OF_WEEK } from '../constants/asset-options';

// A4-435 §5.2 — Zod schemas mirroring the A4-7 Create/UpdateAssetRequest and
// CreateAvailabilityWindowRequest bounds. The backend re-validates the subset it
// enforces; these give fast client-side feedback and block invalid submits (FR-3 / AC-4).

const CODE_REGEX = /^[A-Za-z0-9_-]+$/;
const TIME_REGEX = /^([01]\d|2[0-3]):[0-5]\d$/;

// FR-4 / FR-3.6 — one availability window row. End must be strictly after start
// (A435-OQ-6: enforced client-side; the backend does not check this for asset windows).
export const windowSchema = z
  .object({
    dayOfWeek: z.enum(DAYS_OF_WEEK, { errorMap: () => ({ message: 'Select a day' }) }),
    startTime: z.string().regex(TIME_REGEX, 'Enter a valid time (HH:MM)'),
    endTime: z.string().regex(TIME_REGEX, 'Enter a valid time (HH:MM)'),
  })
  .superRefine((data, ctx) => {
    if (
      TIME_REGEX.test(data.startTime) &&
      TIME_REGEX.test(data.endTime) &&
      data.endTime <= data.startTime
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['endTime'],
        message: 'End time must be after start time',
      });
    }
  });

const baseAssetShape = {
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  assetType: z
    .string()
    .min(1, 'Asset type is required')
    .max(50, 'Must not exceed 50 characters'),
  availabilityWindows: z.array(windowSchema).optional(),
};

// Create: full field set including identifier + owningDepartmentId + campusId
// (all immutable later, on edit).
export const assetCreateSchema = z.object({
  ...baseAssetShape,
  identifier: z
    .string()
    .min(1, 'Identifier is required')
    .max(50, 'Must not exceed 50 characters')
    .regex(CODE_REGEX, 'Identifier must be alphanumeric (hyphens and underscores allowed)'),
  owningDepartmentId: z.coerce
    .number({ invalid_type_error: 'Department is required' })
    .int()
    .positive('Department is required'),
  campusId: z.coerce
    .number({ invalid_type_error: 'Campus is required' })
    .int()
    .positive('Campus is required'),
});

// Edit: A4-7 UpdateAssetRequest accepts only name / assetType / availabilityWindows
// (identifier, owningDepartment, campus are immutable).
export const assetEditSchema = z.object(baseAssetShape);
