import PropTypes from 'prop-types';
import { Component } from 'react';

// Root error boundary (A4-335 design Section 6). Contains render errors and
// failed lazy-chunk imports so a feature failure does not white-screen the app.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    // eslint-disable-next-line no-console
    console.error('Unhandled UI error:', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="empty-state" role="alert">
          <p>Something went wrong. Please reload the page.</p>
        </div>
      );
    }
    return this.props.children;
  }
}

ErrorBoundary.propTypes = {
  children: PropTypes.node,
};
