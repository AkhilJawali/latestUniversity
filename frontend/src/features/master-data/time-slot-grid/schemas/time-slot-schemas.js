import { z } from 'zod';

import { ALL_DAYS, DAY_OPTIONS, SLOT_TYPES } from '../constants/time-slot-options';

// A4-445 §5.2 — Zod schemas mirroring the A4-10 request bounds (requirement §8). All
// validation is advisory; the backend re-validates (day-aware overlap, ≥1 teaching,
// start<end) authoritatively. Times are HH:mm strings from <input type="time">.

const slotTypeValues = SLOT_TYPES.map((t) => t.value);
// Day allowlist for a slot: the weekday values plus the ALL_DAYS ('') sentinel.
const dayValues = DAY_OPTIONS.map((d) => d.value);

const HHMM = /^([01]\d|2[0-3]):[0-5]\d$/;

// endTime strictly after startTime. String compare is valid for zero-padded HH:mm.
function endAfterStart(v, ctx) {
  if (v.startTime && v.endTime && HHMM.test(v.startTime) && HHMM.test(v.endTime)) {
    if (v.endTime <= v.startTime) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['endTime'],
        message: 'End time must be after start time',
      });
    }
  }
}

export const slotDefinitionSchema = z
  .object({
    startTime: z.string().min(1, 'Start time is required').regex(HHMM, 'Use HH:mm'),
    endTime: z.string().min(1, 'End time is required').regex(HHMM, 'Use HH:mm'),
    slotType: z.enum(slotTypeValues, { errorMap: () => ({ message: 'Select a slot type' }) }),
    // Optional day override: '' (ALL_DAYS) or one of the weekday values.
    applicableDay: z.enum(dayValues).optional().or(z.literal(ALL_DAYS)),
    label: z.string().trim().max(100, 'Must not exceed 100 characters').optional().or(z.literal('')),
  })
  .superRefine(endAfterStart);

export const gridCreateSchema = z
  .object({
    gridName: z
      .string()
      .trim()
      .min(1, 'Grid name is required')
      .max(200, 'Must not exceed 200 characters'),
    slots: z.array(slotDefinitionSchema).min(1, 'At least one slot is required'),
  })
  .superRefine((v, ctx) => {
    // HC-GRID-4 mirror: at least one TEACHING slot (FR-4.2).
    if (Array.isArray(v.slots) && !v.slots.some((s) => s.slotType === 'TEACHING')) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['slots'],
        message: 'At least one teaching slot is required',
      });
    }
  });

export const gridRenameSchema = z.object({
  gridName: z
    .string()
    .trim()
    .min(1, 'Grid name is required')
    .max(200, 'Must not exceed 200 characters'),
});

// Convert a validated slot form value into the A4-10 request body: the ALL_DAYS sentinel
// ('') and empty label become null (backend treats null applicableDay as an all-days slot).
export function toSlotRequest(data) {
  return {
    startTime: data.startTime,
    endTime: data.endTime,
    slotType: data.slotType,
    applicableDay: data.applicableDay ? data.applicableDay : null,
    label: data.label ? data.label : null,
  };
}
