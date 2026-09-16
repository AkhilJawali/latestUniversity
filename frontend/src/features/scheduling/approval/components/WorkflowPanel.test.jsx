import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import WorkflowPanel from './WorkflowPanel';

describe('WorkflowPanel', () => {
  const levels = [
    { levelIndex: 0, levelName: 'Coordinator' },
    { levelIndex: 1, levelName: 'HOD' },
    { levelIndex: 2, levelName: 'Dean/Registrar' },
  ];
  const underReview = {
    id: 41,
    version: 3,
    semester: 'ODD',
    academicYear: '2024-25',
    status: 'UNDER_REVIEW',
    totalSessionsPlaced: 34,
    totalSessionsRequired: 34,
  };
  const atHod = {
    id: 7,
    pipelineId: 1,
    state: 'IN_REVIEW',
    currentLevelIndex: 1,
    currentLevelName: 'HOD',
    steps: [
      {
        id: 1,
        levelIndex: 1,
        levelName: 'HOD',
        action: 'SUBMITTED',
        actorUserId: 'Asha (Coordinator)',
        comments: 'Please review',
        actedAt: '2026-09-11T10:00:00',
      },
    ],
  };
  const renderPanel = (props = {}) => {
    const handlers = { onAction: vi.fn(), onConfirm: vi.fn(), onCancel: vi.fn() };
    render(<WorkflowPanel draft={underReview} workflow={atHod} levels={levels} {...handlers} {...props} />);
    return handlers;
  };

  it('shows where the draft is in the approval chain and its history', () => {
    renderPanel();
    expect(screen.getByRole('heading', { name: 'Version 3 · ODD 2024-25' })).toBeInTheDocument();
    expect(screen.getByText('Waiting for HOD to approve or reject.')).toBeInTheDocument();
    const current = screen.getByRole('listitem', { current: 'step' });
    expect(within(current).getByText('HOD')).toBeInTheDocument();
    expect(screen.getByText('Asha (Coordinator)')).toBeInTheDocument();
    expect(screen.getByText('Please review')).toBeInTheDocument();
  });

  it('offers approve and reject while the draft is in review', () => {
    const { onAction } = renderPanel();
    fireEvent.click(screen.getByRole('button', { name: 'Approve as HOD' }));
    expect(onAction).toHaveBeenCalledWith('approve');
    expect(screen.getByRole('button', { name: 'Reject' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
  });

  it('requires a reason before rejecting', () => {
    const { onConfirm } = renderPanel({ action: 'reject' });
    fireEvent.click(screen.getByRole('button', { name: 'Reject' }));
    expect(screen.getByText('Please give a reason for rejecting.')).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/^Reason/), { target: { value: 'Room clash on Monday' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reject' }));
    expect(onConfirm).toHaveBeenCalledWith('reject', { comments: '', rejectionReason: 'Room clash on Monday' });
  });

  it('warns that the final approval publishes the timetable', () => {
    renderPanel({
      action: 'approve',
      workflow: { ...atHod, currentLevelIndex: 2, currentLevelName: 'Dean/Registrar' },
    });
    expect(screen.getByText(/final approval — the timetable will be published/)).toBeInTheDocument();
  });

  it('offers Publish for an approved draft', () => {
    const { onAction } = renderPanel({
      draft: { ...underReview, status: 'APPROVED' },
      workflow: { ...atHod, state: 'APPROVED', currentLevelIndex: 2, currentLevelName: 'Dean/Registrar' },
    });
    expect(screen.getByText('Fully approved. Publish it to make it the live timetable.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Publish' }));
    expect(onAction).toHaveBeenCalledWith('publish');
  });
});
