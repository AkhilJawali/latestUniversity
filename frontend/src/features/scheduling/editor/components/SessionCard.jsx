import PropTypes from 'prop-types';

// A4-15 §8 — a single session rendered as a draggable card. Visual states are conveyed by
// icon + text + color (never color alone, NFR-2): pending (check in flight), conflict,
// unsaved (staged move), locked. Keyboard: role="button" + tabIndex so it can be picked up
// with Enter (FR-7). Uses native HTML5 drag — no dnd library needed for grid drops.
export default function SessionCard({
  session,
  state, // 'normal' | 'pending' | 'conflict' | 'unsaved' | 'locked'
  selected,
  onPickUp,
  onDragStart,
  onDragEnd,
}) {
  const classes = ['session-card'];
  if (state && state !== 'normal') classes.push(`session-card--${state}`);
  if (selected) classes.push('session-card--selected');

  const stateNote =
    state === 'pending'
      ? '⏳ checking…'
      : state === 'conflict'
        ? '⚠ conflict'
        : state === 'unsaved'
          ? '● unsaved'
          : state === 'locked'
            ? '🔒 locked'
            : '';

  const onKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onPickUp?.(session);
    }
  };

  return (
    <div
      className={classes.join(' ')}
      role="button"
      tabIndex={0}
      aria-pressed={selected}
      aria-label={`Session ${session.courseId ?? ''} — press Enter to pick up`}
      draggable
      onDragStart={(e) => {
        e.dataTransfer.setData('text/plain', String(session.id));
        onDragStart?.(session);
      }}
      onDragEnd={() => onDragEnd?.(session)}
      onClick={() => onPickUp?.(session)}
      onKeyDown={onKeyDown}
    >
      <div className="session-card__title">Course {session.courseId ?? '—'}</div>
      <div className="session-card__meta">Faculty {session.facultyId ?? '—'}</div>
      <div className="session-card__meta">Room {session.roomId ?? '—'}</div>
      <div className="session-card__meta">
        Batch {session.batchId ?? '—'}
        {session.sectionId != null ? ` · Sec ${session.sectionId}` : ''}
      </div>
      {stateNote && (
        <div className={`session-card__state session-card__state--${state}`}>{stateNote}</div>
      )}
    </div>
  );
}

SessionCard.propTypes = {
  session: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    courseId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    facultyId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    roomId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    batchId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    sectionId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }).isRequired,
  state: PropTypes.oneOf(['normal', 'pending', 'conflict', 'unsaved', 'locked']),
  selected: PropTypes.bool,
  onPickUp: PropTypes.func,
  onDragStart: PropTypes.func,
  onDragEnd: PropTypes.func,
};
