import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

import { mapApiError } from '@/lib/api-error';
import {
  useAddCompetencies,
  useFacultyCompetencies,
  useRemoveCompetency,
} from '../api/useFacultyCompetencies';

// A4-420 §5.5 / FR-4 — manage subject competencies for the selected faculty. The backend
// returns competency course IDs only (OQ-5), so names are resolved from the loaded
// courses list. Add is a bulk POST (single course here); remove is per-course DELETE.
// Errors from both add and remove are surfaced (symmetry lesson from A4-415 review).
export default function CompetencyPanel({ faculty, allCourses }) {
  const list = useFacultyCompetencies(faculty.id);
  const add = useAddCompetencies(faculty.id);
  const remove = useRemoveCompetency(faculty.id);

  const [picked, setPicked] = useState('');
  const [message, setMessage] = useState(null);

  const courseIds = list.data?.data ?? [];
  const courseById = useMemo(() => {
    const m = new Map();
    allCourses.forEach((c) => m.set(c.id, c));
    return m;
  }, [allCourses]);

  const nameOf = (id) => {
    const c = courseById.get(id);
    return c ? `${c.code} — ${c.name}` : `Course #${id}`;
  };

  const candidates = useMemo(
    () => allCourses.filter((c) => !courseIds.includes(c.id)),
    [allCourses, courseIds],
  );

  const onAdd = (e) => {
    e.preventDefault();
    setMessage(null);
    const id = Number(picked);
    if (!Number.isInteger(id) || id <= 0) {
      setMessage('Select a course to add.');
      return;
    }
    add.mutate([id], {
      onSuccess: () => setPicked(''),
      onError: (err) => setMessage(mapApiError(err).message ?? 'Could not add competency.'),
    });
  };

  return (
    <section className="card" aria-label={`Competencies for ${faculty.name}`}>
      <div className="section-head">
        <h2>Competencies — {faculty.name}</h2>
      </div>

      <form className="inline-form" onSubmit={onAdd}>
        <label htmlFor="comp-picker">Add competency</label>
        <select id="comp-picker" value={picked} onChange={(e) => setPicked(e.target.value)}>
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
        <p>Loading competencies…</p>
      ) : courseIds.length === 0 ? (
        <p className="table-empty">No competencies.</p>
      ) : (
        <ul className="chip-list">
          {courseIds.map((id) => (
            <li key={id} className="chip">
              {nameOf(id)}
              <button
                type="button"
                className="chip-remove"
                aria-label={`Remove competency ${nameOf(id)}`}
                disabled={remove.isPending}
                onClick={() => {
                  setMessage(null);
                  remove.mutate(id, {
                    onError: (err) =>
                      setMessage(mapApiError(err).message ?? 'Could not remove competency.'),
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

CompetencyPanel.propTypes = {
  faculty: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string,
  }).isRequired,
  allCourses: PropTypes.array.isRequired,
};
