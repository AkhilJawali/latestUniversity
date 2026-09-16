import { describe, expect, it } from 'vitest';

import {
  commonSlotSchema,
  derivationRuleSchema,
  softWeightSchema,
  validateWith,
} from './config-schemas';

// A4-340 FR-6 / AC-5 — client-side Zod validation. validateWith returns
// { success, data } or { success:false, errors:{field:message} }.
describe('derivationRuleSchema', () => {
  it('accepts a valid rule', () => {
    const r = validateWith(derivationRuleSchema, {
      componentType: 'LECTURE',
      slotDurationMinutes: '60',
      hoursPerSession: '1.0',
      description: 'Lecture',
    });
    expect(r.success).toBe(true);
    expect(r.data.componentType).toBe('LECTURE');
    expect(r.data.slotDurationMinutes).toBe(60);
  });

  it('rejects an invalid component type', () => {
    const r = validateWith(derivationRuleSchema, {
      componentType: 'L',
      slotDurationMinutes: '60',
      hoursPerSession: '1.0',
    });
    expect(r.success).toBe(false);
    expect(r.errors.componentType).toBeTruthy();
  });

  it('rejects out-of-range slot duration and hours', () => {
    const r = validateWith(derivationRuleSchema, {
      componentType: 'TUTORIAL',
      slotDurationMinutes: '0',
      hoursPerSession: '99',
    });
    expect(r.success).toBe(false);
    expect(r.errors.slotDurationMinutes).toBeTruthy();
    expect(r.errors.hoursPerSession).toBeTruthy();
  });
});

describe('softWeightSchema', () => {
  it('accepts a valid weight', () => {
    const r = validateWith(softWeightSchema, { constraintType: 'ROOM_PROXIMITY', weight: '2.5' });
    expect(r.success).toBe(true);
    expect(r.data.weight).toBe(2.5);
  });

  it('rejects an unknown constraint type', () => {
    const r = validateWith(softWeightSchema, { constraintType: 'FOO', weight: '1' });
    expect(r.success).toBe(false);
    expect(r.errors.constraintType).toBeTruthy();
  });

  it('rejects weight above 99.99', () => {
    const r = validateWith(softWeightSchema, { constraintType: 'GAP_MINIMIZATION', weight: '100' });
    expect(r.success).toBe(false);
    expect(r.errors.weight).toBeTruthy();
  });
});

describe('commonSlotSchema', () => {
  it('accepts a valid common slot', () => {
    const r = validateWith(commonSlotSchema, {
      name: 'CCC',
      dayOfWeek: 'MONDAY',
      slotDefinitionId: '3',
      appliesToAllBatches: true,
    });
    expect(r.success).toBe(true);
    expect(r.data.slotDefinitionId).toBe(3);
  });

  it('rejects a missing name and a bad day', () => {
    const r = validateWith(commonSlotSchema, { name: '', dayOfWeek: 'FUNDAY', slotDefinitionId: '3' });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
    expect(r.errors.dayOfWeek).toBeTruthy();
  });
});
