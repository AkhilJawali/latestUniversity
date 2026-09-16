import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';

// Shared breadcrumb for the routed campus-hierarchy pages. Each item is either a
// link (ancestor) or plain text (current level). Codes are shown, not numeric ids.
export default function HierarchyBreadcrumb({ items }) {
  return (
    <nav className="breadcrumb" aria-label="Hierarchy path">
      {items.map((item, i) => {
        const isLast = i === items.length - 1;
        return (
          <span key={`${item.label}-${i}`} className="crumb-wrap">
            {item.to && !isLast ? (
              <Link className="crumb crumb--link" to={item.to}>
                {item.label}
              </Link>
            ) : (
              <span className={`crumb ${isLast ? 'crumb--active' : ''}`}>{item.label}</span>
            )}
            {!isLast && <span className="crumb-sep" aria-hidden="true">/</span>}
          </span>
        );
      })}
    </nav>
  );
}

HierarchyBreadcrumb.propTypes = {
  items: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      to: PropTypes.string,
    }),
  ).isRequired,
};
