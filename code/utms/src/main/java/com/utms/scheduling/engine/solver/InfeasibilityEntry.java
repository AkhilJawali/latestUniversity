package com.utms.scheduling.engine.solver;

import java.util.List;

/**
 * In-memory representation of one infeasibility reason collected during solving.
 * Converted to InfeasibilityConflict entity during persistence.
 *
 * @param sessionDescription human-readable description (e.g., "CS301 Lecture — Dr. Sharma — Batch 3A")
 * @param constraintTypes list of constraint type names that caused the empty domain
 * @param explanation actionable description of why the session cannot be placed
 */
public record InfeasibilityEntry(
    String sessionDescription,
    List<String> constraintTypes,
    String explanation
) {}
