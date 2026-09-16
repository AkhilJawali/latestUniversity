import { z } from 'zod';

import { ROOM_TYPES } from '../constants/room-options';

// A4-430 §5.2 — Zod schemas mirroring the A4-6 Create/UpdateRoomRequest bounds. The
// backend re-validates authoritatively; these give fast client-side feedback and block
// invalid submits (FR-2.2 / AC-4).

const CODE_REGEX = /^[A-Za-z0-9_-]+$/;

// Equipment tags are stored by the backend as a delimited String list. In the form they
// are entered as free text (comma/newline separated); these helpers convert both ways.
export function parseTags(input) {
  if (Array.isArray(input)) return input.map((t) => String(t).trim()).filter(Boolean);
  return String(input ?? '')
    .split(/[,\n]/)
    .map((t) => t.trim())
    .filter(Boolean);
}

export function formatTags(tags) {
  return (tags ?? []).join(', ');
}

const baseRoomShape = {
  name: z.string().min(1, 'Name is required').max(200, 'Must not exceed 200 characters'),
  capacity: z.coerce
    .number({ invalid_type_error: 'Enter a number' })
    .int('Must be a whole number')
    .min(1, 'Capacity must be at least 1'),
  roomType: z.enum(ROOM_TYPES, { errorMap: () => ({ message: 'Select a room type' }) }),
  equipmentTags: z.array(z.string()).optional(),
  building: z.string().max(100, 'Must not exceed 100 characters').optional().or(z.literal('')),
  floor: z.string().max(20, 'Must not exceed 20 characters').optional().or(z.literal('')),
};

// Create: full field set including code + campusId (both immutable later, on edit).
export const roomCreateSchema = z.object({
  ...baseRoomShape,
  code: z
    .string()
    .min(1, 'Code is required')
    .max(20, 'Must not exceed 20 characters')
    .regex(CODE_REGEX, 'Code must be alphanumeric (hyphens and underscores allowed)'),
  campusId: z.coerce
    .number({ invalid_type_error: 'Campus is required' })
    .int()
    .positive('Campus is required'),
});

// Edit: A4-6 UpdateRoomRequest has neither code nor campusId (both immutable).
export const roomEditSchema = z.object(baseRoomShape);
