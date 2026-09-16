import PropTypes from 'prop-types';

import { cellKey } from '../lib/grid-model';
import SessionCard from './SessionCard';

// A4-15 §8 — the weekly grid (day columns × slot rows). Each cell is both a drop target
// (native onDragOver/onDrop) and a keyboard target: when a session is "picked up", pressing
// Enter on a focused cell drops it there (FR-7). The conflicted target cell is highlighted.
// Cards render from the prebuilt (day×slot) grid map with staged moves already applied.
export default function EditorGrid({
  dayAxis,
  slotAxis,
  grid,
  selectedSessionId,
  conflictCellKey,
  pendingCellKey,
  cardState,
  onPickUp,
  onDragStartSession,
  onDragEndSession,
  onDropOnCell,
}) {
  const handleDrop = (day, slotId) => (e) => {
    e.preventDefault();
    onDropOnCell?.({ dayOfWeek: day, slotDefinitionId: slotId });
  };
  const handleCellKeyDown = (day, slotId) => (e) => {
    if (e.key === 'Enter' && selectedSessionId != null) {
      e.preventDefault();
      onDropOnCell?.({ dayOfWeek: day, slotDefinitionId: slotId });
    }
  };

  return (
    <div className="editor-grid-scroll">
      <table className="editor-grid" aria-label="Timetable editor grid">
        <thead>
          <tr>
            <th scope="col" className="editor-grid__corner">
              Slot \ Day
            </th>
            {dayAxis.map((day) => (
              <th key={day} scope="col">
                {day.charAt(0) + day.slice(1).toLowerCase()}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {slotAxis.map((slotId) => (
            <tr key={slotId}>
              <th scope="row" className="editor-grid__slot">
                Slot {slotId}
              </th>
              {dayAxis.map((day) => {
                const key = cellKey(day, slotId);
                const session = grid.get(key);
                const cellClasses = ['editor-grid__cell'];
                if (key === conflictCellKey) cellClasses.push('editor-grid__cell--conflict');
                if (key === pendingCellKey) cellClasses.push('editor-grid__cell--pending');
                return (
                  <td
                    key={key}
                    data-cell={key}
                    className={cellClasses.join(' ')}
                    tabIndex={0}
                    aria-label={`${day.charAt(0) + day.slice(1).toLowerCase()}, slot ${slotId}${
                      session ? ', occupied' : ', empty'
                    }`}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={handleDrop(day, slotId)}
                    onKeyDown={handleCellKeyDown(day, slotId)}
                  >
                    {session && (
                      <SessionCard
                        session={session}
                        state={cardState ? cardState(session, key) : 'normal'}
                        selected={String(session.id) === String(selectedSessionId)}
                        onPickUp={onPickUp}
                        onDragStart={onDragStartSession}
                        onDragEnd={onDragEndSession}
                      />
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

EditorGrid.propTypes = {
  dayAxis: PropTypes.arrayOf(PropTypes.string).isRequired,
  slotAxis: PropTypes.array.isRequired,
  grid: PropTypes.instanceOf(Map).isRequired,
  selectedSessionId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  conflictCellKey: PropTypes.string,
  pendingCellKey: PropTypes.string,
  cardState: PropTypes.func,
  onPickUp: PropTypes.func,
  onDragStartSession: PropTypes.func,
  onDragEndSession: PropTypes.func,
  onDropOnCell: PropTypes.func,
};
