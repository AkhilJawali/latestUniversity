import { describe, expect, it } from 'vitest';

import {
  actorHeaders,
  draftActions,
  draftTitle,
  isFinalLevel,
  isIncomplete,
  levelStage,
  workflowSummary,
} from './approval-flow';

const levels = [
  { levelIndex: 0, levelName: 'Coordinator' },
  { levelIndex: 1, levelName: 'HOD' },
  { levelIndex: 2, levelName: 'Dean/Registrar' },
];

describe('draftActions', () => {
  it('offers submit for a draft and publish for an approved draft only', () => {
    expect(draftActions('DRAFT')).toEqual(['submit']);
    expect(draftActions('APPROVED')).toEqual(['publish']);
    expect(draftActions('UNDER_REVIEW')).toEqual([]);
    expect(draftActions('PUBLISHED')).toEqual([]);
    expect(draftActions('SUPERSEDED')).toEqual([]);
  });
});

describe('levelStage', () => {
  const inReviewAtHod = { state: 'IN_REVIEW', currentLevelIndex: 1 };

  it('marks earlier levels done, the current level waiting and later levels not yet', () => {
    expect(levelStage(0, inReviewAtHod)).toBe('done');
    expect(levelStage(1, inReviewAtHod)).toBe('current');
    expect(levelStage(2, inReviewAtHod)).toBe('upcoming');
  });

  it('marks every level done once fully approved, and none before submission', () => {
    expect(levelStage(2, { state: 'APPROVED', currentLevelIndex: 2 })).toBe('done');
    expect(levelStage(0, null)).toBe('upcoming');
  });
});

describe('isFinalLevel', () => {
  it('is true only at the highest level', () => {
    expect(isFinalLevel({ currentLevelIndex: 2 }, levels)).toBe(true);
    expect(isFinalLevel({ currentLevelIndex: 1 }, levels)).toBe(false);
  });
});

describe('workflowSummary', () => {
  it('explains each position in plain words', () => {
    const draft = { status: 'UNDER_REVIEW' };
    expect(workflowSummary(draft, { state: 'IN_REVIEW', currentLevelName: 'HOD' })).toBe(
      'Waiting for HOD to approve or reject.',
    );
    expect(workflowSummary({ status: 'DRAFT' }, null)).toBe('Not sent for approval yet.');
    expect(workflowSummary({ status: 'PUBLISHED' }, { state: 'APPROVED' })).toMatch(/live timetable/);
  });
});

describe('small helpers', () => {
  it('names a draft by version, semester and year', () => {
    expect(draftTitle({ version: 3, semester: 'ODD', academicYear: '2024-25' })).toBe(
      'Version 3 · ODD 2024-25',
    );
  });

  it('detects an incomplete draft', () => {
    expect(isIncomplete({ totalSessionsPlaced: 30, totalSessionsRequired: 34 })).toBe(true);
    expect(isIncomplete({ totalSessionsPlaced: 34, totalSessionsRequired: 34 })).toBe(false);
  });

  it('sends the acting name as X-User-Id, dropping characters headers cannot carry', () => {
    expect(actorHeaders(' Dr. Rao (HOD) ')).toEqual({ headers: { 'X-User-Id': 'Dr. Rao (HOD)' } });
    expect(actorHeaders('Rão')).toEqual({ headers: { 'X-User-Id': 'Ro' } });
    expect(actorHeaders('')).toBeUndefined();
  });
});
