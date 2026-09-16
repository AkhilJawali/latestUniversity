# Code Coverage Report — A4-7 Schedulable Asset Master Data

**Story Key:** A4-7
**Subtask Key:** A4-292
**Generated:** 2026-08-25

## Test Count: 6 (AssetServiceTest)

## Covered Scenarios
- create: happy (with calendar windows), invalid department (404)
- update: replace calendar windows
- delete: active blocks block (422), historical only allows (200) per KD-30
- findById: exists returns DTO

## Notes
- Calendar in create DTO verified (KD-28 fix)
- AssetAvailabilityQueryService checks calendar AND blocks (KD-29)
- Actual JaCoCo via mvn test jacoco:report
