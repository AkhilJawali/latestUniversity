import PropTypes from 'prop-types';

import { DRAFT_STATUS_LABELS } from '@/features/scheduling/approval/lib/approval-flow';

// Draft status as a coloured label (text is always shown, colour is only a cue).
export default function StatusBadge({ status }) {
  const modifier = String(status ?? '').toLowerCase().replace(/_/g, '-');
  return (
    <span className={`status-badge status-badge--${modifier}`}>
      {DRAFT_STATUS_LABELS[status] ?? status}
    </span>
  );
}

StatusBadge.propTypes = {
  status: PropTypes.string,
};
