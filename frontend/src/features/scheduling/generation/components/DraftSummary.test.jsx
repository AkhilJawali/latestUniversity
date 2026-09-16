import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import DraftSummary from './DraftSummary';

// A4-345 AC#3 — for a completed draft, the feasibility and quality scores are
// displayed alongside placement counts.
describe('DraftSummary', () => {
  const draft = {
    feasibilityScore: 0.95,
    qualityScore: 0.82,
    totalSessionsPlaced: 38,
    totalSessionsRequired: 40,
    version: 2,
    violationCount: 3,
    generatedAt: '2026-09-01T10:00:00Z',
  };

  it('renders feasibility and quality scores as percentages', () => {
    render(<DraftSummary draft={draft} />);
    expect(screen.getByText('Feasibility score')).toBeInTheDocument();
    expect(screen.getByText('95.0%')).toBeInTheDocument();
    expect(screen.getByText('Quality score')).toBeInTheDocument();
    expect(screen.getByText('82.0%')).toBeInTheDocument();
  });

  it('shows placed vs required session counts and violation count', () => {
    render(<DraftSummary draft={draft} />);
    expect(screen.getByText('38 / 40')).toBeInTheDocument();
    expect(screen.getByText('Soft-constraint violations')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('exposes the raw score in the title attribute', () => {
    render(<DraftSummary draft={draft} />);
    expect(screen.getByText('95.0%')).toHaveAttribute('title', '0.950');
  });

  it('renders nothing when no draft is provided', () => {
    const { container } = render(<DraftSummary draft={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
