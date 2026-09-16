import PropTypes from 'prop-types';
import { useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { mapApiError } from '@/lib/api-error';
import {
  useAddFacultyCampus,
  useFacultyCampuses,
  useRemoveFacultyCampus,
} from '../api/useFacultyCampuses';

// A4-420 §5.5 / FR-5 — manage a faculty's campus associations. The current set is read
// from the OQ-1 GET endpoint ({data:[{campusId,name,code}]}). Add via a campus picker
// (POST); remove via DELETE — the backend blocks removing the last association (422),
// whose message is surfaced. Add + remove errors are both shown.
export default function CampusPanel({ faculty }) {
  const list = useFacultyCampuses(faculty.id);
  const add = useAddFacultyCampus(faculty.id);
  const remove = useRemoveFacultyCampus(faculty.id);

  const campuses = useCampuses();
  const [picked, setPicked] = useState('');
  const [message, setMessage] = useState(null);

  const current = list.data?.data ?? [];
  const currentIds = current.map((c) => c.campusId);
  const allCampuses = campuses.data?.data ?? [];
  const candidates = allCampuses.filter((c) => !currentIds.includes(c.id));

  const onAdd = (e) => {
    e.preventDefault();
    setMessage(null);
    const id = Number(picked);
    if (!Number.isInteger(id) || id <= 0) {
      setMessage('Select a campus to add.');
      return;
    }
    add.mutate(id, {
      onSuccess: () => setPicked(''),
      onError: (err) => setMessage(mapApiError(err).message ?? 'Could not add campus.'),
    });
  };

  return (
    <section className="card" aria-label={`Campus associations for ${faculty.name}`}>
      <div className="section-head">
        <h2>Campuses — {faculty.name}</h2>
      </div>

      <form className="inline-form" onSubmit={onAdd}>
        <label htmlFor="campus-picker">Add campus</label>
        <select id="campus-picker" value={picked} onChange={(e) => setPicked(e.target.value)}>
          <option value="">Select a campus…</option>
          {candidates.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
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
        <p>Loading campuses…</p>
      ) : current.length === 0 ? (
        <p className="table-empty">No campus associations.</p>
      ) : (
        <ul className="chip-list">
          {current.map((c) => (
            <li key={c.campusId} className="chip">
              {c.name}
              <button
                type="button"
                className="chip-remove"
                aria-label={`Remove campus ${c.name}`}
                disabled={remove.isPending}
                onClick={() => {
                  setMessage(null);
                  remove.mutate(c.campusId, {
                    onError: (err) =>
                      setMessage(mapApiError(err).message ?? 'Could not remove campus.'),
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

CampusPanel.propTypes = {
  faculty: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string,
  }).isRequired,
};
