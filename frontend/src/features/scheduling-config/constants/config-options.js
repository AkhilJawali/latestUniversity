// A4-340 PD-78 — dropdown option sources for the config forms.
// componentType and softConstraintType mirror the backend enums (A4-390):
//   - componentType values are the engine's literals LECTURE/TUTORIAL/PRACTICAL
//     (reconciled from the design's provisional L/T/P after A4-390 shipped).
//   - softConstraintType mirrors com.utms.scheduling.engine.enums.SoftConstraintType (6 values).
// dayOfWeek mirrors the backend DayOfWeekEnum (Mon-Sun).

export const COMPONENT_TYPES = ['LECTURE', 'TUTORIAL', 'PRACTICAL'];

export const SOFT_CONSTRAINT_TYPES = [
  'FACULTY_TIME_PREFERENCE',
  'FACULTY_DISTRIBUTION_PREFERENCE',
  'ROOM_PROXIMITY',
  'GAP_MINIMIZATION',
  'SOFT_BLOCK_OVERRIDE',
  'DAY_PATTERN_BALANCE',
];

export const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];
