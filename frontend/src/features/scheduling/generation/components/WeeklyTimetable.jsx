import PropTypes from 'prop-types';

import {
  cellKey,
  dayLabel,
  formatTime,
  groupSessionsByCell,
  SESSION_TYPE_LABELS,
  slotAppliesTo,
} from '@/features/scheduling/generation/lib/timetable-grid';

// Placed sessions drawn as a weekly timetable: one row per working day, one column per
// campus time slot (breaks and lunch included). Each box shows the course code, teacher
// and room — resolved names/codes, never raw IDs. A box holding more than one session is
// flagged as a clash rather than hiding the extra session.

const NON_TEACHING_LABELS = { BREAK: 'Break', LUNCH: 'Lunch' };

const lookupsShape = PropTypes.shape({
  courses: PropTypes.instanceOf(Map).isRequired,
  faculty: PropTypes.instanceOf(Map).isRequired,
  rooms: PropTypes.instanceOf(Map).isRequired,
});

function SessionCard({ session, lookups, namesLoading }) {
  const fallback = (what) => (namesLoading ? '…' : `Unknown ${what}`);
  const course = lookups.courses.get(session.courseId);
  const teacher = lookups.faculty.get(session.facultyId);
  const room = lookups.rooms.get(session.roomId);
  const type = session.sessionType;

  return (
    <div className={`tt-session tt-session--${String(type).toLowerCase()}`} title={course?.name}>
      <span className="tt-session__course">{course?.code ?? course?.name ?? fallback('course')}</span>
      <span>{teacher?.name ?? fallback('teacher')}</span>
      <span>{room?.code ?? room?.name ?? fallback('room')}</span>
      <span className="tt-session__meta">
        {SESSION_TYPE_LABELS[type] ?? type}
        {session.isLocked ? ' · Locked' : ''}
      </span>
    </div>
  );
}

SessionCard.propTypes = {
  session: PropTypes.object.isRequired,
  lookups: lookupsShape.isRequired,
  namesLoading: PropTypes.bool,
};

function TimetableCell({ day, slot, sessions, lookups, namesLoading }) {
  if (!slotAppliesTo(slot, day)) {
    return <td className="tt-cell tt-cell--na" title="This slot is not used on this day" />;
  }

  const nonTeaching = NON_TEACHING_LABELS[slot.slotType];
  if (nonTeaching && sessions.length === 0) {
    return <td className="tt-cell tt-cell--break">{nonTeaching}</td>;
  }

  const clash = sessions.length > 1;
  return (
    <td className={`tt-cell${clash ? ' tt-cell--clash' : ''}`}>
      {clash && <span className="tt-clash-label">Clash: {sessions.length} sessions</span>}
      {sessions.map((s) => (
        <SessionCard key={s.id} session={s} lookups={lookups} namesLoading={namesLoading} />
      ))}
    </td>
  );
}

TimetableCell.propTypes = {
  day: PropTypes.string.isRequired,
  slot: PropTypes.object.isRequired,
  sessions: PropTypes.array.isRequired,
  lookups: lookupsShape.isRequired,
  namesLoading: PropTypes.bool,
};

export default function WeeklyTimetable({ days, slots, sessions, lookups, namesLoading = false }) {
  const cells = groupSessionsByCell(sessions);

  return (
    <div className="tt-scroll">
      <table className="tt-grid">
        <caption className="tt-caption">
          Days down the side, time slots across the top. Each box shows the course code, teacher
          and room. Blue = lecture, green = tutorial, orange = practical.
        </caption>
        <thead>
          <tr>
            <th scope="col" className="tt-corner">
              Day
            </th>
            {slots.map((slot) => (
              <th
                key={slot.id}
                scope="col"
                className={`tt-slot-head${NON_TEACHING_LABELS[slot.slotType] ? ' tt-slot-head--break' : ''}`}
              >
                {formatTime(slot.startTime)}–{formatTime(slot.endTime)}
                {slot.applicableDay && (
                  <span className="tt-slot-kind">{dayLabel(slot.applicableDay)} only</span>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {days.map((day) => (
            <tr key={day}>
              <th scope="row" className="tt-day">
                {dayLabel(day)}
              </th>
              {slots.map((slot) => (
                <TimetableCell
                  key={slot.id}
                  day={day}
                  slot={slot}
                  sessions={cells.get(cellKey(day, slot.id)) ?? []}
                  lookups={lookups}
                  namesLoading={namesLoading}
                />
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

WeeklyTimetable.propTypes = {
  days: PropTypes.arrayOf(PropTypes.string).isRequired,
  slots: PropTypes.array.isRequired,
  sessions: PropTypes.array.isRequired,
  lookups: lookupsShape.isRequired,
  namesLoading: PropTypes.bool,
};
