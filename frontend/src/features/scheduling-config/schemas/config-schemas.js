import { z } from 'zod';

import { COMPONENT_TYPES, DAYS_OF_WEEK, SOFT_CONSTRAINT_TYPES } from '../constants/config-options';

// A4-340 section 5.4 — Zod schemas mirroring the backend (A4-390) validation rules.
// Bounds reconciled to A4-390 PD-93: slotDurationMinutes 1..600, hoursPerSession
// 0.5..8.0 (DECIMAL(3,1)), weight 0.00..99.99 (DECIMAL(4,2)).

export const derivationRuleSchema = z.object({
  componentType: z.enum(COMPONENT_TYPES, { errorMap: () => ({ message: 'Select a component type' }) }),
  slotDurationMinutes: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .int('Must be a whole number')
    .min(1, 'Must be at least 1 minute')
    .max(600, 'Must not exceed 600 minutes'),
  hoursPerSession: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .min(0.5, 'Must be at least 0.5')
    .max(8.0, 'Must not exceed 8.0'),
  description: z.string().max(200, 'Must not exceed 200 characters').optional().or(z.literal('')),
  isActive: z.boolean().optional(),
});

export const softWeightSchema = z.object({
  constraintType: z.enum(SOFT_CONSTRAINT_TYPES, {
    errorMap: () => ({ message: 'Select a constraint type' }),
  }),
  weight: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .min(0, 'Must be at least 0.00')
    .max(99.99, 'Must not exceed 99.99'),
  isActive: z.boolean().optional(),
});

export const commonSlotSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100, 'Must not exceed 100 characters'),
  dayOfWeek: z.enum(DAYS_OF_WEEK, { errorMap: () => ({ message: 'Select a day' }) }),
  slotDefinitionId: z.coerce
    .number({ invalid_type_error: 'Select a slot' })
    .int()
    .positive('Select a slot'),
  appliesToAllBatches: z.boolean().optional(),
  isActive: z.boolean().optional(),
});

// Shared validate helper matching the generation feature's convention:
// returns { success, data } or { success:false, errors: { field: message } }.
export function validateWith(schema, values) {
  const result = schema.safeParse(values);
  if (result.success) return { success: true, data: result.data };
  const errors = {};
  for (const issue of result.error.issues) {
    const field = issue.path[0];
    if (field != null && errors[field] == null) errors[field] = issue.message;
  }
  return { success: false, errors };
}
