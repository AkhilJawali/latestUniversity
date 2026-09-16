import { describe, expect, it } from 'vitest';

import { validateGenerationForm } from './generationSchema';

// A4-345 AC#1 — the coordinator triggers generation for a department/semester.
// The generate form is validated client-side (allowlist, Zod) before a request
// is submitted, so invalid input never reaches the backend.
describe('validateGenerationForm', () => {
  it('accepts a valid request and coerces the department id to a number', () => {
    const result = validateGenerationForm({
      departmentId: '3',
      semester: 'ODD',
      academicYear: '2024-25',
    });

    expect(result.success).toBe(true);
    expect(result.data.departmentId).toBe(3);
    expect(result.data.semester).toBe('ODD');
    expect(result.data.academicYear).toBe('2024-25');
  });

  it('drops an empty seed and keeps a provided numeric seed', () => {
    const withoutSeed = validateGenerationForm({
      departmentId: 1,
      semester: 'ODD',
      academicYear: '2024-25',
      seed: '',
    });
    expect(withoutSeed.success).toBe(true);
    expect(withoutSeed.data).not.toHaveProperty('seed');

    const withSeed = validateGenerationForm({
      departmentId: 1,
      semester: 'ODD',
      academicYear: '2024-25',
      seed: '42',
    });
    expect(withSeed.success).toBe(true);
    expect(withSeed.data.seed).toBe(42);
  });

  it('rejects a missing department with a field-level error', () => {
    const result = validateGenerationForm({
      departmentId: '',
      semester: 'ODD',
      academicYear: '2024-25',
    });
    expect(result.success).toBe(false);
    expect(result.errors.departmentId).toBe('Please select a department');
  });

  it('sends only backend fields (the campus picker value is not submitted)', () => {
    const result = validateGenerationForm({
      campusId: '1',
      departmentId: '12',
      semester: 'ODD',
      academicYear: '2024-25',
    });
    expect(result.success).toBe(true);
    expect(result.data).toEqual({ departmentId: 12, semester: 'ODD', academicYear: '2024-25' });
  });

  it('rejects a non-positive department id', () => {
    const result = validateGenerationForm({
      departmentId: '0',
      semester: 'ODD',
      academicYear: '2024-25',
    });
    expect(result.success).toBe(false);
    expect(result.errors.departmentId).toBeTruthy();
  });

  it('rejects blank semester and academic year', () => {
    const result = validateGenerationForm({
      departmentId: 1,
      semester: '   ',
      academicYear: '',
    });
    expect(result.success).toBe(false);
    expect(result.errors.semester).toBeTruthy();
    expect(result.errors.academicYear).toBeTruthy();
  });
});
