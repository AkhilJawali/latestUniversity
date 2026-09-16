# Unit Test Results — A4-7 Schedulable Asset Master Data

**Story Key:** A4-7
**Generated:** 2026-08-26
**Framework:** JUnit 5 + Mockito
**Status:** Tests written — awaiting execution in IntelliJ

## Results Summary

| Test Class | Tests | Expected Pass | Notes |
|---|---|---|---|
| AssetServiceTest | 6 | 6 | All assertions verified at code level |
| **Total** | **6** | **6** | Run `mvn test` in IntelliJ to confirm |

## Test Details

### AssetServiceTest (6 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | create_validRequest_returnsAssetDto | Happy path — valid department, campus, identifier, availability windows | Asset saved with windows, DTO returned |
| 2 | create_invalidDepartment_throwsEntityNotFound | Owning department ID doesn't exist | EntityNotFoundException thrown |
| 3 | create_duplicateIdentifier_throwsConflict | Asset identifier already exists (system-wide) | ConflictException thrown |
| 4 | update_validRequest_replacesWindows | Update with new availability windows | Old windows cleared, new windows set |
| 5 | delete_noActiveBlocks_softDeletes | No active resource blocks on asset | deletedAt set, isActive=false |
| 6 | delete_notFound_throwsEntityNotFoundException | Asset ID doesn't exist | EntityNotFoundException thrown |

## Issues Found

None identified at code level. Actual runtime verification pending.

## Notes

- Tests written using Mockito (service layer isolation — no database)
- Availability windows (day-of-week + start/end time) tested as part of create flow
- Window replacement on update verified (full replace strategy, not merge)
- Asset identifier uniqueness enforced system-wide (not scoped to campus)
- KD-28: Availability windows included in create DTO (verified)
- Actual pass/fail results require running `mvn test -Dtest=AssetServiceTest`
