import PropTypes from 'prop-types';

import { useUnplaced } from '@/features/scheduling/generation/api/useGeneration';

// A4-345 FR-6 — unplaced sessions for a partial (TIMED_OUT) draft. Unlike the
// placed-sessions endpoint, this DTO includes names, so we show them.
export default function UnplacedList({ draftId }) {
  const { data, isLoading, isError } = useUnplaced(draftId);

  if (isError) {
    return (
      <section className="card" aria-label="Unplaced sessions">
        <h2>Unplaced Sessions</h2>
        <p className="form-error" role="alert">Could not load unplaced sessions. Please try again.</p>
      </section>
    );
  }

  if (isLoading) {
    return (
      <section className="card" aria-label="Unplaced sessions">
        <h2>Unplaced Sessions</h2>
        <p className="muted">Loading…</p>
      </section>
    );
  }

  const items = data ?? [];

  return (
    <section className="card unplaced-list" aria-label="Unplaced sessions">
      <h2>Unplaced Sessions</h2>

      {items.length === 0 ? (
        <div className="empty-state">
          <p>All sessions were placed.</p>
        </div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Course</th>
              <th scope="col">Faculty</th>
              <th scope="col">Batch</th>
              <th scope="col">Type</th>
              <th scope="col">Duration (min)</th>
              <th scope="col">Reason</th>
            </tr>
          </thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.id}>
                <td>
                  {u.courseCode ? `${u.courseCode} — ` : ''}
                  {u.courseName ?? '—'}
                </td>
                <td>{u.facultyName ?? '—'}</td>
                <td>{u.batchName ?? '—'}</td>
                <td>{u.sessionType}</td>
                <td>{u.requiredDurationMinutes ?? '—'}</td>
                <td>{u.reason}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

UnplacedList.propTypes = {
  draftId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
};
