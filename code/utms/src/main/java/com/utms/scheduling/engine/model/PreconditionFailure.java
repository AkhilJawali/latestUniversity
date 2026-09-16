package com.utms.scheduling.engine.model;

/**
 * Represents a single precondition check failure (FR-1.2/1.3).
 * All failures are collected and returned together, not fail-fast.
 */
public record PreconditionFailure(String check, String message) {
}
