/**
 * A4-430 §5.2 — Zod schemas for room create/edit forms.
 */
import { z } from 'zod';
import { ROOM_TYPES } from '../constants/room-options';

/**
 * Room type values for enum validation.
 */
const roomTypeValues = ROOM_TYPES.map((t) => t.value);

/**
 * Create room schema.
 * All fields required except optional ones.
 */
export const roomCreateSchema = z.object({
  name: z
    .string()
    .min(1, 'Name is required')
    .max(200, 'Name must be 200 characters or less'),
  code: z
    .string()
    .min(1, 'Code is required')
    .max(20, 'Code must be 20 characters or less')
    .regex(/^[A-Za-z0-9_-]+$/, 'Code must contain only letters, numbers, underscore, or hyphen'),
  campusId: z.coerce.number().int().positive('Campus is required'),
  capacity: z.coerce.number().int().min(1, 'Capacity must be at least 1'),
  roomType: z.enum(roomTypeValues, {
    errorMap: () => ({ message: 'Select a valid room type' }),
  }),
  equipmentTags: z.array(z.string()).optional(),
  building: z.string().max(100, 'Building must be 100 characters or less').optional(),
  floor: z.string().max(20, 'Floor must be 20 characters or less').optional(),
});

/**
 * Edit room schema.
 * Code and campusId are immutable (not included).
 */
export const roomEditSchema = z.object({
  name: z
    .string()
    .min(1, 'Name is required')
    .max(200, 'Name must be 200 characters or less'),
  capacity: z.coerce.number().int().min(1, 'Capacity must be at least 1'),
  roomType: z.enum(roomTypeValues, {
    errorMap: () => ({ message: 'Select a valid room type' }),
  }),
  equipmentTags: z.array(z.string()).optional(),
  building: z.string().max(100, 'Building must be 100 characters or less').optional(),
  floor: z.string().max(20, 'Floor must be 20 characters or less').optional(),
});

/**
 * Parse comma/newline-separated tags into array.
 * @param {string} input - Comma or newline separated tags
 * @returns {string[]} Trimmed tag array
 */
export function parseTags(input) {
  if (!input || typeof input !== 'string') return [];
  return input
    .split(/[,\n]+/)
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0);
}

/**
 * Format tag array into comma-separated string.
 * @param {string[]} tags - Tag array
 * @returns {string} Comma-separated string
 */
export function formatTags(tags) {
  if (!tags || !Array.isArray(tags)) return '';
  return tags.join(', ');
}
