import PropTypes from 'prop-types';

import StatusBadge from '@/features/scheduling/approval/components/StatusBadge';
import {
  draftActions,
  draftTitle,
  formatDateTime,
} from '@/features/scheduling/approval/lib/approval-flow';

// A department's timetable drafts (newest first). Drafts are named by version, semester
// and year — never by id. Row buttons open the approval panel for that draft.
export default function DraftList({ drafts, selectedDraftId, onSelect, onAction }) {
  return (
    <div className="table-scroll">
      <table className="data-table draft-list">
        <thead>
          <tr>
            <th scope="col">Draft</th>
            <th scope="col">Status</th>
            <th scope="col">Sessions placed</th>
            <th scope="col">Generated</th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          {drafts.map((d) => {
            const selected = d.id === selectedDraftId;
            const actions = draftActions(d.status);
            return (
              <tr key={d.id} className={selected ? 'draft-row--selected' : undefined}>
                <td>{draftTitle(d)}</td>
                <td>
                  <StatusBadge status={d.status} />
                </td>
                <td>
                  {d.totalSessionsPlaced ?? 0} / {d.totalSessionsRequired ?? 0}
                </td>
                <td>{formatDateTime(d.generatedAt)}</td>
                <td className="draft-actions">
                  <button
                    type="button"
                    className="btn"
                    aria-pressed={selected}
                    onClick={() => onSelect(d.id)}
                  >
                    {selected ? 'Viewing' : 'View'}
                  </button>
                  {actions.includes('submit') && (
                    <button type="button" className="btn btn--primary" onClick={() => onAction(d.id, 'submit')}>
                      Submit for approval
                    </button>
                  )}
                  {actions.includes('publish') && (
                    <button type="button" className="btn btn--primary" onClick={() => onAction(d.id, 'publish')}>
                      Publish
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

DraftList.propTypes = {
  drafts: PropTypes.array.isRequired,
  selectedDraftId: PropTypes.number,
  onSelect: PropTypes.func.isRequired,
  onAction: PropTypes.func.isRequired,
};
