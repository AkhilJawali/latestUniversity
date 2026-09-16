import PropTypes from 'prop-types';

import { useTimetableGridData } from '@/features/scheduling/generation/api/useTimetableGridData';
import WeeklyTimetable from '@/features/scheduling/generation/components/WeeklyTimetable';
import {
  countOutsideGrid,
  sortSlots,
  workingDays,
} from '@/features/scheduling/generation/lib/timetable-grid';

// Placed sessions of a draft as a weekly timetable (days x campus time slots).
export default function TimetableGrid({ draftId, departmentId }) {
  const { sessions, grid, patternType, lookups, isLoading, isError, namesLoading } =
    useTimetableGridData(draftId, departmentId);

  const wrap = (body, extraClass = '') => (
    <section className={`card timetable-grid ${extraClass}`.trim()} aria-label="Placed sessions">
      <div className="section-head">
        <h2>Placed Sessions</h2>
        {!isLoading && !isError && <span className="page-info">{sessions.length} sessions</span>}
      </div>
      {body}
    </section>
  );

  if (isError) {
    return wrap(
      <p className="form-error" role="alert">
        Could not load the timetable. Please try again.
      </p>,
    );
  }

  if (departmentId == null || isLoading) {
    return wrap(<p className="muted">Loading timetable…</p>);
  }

  if (sessions.length === 0) {
    return wrap(
      <div className="empty-state">
        <p>No sessions in this draft.</p>
      </div>,
    );
  }

  if (!grid) {
    return wrap(
      <p className="form-error" role="alert">
        The campus of this department has no time-slot grid, so the timetable cannot be drawn.
      </p>,
    );
  }

  const slots = sortSlots(grid.slots ?? []);
  const hidden = countOutsideGrid(sessions, slots);

  return wrap(
    <>
      {namesLoading && (
        <p className="muted" role="status">
          Loading course, teacher and room names…
        </p>
      )}
      <WeeklyTimetable
        days={workingDays(patternType, sessions)}
        slots={slots}
        sessions={sessions}
        lookups={lookups}
        namesLoading={namesLoading}
      />
      {hidden > 0 && (
        <p className="muted">
          {hidden} session(s) use a time slot that is no longer in the campus grid and are not shown.
        </p>
      )}
    </>,
  );
}

TimetableGrid.propTypes = {
  draftId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  departmentId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
};
