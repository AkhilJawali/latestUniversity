import { z } from 'zod';

import { DAYS_OF_WEEK, SESSION_DISTRIBUTION, TIME_OF_DAY } from '../constants/availability-options';

// A4-425 §5.2 — Zod schemas mirroring the A4-5 Create/UpdateAvailabilityWindowRequest
// and SetPreferenceRequest bounds. The backend re-validates authoritatively; these give
// fast client-side feedback and block invalid submits (FR-3.1 / AC-3).

// <input type="time"> produces zero-padded "HH:mm"; the backend LocalTime accepts it.
const TIME_REGEX = /^([01]\d|2[0-3]):[0-5]\d$/;

const timeField = z
  .string()
  .min(1, 'Required')
  .regex(TIME_REGEX, 'Enter a valid time (HH:MM)');

// FR-2 / FR-3 — availability (unavailability) window.
export const availabilityWindowSchema = z
  .object({
    dayOfWeek: z.enum(DAYS_OF_WEEK, { errorMap: () => ({ message: 'Select a day' }) }),
    startTime: timeField,
    endTime: timeField,
    reasonCode: z
      .string()
      .min(1, 'Reason code is required')
      .max(50, 'Must not exceed 50 characters'),
    reasonNote: z
      .string()
      .max(500, 'Must not exceed 500 characters')
      .optional()
      .or(z.literal('')),
  })
  .superRefine((data, ctx) => {
    // End must be strictly after start. Zero-padded HH:mm compares correctly as strings.
    if (data.startTime && data.endTime && TIME_REGEX.test(data.startTime) && TIME_REGEX.test(data.endTime)) {
      if (data.endTime <= data.startTime) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['endTime'],
          message: 'End time must be after start time',
        });
      }
    }
  });

// FR-4 — soft preferences.
export const preferenceSchema = z.object({
  preferredTimeOfDay: z.enum(TIME_OF_DAY, {
    errorMap: () => ({ message: 'Select a preferred time of day' }),
  }),
  sessionDistribution: z.enum(SESSION_DISTRIBUTION, {
    errorMap: () => ({ message: 'Select a session distribution' }),
  }),
});
