import { describe, expect, it } from 'vitest';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import {
  batchSchema,
  campusSchema,
  departmentSchema,
  programSchema,
  sectionSchema,
} from './hierarchy-schemas';

// A4-410 FR-3.2 / AC-5 — client-side Zod validation mirroring the A4-2 backend
// Create*Request rules. validateWith returns { success, data } or
// { success:false, errors:{field:message} } (shared helper from A4-340).

describe('campusSchema', () => {
  it('accepts a valid campus', () => {
    const r = validateWith(campusSchema, { name: 'Main Campus', code: 'BLR', location: 'Bangalore' });
    expect(r.success).toBe(true);
    expect(r.data.code).toBe('BLR');
  });

  it('rejects a missing name (AC-5)', () => {
    const r = validateWith(campusSchema, { name: '', code: 'BLR', location: 'Bangalore' });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects a code with illegal characters', () => {
    const r = validateWith(campusSchema, { name: 'Main', code: 'BL R!', location: 'Bangalore' });
    expect(r.success).toBe(false);
    expect(r.errors.code).toBeTruthy();
  });

  it('rejects a code over 20 characters', () => {
    const r = validateWith(campusSchema, {
      name: 'Main',
      code: 'A'.repeat(21),
      location: 'Bangalore',
    });
    expect(r.success).toBe(false);
    expect(r.errors.code).toBeTruthy();
  });
});

describe('departmentSchema', () => {
  it('accepts a valid department', () => {
    const r = validateWith(departmentSchema, { name: 'CSE', code: 'CSE', campusId: '1' });
    expect(r.success).toBe(true);
    expect(r.data.campusId).toBe(1);
  });

  it('rejects a missing parent campus (AC-5)', () => {
    const r = validateWith(departmentSchema, { name: 'CSE', code: 'CSE', campusId: '' });
    expect(r.success).toBe(false);
    expect(r.errors.campusId).toBeTruthy();
  });
});

describe('programSchema', () => {
  it('accepts a valid program', () => {
    const r = validateWith(programSchema, {
      name: 'B.Tech CSE',
      code: 'BTCS',
      departmentId: '1',
      durationSemesters: '8',
      degreeType: 'B.TECH',
    });
    expect(r.success).toBe(true);
    expect(r.data.durationSemesters).toBe(8);
  });

  it('rejects a non-positive duration', () => {
    const r = validateWith(programSchema, {
      name: 'B.Tech CSE',
      code: 'BTCS',
      departmentId: '1',
      durationSemesters: '0',
      degreeType: 'B.TECH',
    });
    expect(r.success).toBe(false);
    expect(r.errors.durationSemesters).toBeTruthy();
  });

  it('rejects a missing degree type', () => {
    const r = validateWith(programSchema, {
      name: 'B.Tech CSE',
      code: 'BTCS',
      departmentId: '1',
      durationSemesters: '8',
      degreeType: '',
    });
    expect(r.success).toBe(false);
    expect(r.errors.degreeType).toBeTruthy();
  });
});

describe('batchSchema', () => {
  it('accepts a valid batch with optional elective basket omitted', () => {
    const r = validateWith(batchSchema, { yearIdentifier: '2024-25', strength: '120', programId: '1' });
    expect(r.success).toBe(true);
    expect(r.data.strength).toBe(120);
  });

  it('accepts a valid batch with an elective basket', () => {
    const r = validateWith(batchSchema, {
      yearIdentifier: '2024-25',
      strength: '60',
      programId: '1',
      electiveBasket: 'AI/ML,Cloud',
    });
    expect(r.success).toBe(true);
  });

  it('rejects strength below 1', () => {
    const r = validateWith(batchSchema, { yearIdentifier: '2024-25', strength: '0', programId: '1' });
    expect(r.success).toBe(false);
    expect(r.errors.strength).toBeTruthy();
  });
});

describe('sectionSchema', () => {
  it('accepts a valid section', () => {
    const r = validateWith(sectionSchema, { sectionIdentifier: 'A', subStrength: '60' });
    expect(r.success).toBe(true);
    expect(r.data.subStrength).toBe(60);
  });

  it('rejects a missing identifier and non-positive sub-strength', () => {
    const r = validateWith(sectionSchema, { sectionIdentifier: '', subStrength: '0' });
    expect(r.success).toBe(false);
    expect(r.errors.sectionIdentifier).toBeTruthy();
    expect(r.errors.subStrength).toBeTruthy();
  });
});
