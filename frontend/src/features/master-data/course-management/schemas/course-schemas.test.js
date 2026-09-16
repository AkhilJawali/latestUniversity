import { describe, expect, it } from 'vitest';

import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';
import { courseCreateSchema, courseEditSchema, filterCourses } from './course-schemas';

// A4-415 FR-2.2 / AC-6 — client-side Zod validation mirroring the A4-3 backend
// Create/UpdateCourseRequest rules. validateWith returns { success, data } or
// { success:false, errors:{field:message} } (shared helper from A4-340).

describe('courseCreateSchema', () => {
  const valid = {
    name: 'Data Structures',
    code: 'CS201',
    departmentId: '3',
    lectureHours: '3',
    tutorialHours: '1',
    practicalHours: '2',
    credits: '4',
    courseType: 'CORE',
  };

  it('accepts a valid course and coerces numbers', () => {
    const r = validateWith(courseCreateSchema, valid);
    expect(r.success).toBe(true);
    expect(r.data.departmentId).toBe(3);
    expect(r.data.lectureHours).toBe(3);
    expect(r.data.credits).toBe(4);
  });

  it('rejects a missing name (AC-6)', () => {
    const r = validateWith(courseCreateSchema, { ...valid, name: '' });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects a name over 200 characters', () => {
    const r = validateWith(courseCreateSchema, { ...valid, name: 'A'.repeat(201) });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects a code with illegal characters', () => {
    const r = validateWith(courseCreateSchema, { ...valid, code: 'CS 201!' });
    expect(r.success).toBe(false);
    expect(r.errors.code).toBeTruthy();
  });

  it('rejects a code over 20 characters', () => {
    const r = validateWith(courseCreateSchema, { ...valid, code: 'A'.repeat(21) });
    expect(r.success).toBe(false);
    expect(r.errors.code).toBeTruthy();
  });

  it('rejects a missing department (AC-6)', () => {
    const r = validateWith(courseCreateSchema, { ...valid, departmentId: '' });
    expect(r.success).toBe(false);
    expect(r.errors.departmentId).toBeTruthy();
  });

  it('rejects negative L-T-P hours', () => {
    const r = validateWith(courseCreateSchema, { ...valid, lectureHours: '-1' });
    expect(r.success).toBe(false);
    expect(r.errors.lectureHours).toBeTruthy();
  });

  it('rejects credits below 0.1', () => {
    const r = validateWith(courseCreateSchema, { ...valid, credits: '0' });
    expect(r.success).toBe(false);
    expect(r.errors.credits).toBeTruthy();
  });

  it('rejects an invalid course type', () => {
    const r = validateWith(courseCreateSchema, { ...valid, courseType: 'MANDATORY' });
    expect(r.success).toBe(false);
    expect(r.errors.courseType).toBeTruthy();
  });
});

describe('courseEditSchema', () => {
  const valid = {
    name: 'Data Structures',
    lectureHours: '3',
    tutorialHours: '1',
    practicalHours: '2',
    credits: '4',
    courseType: 'CORE',
  };

  it('accepts a valid edit payload (no code / departmentId)', () => {
    const r = validateWith(courseEditSchema, valid);
    expect(r.success).toBe(true);
    expect(r.data.credits).toBe(4);
    // code + departmentId are immutable on the backend, so not part of the schema.
    expect(r.data.code).toBeUndefined();
    expect(r.data.departmentId).toBeUndefined();
  });

  it('rejects an empty name', () => {
    const r = validateWith(courseEditSchema, { ...valid, name: '' });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBeTruthy();
  });

  it('rejects credits below 0.1', () => {
    const r = validateWith(courseEditSchema, { ...valid, credits: '0.05' });
    expect(r.success).toBe(false);
    expect(r.errors.credits).toBeTruthy();
  });
});

describe('filterCourses', () => {
  const rows = [
    { id: 1, name: 'Data Structures', code: 'CS201', courseType: 'CORE' },
    { id: 2, name: 'Machine Learning', code: 'CS402', courseType: 'ELECTIVE' },
    { id: 3, name: 'Yoga', code: 'AUD100', courseType: 'AUDIT' },
  ];

  it('returns all rows when no filter is applied', () => {
    expect(filterCourses(rows, { type: '', search: '' })).toHaveLength(3);
  });

  it('filters by course type', () => {
    const out = filterCourses(rows, { type: 'ELECTIVE', search: '' });
    expect(out).toHaveLength(1);
    expect(out[0].code).toBe('CS402');
  });

  it('filters by a case-insensitive name search', () => {
    const out = filterCourses(rows, { type: '', search: 'machine' });
    expect(out).toHaveLength(1);
    expect(out[0].id).toBe(2);
  });

  it('filters by a code search', () => {
    const out = filterCourses(rows, { type: '', search: 'CS2' });
    expect(out).toHaveLength(1);
    expect(out[0].code).toBe('CS201');
  });

  it('applies type and search together', () => {
    const out = filterCourses(rows, { type: 'CORE', search: 'data' });
    expect(out).toHaveLength(1);
    expect(out[0].id).toBe(1);
  });

  it('tolerates undefined search', () => {
    expect(filterCourses(rows, { type: '' })).toHaveLength(3);
  });
});
