import { describe, expect, it } from 'vitest';

import { describeGenerationError, PRECONDITION_LABELS } from './generationErrors';

const httpError = (status, data) => ({ response: { status, data } });

describe('describeGenerationError', () => {
  it('lists each failed precondition in plain words', () => {
    const result = describeGenerationError(
      httpError(422, {
        message: 'Generation preconditions not met',
        details: [
          { check: 'ROOMS', message: 'No active rooms available for campus 101' },
          { check: 'ACADEMIC_CALENDAR', message: 'No academic calendar for campus 101, year 2019, semester Odd' },
        ],
      }),
    );

    expect(result.message).toBe('Generation cannot start until these are fixed:');
    expect(result.reasons).toEqual([
      { check: 'ROOMS', text: PRECONDITION_LABELS.ROOMS },
      { check: 'ACADEMIC_CALENDAR', text: PRECONDITION_LABELS.ACADEMIC_CALENDAR },
    ]);
  });

  it('falls back to the backend message for an unknown check', () => {
    const result = describeGenerationError(
      httpError(422, { details: [{ check: 'SOMETHING_NEW', message: 'New rule failed' }] }),
    );
    expect(result.reasons[0].text).toBe('New rule failed');
  });

  it('explains busy, already-running and not-found cases', () => {
    expect(describeGenerationError(httpError(503)).message).toMatch(/busy/);
    expect(describeGenerationError(httpError(409)).message).toMatch(/already running/);
    expect(describeGenerationError(httpError(404)).message).toMatch(/not found/);
  });

  it('uses a generic message for anything else', () => {
    expect(describeGenerationError(new Error('Network Error')).message).toMatch(/Could not start generation/);
  });
});
