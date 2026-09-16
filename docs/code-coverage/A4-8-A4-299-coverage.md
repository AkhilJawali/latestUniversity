# Code Coverage Report — A4-8 Resource Blocking Workflow

**Story Key:** A4-8
**Subtask Key:** A4-299
**Generated:** 2026-08-25

## Test Count: 11 (BlockServiceTest)

## Covered Scenarios
- raiseBlock: no impact to active, with impact to pending
- approveBlock: active + event emitted, not pending throws
- rejectBlock: records rejection
- withdrawBlock: by raiser
- releaseBlock: active to released, not active throws
- recordSoftBlockOverride: happy path (KD-33), invalid type rejected
- expirePendingBlocks: expired blocks transition

## Key Design Fixes Verified
- KD-32: Event-based A4-16 integration
- KD-33: SoftBlockOverride write path exists and tested
- KD-35: Pending expiry via scheduled job
- KD-37: Boundary-aware reporting

## Notes
- 11 unit tests, service layer isolation
- EnableScheduling added for expiry job
- JaCoCo via mvn test jacoco:report
