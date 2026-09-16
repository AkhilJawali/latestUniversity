import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

// Admin/coordinator shell: header + always-visible grouped left nav + content.
// Items are grouped so the information architecture is obvious: general, then
// Master Data (the campus hierarchy + all reference data), then Scheduling.
const NAV_GROUPS = [
  {
    title: null,
    items: [{ to: '/', label: 'Dashboard', glyph: '⌂', end: true }],
  },
  {
    title: 'Master Data',
    items: [
      { to: '/master-data/campus-hierarchy', label: 'Campus Hierarchy', glyph: '🏛' },
      { to: '/master-data/courses', label: 'Courses', glyph: '📚' },
      { to: '/master-data/faculty', label: 'Faculty', glyph: '👤' },
      { to: '/master-data/faculty-availability', label: 'Faculty Availability', glyph: '🕑' },
      { to: '/master-data/rooms', label: 'Rooms', glyph: '🚪' },
      { to: '/master-data/assets', label: 'Assets', glyph: '📦' },
      { to: '/master-data/academic-calendar', label: 'Academic Calendar', glyph: '📅' },
      { to: '/master-data/time-slot-grid', label: 'Time-Slot Grid', glyph: '⏱' },
    ],
  },
  {
    title: 'Scheduling',
    items: [
      { to: '/scheduling/config', label: 'Engine Configuration', glyph: '⚙' },
      { to: '/scheduling/generation', label: 'Timetable Generation', glyph: '🗓' },
      { to: '/scheduling/approvals', label: 'Approvals', glyph: '✅' },
    ],
  },
];

export default function AppShell({ children }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="app-logo">UTMS</span>
        <span className="app-header-sub">Timetable Management</span>
      </header>
      <div className="app-body">
        <nav className="app-sidebar" aria-label="Primary">
          {NAV_GROUPS.map((group, gi) => (
            <div className="nav-group" key={group.title ?? `group-${gi}`}>
              {group.title && <p className="nav-group-title">{group.title}</p>}
              <ul>
                {group.items.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      end={item.end}
                      className={({ isActive }) =>
                        isActive ? 'nav-link nav-link--active' : 'nav-link'
                      }
                    >
                      <span className="nav-glyph" aria-hidden="true">
                        {item.glyph}
                      </span>
                      <span className="nav-label">{item.label}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
        <main className="app-main">{children}</main>
      </div>
    </div>
  );
}

AppShell.propTypes = {
  children: PropTypes.node,
};
