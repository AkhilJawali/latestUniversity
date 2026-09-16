import PropTypes from 'prop-types';

// Temporary page shown for routes whose feature story hasn't been implemented yet.
export default function PlaceholderPage({ title, note }) {
  return (
    <section>
      <h1 className="page-title">{title}</h1>
      <p className="page-subtitle">{note}</p>
      <div className="empty-state">
        <p>This screen is coming soon.</p>
      </div>
    </section>
  );
}

PlaceholderPage.propTypes = {
  title: PropTypes.string.isRequired,
  note: PropTypes.string,
};
