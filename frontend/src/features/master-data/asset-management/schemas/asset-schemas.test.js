import { describe, expect, it } from 'vitest';

import { assetCreateSchema, assetEditSchema, windowSchema } from './asset-schemas';
import { validateWith } from '@/features/scheduling-config/schemas/config-schemas';

// A4-435 §5.2 — Zod schema validation tests mirroring A4-7 Create/UpdateAssetRequest bounds.
// validateWith returns { success, data } or { success: false, errors: { field: message } }.

describe('windowSchema', () => {
  it('accepts a valid availability window', () => {
    const r = validateWith(windowSchema, {
      dayOfWeek: 'MONDAY',
      startTime: '09:00',
      endTime: '10:00',
    });
    expect(r.success).toBe(true);
    expect(r.data.dayOfWeek).toBe('MONDAY');
    expect(r.data.startTime).toBe('09:00');
    expect(r.data.endTime).toBe('10:00');
  });

  it('rejects an invalid day of week', () => {
    const r = validateWith(windowSchema, {
      dayOfWeek: 'FUNDAY',
      startTime: '09:00',
      endTime: '10:00',
    });
    expect(r.success).toBe(false);
    expect(r.errors.dayOfWeek).toBeTruthy();
  });

  it('rejects invalid time format', () => {
    const r = validateWith(windowSchema, {
      dayOfWeek: 'TUESDAY',
      startTime: '9:00',
      endTime: '25:00',
    });
    expect(r.success).toBe(false);
    expect(r.errors.startTime).toBeTruthy();
    expect(r.errors.endTime).toBeTruthy();
  });

  it('rejects when end time is not after start time', () => {
    const r = validateWith(windowSchema, {
      dayOfWeek: 'WEDNESDAY',
      startTime: '14:00',
      endTime: '14:00',
    });
    expect(r.success).toBe(false);
    expect(r.errors.endTime).toBe('End time must be after start time');
  });

  it('rejects when end time is before start time', () => {
    const r = validateWith(windowSchema, {
      dayOfWeek: 'THURSDAY',
      startTime: '15:00',
      endTime: '10:00',
    });
    expect(r.success).toBe(false);
    expect(r.errors.endTime).toBe('End time must be after start time');
  });
});

describe('assetCreateSchema', () => {
  it('accepts a valid create payload', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Projector Set A',
      identifier: 'PROJ-A-001',
      assetType: 'PROJECTOR_SET',
      owningDepartmentId: '1',
      campusId: '2',
      availabilityWindows: [],
    });
    expect(r.success).toBe(true);
    expect(r.data.name).toBe('Projector Set A');
    expect(r.data.identifier).toBe('PROJ-A-001');
    expect(r.data.owningDepartmentId).toBe(1);
    expect(r.data.campusId).toBe(2);
  });

  it('accepts a create payload with availability windows', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Sports Kit',
      identifier: 'SPORTS-001',
      assetType: 'SPORTS_FACILITY',
      owningDepartmentId: '3',
      campusId: '1',
      availabilityWindows: [
        { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00' },
        { dayOfWeek: 'WEDNESDAY', startTime: '10:00', endTime: '12:00' },
      ],
    });
    expect(r.success).toBe(true);
    expect(r.data.availabilityWindows).toHaveLength(2);
  });

  it('rejects missing required fields', () => {
    const r = validateWith(assetCreateSchema, {
      name: '',
      identifier: '',
      assetType: '',
      owningDepartmentId: '',
      campusId: '',
    });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBe('Name is required');
    expect(r.errors.identifier).toBe('Identifier is required');
    expect(r.errors.assetType).toBe('Asset type is required');
    expect(r.errors.owningDepartmentId).toBe('Department is required');
    expect(r.errors.campusId).toBe('Campus is required');
  });

  it('rejects name exceeding 200 characters', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'A'.repeat(201),
      identifier: 'TEST-001',
      assetType: 'TYPE',
      owningDepartmentId: '1',
      campusId: '1',
    });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBe('Must not exceed 200 characters');
  });

  it('rejects identifier with invalid characters', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Test Asset',
      identifier: 'test 001!', // spaces and special chars not allowed
      assetType: 'TYPE',
      owningDepartmentId: '1',
      campusId: '1',
    });
    expect(r.success).toBe(false);
    expect(r.errors.identifier).toBe('Identifier must be alphanumeric (hyphens and underscores allowed)');
  });

  it('accepts identifier with hyphens and underscores', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Test Asset',
      identifier: 'TEST_001-A',
      assetType: 'TYPE',
      owningDepartmentId: '1',
      campusId: '1',
    });
    expect(r.success).toBe(true);
    expect(r.data.identifier).toBe('TEST_001-A');
  });

  it('rejects asset type exceeding 50 characters', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Test Asset',
      identifier: 'TEST-001',
      assetType: 'A'.repeat(51),
      owningDepartmentId: '1',
      campusId: '1',
    });
    expect(r.success).toBe(false);
    expect(r.errors.assetType).toBe('Must not exceed 50 characters');
  });

  it('rejects identifier exceeding 50 characters', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Test Asset',
      identifier: 'A'.repeat(51),
      assetType: 'TYPE',
      owningDepartmentId: '1',
      campusId: '1',
    });
    expect(r.success).toBe(false);
    expect(r.errors.identifier).toBe('Must not exceed 50 characters');
  });

  it('rejects invalid availability windows in create payload', () => {
    const r = validateWith(assetCreateSchema, {
      name: 'Test Asset',
      identifier: 'TEST-001',
      assetType: 'TYPE',
      owningDepartmentId: '1',
      campusId: '1',
      availabilityWindows: [
        { dayOfWeek: 'INVALID_DAY', startTime: '09:00', endTime: '10:00' },
      ],
    });
    expect(r.success).toBe(false);
  });
});

