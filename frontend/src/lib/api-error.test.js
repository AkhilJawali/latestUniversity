import { describe, expect, it } from 'vitest';

import { mapApiError } from '@/lib/api-error';

// A4-340 section 5.6 — maps the backend error envelope to field errors / message,
// never leaking internals.
describe('mapApiError', () => {
  it('maps 400 details to per-field errors', () => {
    const err = {
      response: {
        data: {
          status: 400,
          message: 'Validation failed',
          details: [
            { field: 'weight', message: 'must not exceed 99.99', rejectedValue: 100 },
            { field: 'constraintType', message: 'invalid' },
          ],
        },
      },
    };
    const result = mapApiError(err);
    expect(result.fields).toEqual({
      weight: 'must not exceed 99.99',
      constraintType: 'invalid',
    });
  });

  it('falls back to the envelope message for non-field errors (e.g. 409)', () => {
    const err = { response: { data: { status: 409, message: 'Already exists' } } };
    expect(mapApiError(err)).toEqual({ message: 'Already exists' });
  });

  it('uses a safe generic message when nothing is provided', () => {
    expect(mapApiError({}).message).toBe('Something went wrong. Please try again.');
  });
});
