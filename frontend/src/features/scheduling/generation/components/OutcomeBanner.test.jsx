import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import OutcomeBanner from './OutcomeBanner';

// A4-345 AC#5 — a timed-out (partial) or infeasible result must be clearly
// indicated to the coordinator. The banner conveys the outcome by text (not
// colour alone) for every terminal GenerationStatus.
describe('OutcomeBanner', () => {
  it('clearly indicates a partial (timed out) outcome', () => {
    render(<OutcomeBanner status="TIMED_OUT" />);
    expect(screen.getByText('Partial (timed out)')).toBeInTheDocument();
    expect(screen.getByText(/partial draft/i)).toBeInTheDocument();
  });

  it('clearly indicates an infeasible outcome', () => {
    render(<OutcomeBanner status="INFEASIBLE" />);
    expect(screen.getByText('Infeasible')).toBeInTheDocument();
    expect(screen.getByText(/no valid timetable exists/i)).toBeInTheDocument();
  });

  it('indicates a completed outcome', () => {
    render(<OutcomeBanner status="COMPLETED" />);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('indicates failed and cancelled outcomes', () => {
    const { rerender } = render(<OutcomeBanner status="FAILED" />);
    expect(screen.getByText('Failed')).toBeInTheDocument();

    rerender(<OutcomeBanner status="CANCELLED" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('exposes the outcome as a status region for assistive tech', () => {
    render(<OutcomeBanner status="COMPLETED" />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders nothing for an unknown status', () => {
    const { container } = render(<OutcomeBanner status="IN_PROGRESS" />);
    expect(container).toBeEmptyDOMElement();
  });
});
