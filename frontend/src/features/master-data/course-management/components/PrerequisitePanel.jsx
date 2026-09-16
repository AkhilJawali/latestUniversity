import PropTypes from 'prop-types';
import { useState } from 'react';

import { mapApiError } from '@/lib/api-error';
import {
  useAddPrerequisite,
  usePrerequisites,
  useRemovePrerequisite,
} from '../api/usePrerequisites';

// A4-415 §5.2 / FR-5 — manage prerequisites for the selected course. Lists current
// prerequisite course IDs, resolves their names from the loaded course list, and
// adds/removes via the sub-resource endpoints. Backend cycle/non-existent errors
// are surfaced (FR-5.3 / AC-2).
export default function PrerequisitePanel({ course, allCourses }) {
  const list = usePrerequisites(course.id);
  const add = useAddPrerequisite(course.id);
  const remove = useRemovePrerequisite(course.id);

  const [picked, setPicked] = useState('');
  const [message, setMessage] = useState(null);

  const prereqIds = list.data?.data ?? [];
  const nameOf = (id) => {
    const c = allCourses.find((x) => x.id === id);
    return c ? `${c.code} — ${c.name}` : `Course #${id}`;
  };

  // Candidate courses: everything except self and already-added prerequisites.
  const candidates = allCourses.filter(
    (c) => c.id !== course.id && !prereqIds.includes(c.id),
  );

  const onAdd = (e) => {
    e.preventDefault();
    setMessage(null);
    const id = Number(picked);
    if (!Number.isInteger(id) || id <= 0) {
      setMessage('Select a course to add as a prerequisite.');
      return;
    }
    add.mutate(id, {
      onSuccess: () => setPicked(''),
      onError: (err) => setMessage(mapApiError(err).message ?? 'Could not add prerequisite.'),
    });
  };

  return (
    <section className="card" aria-label={`Prerequisites for ${course.code}`}>
      <div className="section-head">
        <h2>Prerequisites — {course.code}</h2>
      </div>

      <form className="inline-form" onSubmit={onAdd}>
        <label htmlFor="prereq-picker">Add prerequisite</label>
        <select
          id="prereq-picker"
          value={picked}
          onChange={(e) => setPicked(e.target.value)}
        >
          <option value="">Select a course…</option>
          {candidates.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} — {c.name}
            </option>
          ))}
        </select>
        <button type="submit" className="btn btn--primary" disabled={add.isPending}>
          {add.isPending ? 'Adding…' : 'Add'}
        </button>
      </form>

      {message && (
        <p className="form-error" role="alert">
          {message}
        </p>
      )}

      {list.isLoading ? (
        <p>Loading prerequisites…</p>
      ) : prereqIds.length === 0 ? (
        <p className="table-empty">No prerequisites.</p>
      ) : (
        <ul className="chip-list">
          {prereqIds.map((id) => (
            <li key={id} className="chip">
              {nameOf(id)}
              <button
                type="button"
                className="chip-remove"
                aria-label={`Remove prerequisite ${nameOf(id)}`}
                disabled={remove.isPending}
                onClick={() => {
                  setMessage(null);
                  remove.mutate(id, {
                    onError: (err) =>
                      setMessage(mapApiError(err).message ?? 'Could not remove prerequisite.'),
                  });
                }}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

PrerequisitePanel.propTypes = {
  course: PropTypes.shape({
    id: PropTypes.number.isRequired,
    code: PropTypes.string,
    name: PropTypes.string,
  }).isRequired,
  allCourses: PropTypes.array.isRequired,
};
