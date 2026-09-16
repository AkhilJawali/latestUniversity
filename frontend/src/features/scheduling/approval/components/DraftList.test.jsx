import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import DraftList from './DraftList';

describe('DraftList', () => {
  const drafts = [
    { id: 41, version: 3, semester: 'ODD', academicYear: '2024-25', status: 'DRAFT', totalSessionsPlaced: 30, totalSessionsRequired: 34 },
    { id: 42, version: 2, semester: 'ODD', academicYear: '2024-25', status: 'APPROVED', totalSessionsPlaced: 34, totalSessionsRequired: 34 },
    { id: 43, version: 1, semester: 'ODD', academicYear: '2024-25', status: 'PUBLISHED', totalSessionsPlaced: 34, totalSessionsRequired: 34 },
  ];
  const renderList = (props = {}) => {
    const handlers = { onSelect: vi.fn(), onAction: vi.fn() };
    render(<DraftList drafts={drafts} selectedDraftId={null} {...handlers} {...props} />);
    return handlers;
  };
  const bodyRows = () => screen.getAllByRole('row').slice(1);

  it('names drafts by version, semester and year and never shows their id', () => {
    renderList();
    expect(screen.getByText('Version 3 · ODD 2024-25')).toBeInTheDocument();
    expect(screen.getByText('30 / 34')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    ['41', '42', '43'].forEach((id) => expect(screen.queryByText(id)).not.toBeInTheDocument());
  });

  it('offers Submit only for a draft and Publish only for an approved draft', () => {
    renderList();
    const [draftRow, approvedRow, publishedRow] = bodyRows();
    expect(within(draftRow).getByRole('button', { name: 'Submit for approval' })).toBeInTheDocument();
    expect(within(draftRow).queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
    expect(within(approvedRow).getByRole('button', { name: 'Publish' })).toBeInTheDocument();
    expect(within(publishedRow).queryByRole('button', { name: /Submit|Publish/ })).not.toBeInTheDocument();
  });

  it('reports the chosen draft and action', () => {
    const { onSelect, onAction } = renderList();
    const [draftRow, approvedRow] = bodyRows();
    fireEvent.click(within(draftRow).getByRole('button', { name: 'Submit for approval' }));
    expect(onAction).toHaveBeenCalledWith(41, 'submit');
    fireEvent.click(within(approvedRow).getByRole('button', { name: 'View' }));
    expect(onSelect).toHaveBeenCalledWith(42);
  });
});
