import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import WeeklyTimetable from './WeeklyTimetable';

// Weekly timetable view of placed sessions: days as rows, campus slots as columns,
// and names/codes (never raw IDs) inside each box.
describe('WeeklyTimetable', () => {
  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
  const slots = [
    { id: 11, startTime: '09:00:00', endTime: '10:00:00', slotType: 'TEACHING', applicableDay: null },
    { id: 12, startTime: '10:00:00', endTime: '10:15:00', slotType: 'BREAK', applicableDay: null },
    { id: 13, startTime: '14:00:00', endTime: '16:00:00', slotType: 'TEACHING', applicableDay: 'FRIDAY' },
  ];
  const lookups = {
    courses: new Map([[9001, { code: 'CS101', name: 'Programming in C' }]]),
    faculty: new Map([[9101, { name: 'Dr. Anil Rao' }]]),
    rooms: new Map([[9201, { code: 'LAB-201', name: 'Computer Lab' }]]),
  };
  const session = (overrides = {}) => ({
    id: 1,
    courseId: 9001,
    facultyId: 9101,
    roomId: 9201,
    batchId: 9301,
    dayOfWeek: 'MONDAY',
    slotDefinitionId: 11,
    sessionType: 'LECTURE',
    isLocked: false,
    ...overrides,
  });
  const rowFor = (dayName) => screen.getByRole('rowheader', { name: dayName }).closest('tr');

  it('draws one row per working day and one column per time slot', () => {
    render(<WeeklyTimetable days={days} slots={slots} sessions={[]} lookups={lookups} />);
    expect(screen.getAllByRole('rowheader')).toHaveLength(5);
    // "Day" corner + 3 slots
    expect(screen.getAllByRole('columnheader')).toHaveLength(4);
    expect(screen.getByRole('columnheader', { name: '09:00–10:00' })).toBeInTheDocument();
  });

  it('shows course code, teacher name and room code instead of IDs', () => {
    render(<WeeklyTimetable days={days} slots={slots} sessions={[session()]} lookups={lookups} />);
    const monday = within(rowFor('Monday'));
    expect(monday.getByText('CS101')).toBeInTheDocument();
    expect(monday.getByText('Dr. Anil Rao')).toBeInTheDocument();
    expect(monday.getByText('LAB-201')).toBeInTheDocument();
    expect(monday.getByText('Lecture')).toBeInTheDocument();
    ['9001', '9101', '9201', '9301', '11'].forEach((id) => {
      expect(screen.queryByText(id)).not.toBeInTheDocument();
    });
  });

  it('labels break slots and leaves a day-specific slot unused on other days', () => {
    render(<WeeklyTimetable days={days} slots={slots} sessions={[]} lookups={lookups} />);
    expect(within(rowFor('Monday')).getByText('Break')).toBeInTheDocument();
    expect(screen.getByText('Friday only')).toBeInTheDocument();
    expect(within(rowFor('Monday')).getByTitle('This slot is not used on this day')).toBeInTheDocument();
  });

  it('flags a box holding two sessions as a clash and shows both', () => {
    const sessions = [session(), session({ id: 2 })];
    render(<WeeklyTimetable days={days} slots={slots} sessions={sessions} lookups={lookups} />);
    expect(screen.getByText('Clash: 2 sessions')).toBeInTheDocument();
    expect(screen.getAllByText('CS101')).toHaveLength(2);
  });

  it('shows a placeholder while names load and "Unknown" when a name cannot be found', () => {
    const missing = [session({ courseId: 5555, facultyId: 5556, roomId: 5557 })];
    const { rerender } = render(
      <WeeklyTimetable days={days} slots={slots} sessions={missing} lookups={lookups} namesLoading />,
    );
    expect(screen.getAllByText('…')).toHaveLength(3);

    rerender(<WeeklyTimetable days={days} slots={slots} sessions={missing} lookups={lookups} />);
    expect(screen.getByText('Unknown course')).toBeInTheDocument();
    expect(screen.getByText('Unknown teacher')).toBeInTheDocument();
    expect(screen.getByText('Unknown room')).toBeInTheDocument();
  });
});
