import { describe, expect, it } from 'vitest';

import {
  countOutsideGrid,
  dayLabel,
  formatTime,
  groupSessionsByCell,
  slotAppliesTo,
  sortSlots,
  uniqueIds,
  workingDays,
} from './timetable-grid';

describe('workingDays', () => {
  it('gives Monday–Friday for a five-day pattern or no pattern', () => {
    const monToFri = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
    expect(workingDays('FIVE_DAY')).toEqual(monToFri);
    expect(workingDays(null)).toEqual(monToFri);
    expect(workingDays('CUSTOM')).toEqual(monToFri);
  });

  it('gives Monday–Saturday for six-day and alternate-Saturday patterns', () => {
    expect(workingDays('SIX_DAY')).toHaveLength(6);
    expect(workingDays('ALTERNATE_SATURDAY').at(-1)).toBe('SATURDAY');
  });

  it('adds a day that already holds a session so nothing is hidden', () => {
    expect(workingDays('FIVE_DAY', [{ dayOfWeek: 'SATURDAY' }])).toContain('SATURDAY');
  });
});

describe('sortSlots', () => {
  it('orders slots by start time, then end time', () => {
    const sorted = sortSlots([
      { id: 3, startTime: '14:00:00', endTime: '16:00:00' },
      { id: 1, startTime: '09:00:00', endTime: '10:00:00' },
      { id: 2, startTime: '09:00:00', endTime: '09:30:00' },
    ]);
    expect(sorted.map((s) => s.id)).toEqual([2, 1, 3]);
  });
});

describe('groupSessionsByCell', () => {
  it('groups sessions by day and slot, keeping every session of a shared box', () => {
    const cells = groupSessionsByCell([
      { id: 1, dayOfWeek: 'MONDAY', slotDefinitionId: 7 },
      { id: 2, dayOfWeek: 'MONDAY', slotDefinitionId: 7 },
      { id: 3, dayOfWeek: 'TUESDAY', slotDefinitionId: 7 },
    ]);
    expect(cells.get('MONDAY|7')).toHaveLength(2);
    expect(cells.get('TUESDAY|7')).toHaveLength(1);
  });
});

describe('small helpers', () => {
  it('formats times and day names for display', () => {
    expect(formatTime('09:00:00')).toBe('09:00');
    expect(dayLabel('WEDNESDAY')).toBe('Wednesday');
  });

  it('applies all-day slots to every day and day-specific slots to their day only', () => {
    expect(slotAppliesTo({ applicableDay: null }, 'MONDAY')).toBe(true);
    expect(slotAppliesTo({ applicableDay: 'FRIDAY' }, 'MONDAY')).toBe(false);
  });

  it('counts sessions whose slot is not in the grid', () => {
    const sessions = [{ slotDefinitionId: 1 }, { slotDefinitionId: 99 }];
    expect(countOutsideGrid(sessions, [{ id: 1 }])).toBe(1);
  });

  it('collects distinct ids in ascending order', () => {
    const sessions = [{ roomId: 5 }, { roomId: 2 }, { roomId: 5 }, { roomId: null }];
    expect(uniqueIds(sessions, 'roomId')).toEqual([2, 5]);
  });
});