describe('assetEditSchema', () => {
  it('accepts a valid edit payload', () => {
    const r = validateWith(assetEditSchema, {
      name: 'Updated Projector Set',
      assetType: 'PROJECTOR_SET_V2',
      availabilityWindows: [],
    });
    expect(r.success).toBe(true);
    expect(r.data.name).toBe('Updated Projector Set');
    expect(r.data.assetType).toBe('PROJECTOR_SET_V2');
  });

  it('accepts an edit payload with availability windows', () => {
    const r = validateWith(assetEditSchema, {
      name: 'Updated Sports Kit',
      assetType: 'SPORTS_FACILITY',
      availabilityWindows: [
        { dayOfWeek: 'FRIDAY', startTime: '08:00', endTime: '16:00' },
      ],
    });
    expect(r.success).toBe(true);
    expect(r.data.availabilityWindows).toHaveLength(1);
  });

  it('rejects missing name in edit payload', () => {
    const r = validateWith(assetEditSchema, {
      name: '',
      assetType: 'TYPE',
    });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBe('Name is required');
  });

  it('rejects missing asset type in edit payload', () => {
    const r = validateWith(assetEditSchema, {
      name: 'Test Asset',
      assetType: '',
    });
    expect(r.success).toBe(false);
    expect(r.errors.assetType).toBe('Asset type is required');
  });

  it('rejects name exceeding 200 characters in edit payload', () => {
    const r = validateWith(assetEditSchema, {
      name: 'A'.repeat(201),
      assetType: 'TYPE',
    });
    expect(r.success).toBe(false);
    expect(r.errors.name).toBe('Must not exceed 200 characters');
  });

  it('rejects asset type exceeding 50 characters in edit payload', () => {
    const r = validateWith(assetEditSchema, {
      name: 'Test Asset',
      assetType: 'A'.repeat(51),
    });
    expect(r.success).toBe(false);
    expect(r.errors.assetType).toBe('Must not exceed 50 characters');
  });

  it('accepts optional availability windows as empty array', () => {
    const r = validateWith(assetEditSchema, {
      name: 'Test Asset',
      assetType: 'TYPE',
      availabilityWindows: [],
    });
    expect(r.success).toBe(true);
  });

  it('allows omitting availability windows entirely', () => {
    const r = validateWith(assetEditSchema, {
      name: 'Test Asset',
      assetType: 'TYPE',
    });
    expect(r.success).toBe(true);
  });
});
