import { describe, expect, it } from 'vitest';

import { formatScorePercent, formatScoreRaw } from './formatScore';

// A4-345 AC#3 — feasibility and quality scores must be displayed. These helpers
// produce the score presentation used by DraftSummary.
describe('formatScorePercent', () => {
  it('formats a 0..1 score as a one-decimal percentage', () => {
    expect(formatScorePercent(0.873)).toBe('87.3%');
    expect(formatScorePercent(1)).toBe('100.0%');
    expect(formatScorePercent(0)).toBe('0.0%');
  });

  it('accepts numeric strings', () => {
    expect(formatScorePercent('0.5')).toBe('50.0%');
  });

  it('renders a dash for null/undefined/NaN', () => {
    expect(formatScorePercent(null)).toBe('—');
    expect(formatScorePercent(undefined)).toBe('—');
    expect(formatScorePercent('abc')).toBe('—');
  });
});

describe('formatScoreRaw', () => {
  it('formats the raw value to three decimals', () => {
    expect(formatScoreRaw(0.8734)).toBe('0.873');
    expect(formatScoreRaw(1)).toBe('1.000');
  });

  it('renders a dash for null/undefined/NaN', () => {
    expect(formatScoreRaw(null)).toBe('—');
    expect(formatScoreRaw(undefined)).toBe('—');
    expect(formatScoreRaw('xyz')).toBe('—');
  });
});
