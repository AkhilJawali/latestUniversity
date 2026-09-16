// A4-345 PD-4 — score presentation. The backend feasibility/quality scores are
// returned as numbers in the 0..1 range. We show them as a percentage with one
// decimal for readability, keeping the raw value available for a tooltip.

/** Format a 0..1 score as a percentage string, e.g. 0.873 -> "87.3%". */
export function formatScorePercent(score) {
  if (score == null || Number.isNaN(Number(score))) return '—';
  return `${(Number(score) * 100).toFixed(1)}%`;
}

/** Raw score as a fixed-precision string for tooltips / secondary text. */
export function formatScoreRaw(score) {
  if (score == null || Number.isNaN(Number(score))) return '—';
  return Number(score).toFixed(3);
}
