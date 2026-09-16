import { describe, expect, it } from 'vitest';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import {
  buildFacultyListParams,
  facultyCreateSchema,
  facultyEditSchema,
} from './faculty-schemas';

// A4-420 FR-2.2 / AC-5 — client-side Zod validation mirroring the A4-4 backend
// Create/UpdateFacultyRequest bounds. validateWith returns { success, data } or
// { success:false, errors:{field:message} } (shared helper from A4-340).

describe('facultyCreateSchema', () => {
  const valid = {
    name: 'Dr. Rao',
    identifier: 'FAC-001',
    designation: 'Professor',
    qualification: 'PhD',
    homeDepartmentId: '3',
    minWeeklyLoad: '4',
    maxWeeklyLoad: '12',
    campusIds: [1, 2],
    competencyCourseIds: [5],
  };

  it('accepts a valid faculty and coerces numbers', () => {
    const r = validateWith(facultyCreateSchema, valid);
    expect(r.success).toBe(true);
    expect(r.data.homeDepartmentId).toBe(3);
    expect(r.data.minWeeklyLoad).toBe(4);
    expect(r.data.campusIds).toEqual([1, 2]);
  });

  it('accepts a valid faculty with the optional loads omitted', () => {
    const { minWeeklyLoad, maxWeeklyLoad, ...rest } = valid;
    const r = validateWith(facultyCreateSchema, { ...rest, minWeeklyLoad: '', maxWeeklyLoad: '' });
    expect(r.success).toBe(true);
    expect(r.data.minWeeklyLoad).toBeUndefined();
    expect(r.data.maxWeeklyLoad).toBeUndefined();
  });

  it('rejects a missing name (AC-5)', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, name: '' });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects a name over 200 characters', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, name: 'A'.repeat(201) });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects a missing identifier', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, identifier: '' });
    expect(r.success).toBe(false);
    expect(r.errors.identifier).toBeTruthy();
  });

  it('rejects an identifier over 50 characters', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, identifier: 'A'.repeat(51) });
    expect(r.success).toBe(false);
    expect(r.errors.identifier).toBeTruthy();
  });

  it('rejects an invalid designation', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, designation: 'Chancellor' });
    expect(r.success).toBe(false);
    expect(r.errors.designation).toBeTruthy();
  });

  it('rejects a missing qualification', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, qualification: '' });
    expect(r.success).toBe(false);
    expect(r.errors.qualification).toBeTruthy();
  });

  it('rejects a missing home department', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, homeDepartmentId: '' });
    expect(r.success).toBe(false);
    expect(r.errors.homeDepartmentId).toBeTruthy();
  });

  it('rejects an empty campus list', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, campusIds: [] });
    expect(r.success).toBe(false);
    expect(r.errors.campusIds).toBeTruthy();
  });

  it('rejects a max load below 0.1', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, maxWeeklyLoad: '0' });
    expect(r.success).toBe(false);
    expect(r.errors.maxWeeklyLoad).toBeTruthy();
  });

  it('rejects min load greater than max load', () => {
    const r = validateWith(facultyCreateSchema, { ...valid, minWeeklyLoad: '15', maxWeeklyLoad: '10' });
    expect(r.success).toBe(false);
    expect(r.errors.maxWeeklyLoad).toBeTruthy();
  });
});

describe('facultyEditSchema', () => {
  const valid = {
    name: 'Dr. Rao',
    designation: 'Associate Professor',
    qualification: 'PhD',
    homeDepartmentId: '3',
    minWeeklyLoad: '4',
    maxWeeklyLoad: '12',
  };

  it('accepts a valid edit payload (no identifier / campuses / competencies)', () => {
    const r = validateWith(facultyEditSchema, valid);
    expect(r.success).toBe(true);
    expect(r.data.identifier).toBeUndefined();
    expect(r.data.campusIds).toBeUndefined();
    expect(r.data.competencyCourseIds).toBeUndefined();
  });

  it('rejects an empty name', () => {
    const r = validateWith(facultyEditSchema, { ...valid, name: '' });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects min load greater than max load', () => {
    const r = validateWith(facultyEditSchema, { ...valid, minWeeklyLoad: '20', maxWeeklyLoad: '5' });
    expect(r.success).toBe(false);
    expect(r.errors.maxWeeklyLoad).toBeTruthy();
  });
});

describe('buildFacultyListParams', () => {
  it('omits empty filters', () => {
    expect(buildFacultyListParams({ departmentId: '', designation: '', competencyCourseId: '' })).toEqual(
      {},
    );
  });

  it('includes only the set filters, coercing ids to numbers', () => {
    const p = buildFacultyListParams({ departmentId: '3', designation: 'Professor', competencyCourseId: '5' });
    expect(p).toEqual({ departmentId: 3, designation: 'Professor', competencyCourseId: 5 });
  });

  it('passes page and size through when provided', () => {
    const p = buildFacultyListParams({ page: 0, size: 20 });
    expect(p).toEqual({ page: 0, size: 20 });
  });
});
