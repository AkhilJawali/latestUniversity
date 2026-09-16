import { describe, expect, it } from 'vitest';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { availabilityWindowSchema, preferenceSchema } from './availability-schemas';

// A4-425 FR-3.1 / AC-3 — client-side Zod validation mirroring the A4-5 backend
// Create/UpdateAvailabilityWindowRequest and SetPreferenceRequest bounds. validateWith
// returns { success, data } or { success:false, errors:{field:message} }.

describe('availabilityWindowSchema', () => {
  const valid = {
    dayOfWeek: 'TUESDAY',
    startTime: '14:00',
    endTime: '16:00',
    reasonCode: 'RESEARCH',
    reasonNote: 'Weekly research block',
  };

  it('accepts a valid window', () => {
    const r = validateWith(availabilityWindowSchema, valid);
    expect(r.success).toBe(true);
    expect(r.data.dayOfWeek).toBe('TUESDAY');
  });

  it('accepts a valid window with the note omitted', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, reasonNote: '' });
    expect(r.success).toBe(true);
  });

  it('rejects an invalid day of week', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, dayOfWeek: 'Funday' });
    expect(r.success).toBe(false);
    expect(r.errors.dayOfWeek).toBeTruthy();
  });

  it('rejects a malformed start time', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, startTime: '25:00' });
    expect(r.success).toBe(false);
    expect(r.errors.startTime).toBeTruthy();
  });

  it('rejects a missing start time', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, startTime: '' });
    expect(r.success).toBe(false);
    expect(r.errors.startTime).toBeTruthy();
  });

  it('rejects end time before start time', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, startTime: '16:00', endTime: '14:00' });
    expect(r.success).toBe(false);
    expect(r.errors.endTime).toBeTruthy();
  });

  it('rejects end time equal to start time (must be strictly after)', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, startTime: '14:00', endTime: '14:00' });
    expect(r.success).toBe(false);
    expect(r.errors.endTime).toBeTruthy();
  });

  it('rejects a missing reason code', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, reasonCode: '' });
    expect(r.success).toBe(false);
    expect(r.errors.reasonCode).toBeTruthy();
  });

  it('rejects a reason code over 50 characters', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, reasonCode: 'A'.repeat(51) });
    expect(r.success).toBe(false);
    expect(r.errors.reasonCode).toBeTruthy();
  });

  it('rejects a note over 500 characters', () => {
    const r = validateWith(availabilityWindowSchema, { ...valid, reasonNote: 'A'.repeat(501) });
    expect(r.success).toBe(false);
    expect(r.errors.reasonNote).toBeTruthy();
  });
});

describe('preferenceSchema', () => {
  it('accepts valid preferences', () => {
    const r = validateWith(preferenceSchema, {
      preferredTimeOfDay: 'MORNING',
      sessionDistribution: 'SPREAD',
    });
    expect(r.success).toBe(true);
  });

  it('accepts NO_PREFERENCE for both', () => {
    const r = validateWith(preferenceSchema, {
      preferredTimeOfDay: 'NO_PREFERENCE',
      sessionDistribution: 'NO_PREFERENCE',
    });
    expect(r.success).toBe(true);
  });

  it('rejects an unsupported time-of-day (e.g. EVENING)', () => {
    const r = validateWith(preferenceSchema, {
      preferredTimeOfDay: 'EVENING',
      sessionDistribution: 'SPREAD',
    });
    expect(r.success).toBe(false);
    expect(r.errors.preferredTimeOfDay).toBeTruthy();
  });

  it('rejects an invalid session distribution', () => {
    const r = validateWith(preferenceSchema, {
      preferredTimeOfDay: 'MORNING',
      sessionDistribution: 'RANDOM',
    });
    expect(r.success).toBe(false);
    expect(r.errors.sessionDistribution).toBeTruthy();
  });
});
