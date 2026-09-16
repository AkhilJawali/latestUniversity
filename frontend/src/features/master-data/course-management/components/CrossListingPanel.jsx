import PropTypes from 'prop-types';
import { useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useDepartments } from '@/features/master-data/campus-hierarchy/api/useDepartments';
import { mapApiError } from '@/lib/api-error';
import { useAddCrossListing } from '../api/useCrossListings';

// A4-415 §5.3 / FR-6 — add a cross-listing for the selected course. The target is a
// departmentId, chosen via a campus -> department two-step (PD-3), reusing the A4-410
// campus/department hooks. Success invalidates the course list so the isCrossListed
// flag refreshes (FR-6.2 / AC-5).
//
// NOTE: A4-3 exposes no "list cross-listings" endpoint (only add/remove by
// departmentId) and CourseDto carries only a boolean isCrossListed (not the set of
// department IDs). So this panel can add a cross-listing and reflect the boolean
// status, but cannot render a removable list of specific departments. Removal by
// department is available in the backend and hook (useRemoveCrossListing) but is not
// surfaced here for lack of a list to remove from — flagged for the code review /
// a follow-up if a list endpoint is added.
export default function CrossListingPanel({ course }) {
  const campuses = useCampuses();
  const [campusId, setCampusId] = useState('');
  const departments = useDepartments(campusId ? Number(campusId) : null);
  const [departmentId, setDepartmentId] = useState('');
  const [message, setMessage] = useState(null);

  const add = useAddCrossListing(course.id);

  const campusRows = campuses.data?.data ?? [];
  const deptRows = departments.data?.data ?? [];

  const onAdd = (e) => {
    e.preventDefault();
    setMessage(null);
    const deptId = Number(departmentId);
    if (!Number.isInteger(deptId) || deptId <= 0) {
      setMessage('Select a department to cross-list to.');
      return;
    }
    add.mutate(deptId, {
      onSuccess: () => {
        setDepartmentId('');
        setMessage('Cross-listing added.');
      },
      onError: (err) => setMessage(mapApiError(err).message ?? 'Could not add cross-listing.'),
    });
  };

  return (
    <section className="card" aria-label={`Cross-listings for ${course.code}`}>
      <div className="section-head">
        <h2>Cross-listings — {course.code}</h2>
      </div>

      <p className="hint">
        Current status: {course.isCrossListed ? 'cross-listed' : 'not cross-listed'}.
      </p>

      <form className="inline-form" onSubmit={onAdd}>
        <label htmlFor="xl-campus">Campus</label>
        <select
          id="xl-campus"
          value={campusId}
          onChange={(e) => {
            setCampusId(e.target.value);
            setDepartmentId('');
            setMessage(null);
          }}
        >
          <option value="">Select campus…</option>
          {campusRows.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>

        <label htmlFor="xl-dept">Department</label>
        <select
          id="xl-dept"
          value={departmentId}
          onChange={(e) => setDepartmentId(e.target.value)}
          disabled={!campusId || departments.isLoading}
        >
          <option value="">Select department…</option>
          {deptRows.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </select>

        <button type="submit" className="btn btn--primary" disabled={add.isPending}>
          {add.isPending ? 'Adding…' : 'Cross-list'}
        </button>
      </form>

      {message && (
        <p className="form-message" role="status">
          {message}
        </p>
      )}
    </section>
  );
}

CrossListingPanel.propTypes = {
  course: PropTypes.shape({
    id: PropTypes.number.isRequired,
    code: PropTypes.string,
    isCrossListed: PropTypes.bool,
  }).isRequired,
};
