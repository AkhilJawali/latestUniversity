import PropTypes from 'prop-types';

import { useCalendarsByCampus } from '@/features/master-data/academic-calendar/api/useAcademicCalendars';
import { describeGenerationError } from '@/features/scheduling/generation/lib/generationErrors';

// Why a generation could not start. Precondition failures are listed one per line; when
// the calendar check fails, the academic year / semester values that do exist on the
// campus are shown, since a typo there is the most common cause.

function CalendarHint({ campusId }) {
  const calendars = useCalendarsByCampus(campusId);
  const available = (calendars.data?.data ?? []).map(
    (c) => `${c.academicYear} / ${c.semesterIdentifier}`,
  );
  if (available.length === 0) return null;
  return (
    <span className="gen-error-hint">
      {' '}
      Calendars on this campus (academic year / semester): {available.join(', ')}.
    </span>
  );
}

CalendarHint.propTypes = {
  campusId: PropTypes.number.isRequired,
};

export default function GenerationStartError({ error, campusId }) {
  const { message, reasons } = describeGenerationError(error);

  return (
    <div className="form-error gen-start-error" role="alert">
      <p>{message}</p>
      {reasons.length > 0 && (
        <ul className="gen-error-reasons">
          {reasons.map((r) => (
            <li key={r.check ?? r.text}>
              {r.text}
              {r.check === 'ACADEMIC_CALENDAR' && campusId && <CalendarHint campusId={Number(campusId)} />}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

GenerationStartError.propTypes = {
  error: PropTypes.object.isRequired,
  campusId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
};
