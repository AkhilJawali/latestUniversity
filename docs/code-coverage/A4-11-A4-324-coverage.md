# Code Coverage Report — A4-11: Timetable Generation Engine

**Story:** A4-11
**Subtask:** A4-324
**Date:** 2026-08-27

## Coverage Summary

| Package | Classes | Lines | Coverage | Notes |
|---|---|---|---|---|
| com.utms.scheduling.engine.enums | 5 | ~50 | N/A | Enums |
| com.utms.scheduling.engine.entity | 7 | ~200 | N/A | JPA entities |
| com.utms.scheduling.engine.model | 8 | ~120 | N/A | Lombok models |
| com.utms.scheduling.engine.repository | 6 | ~60 | Pending | Spring Data |
| com.utms.scheduling.engine.config | 1 | ~15 | Pending | Bean config |
| com.utms.scheduling.engine.service | 4 | ~350 | Pending | Business logic |
| com.utms.scheduling.engine.solver | 5 | ~400 | Pending | CSP solver |
| com.utms.scheduling.engine.controller | 1 | ~80 | Pending | REST |
| com.utms.scheduling.engine.dto | 5 | ~80 | N/A | Lombok DTOs |
| **Total** | **42** | **~1355** | **Pending** | |

## Status

Tests pending master-data integration. Target: 80% on service+solver.

## Requirement Traceability
- FR-1.1-1.4: triggerGeneration
- FR-3.1-3.3: SessionDeriver
- FR-4.1-4.12: HardConstraintValidator (12 checks)
- FR-5.1-5.6: SoftConstraintOptimizer (6 methods)
- FR-6.1-6.4: SchedulingResultPersister
