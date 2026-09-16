# Code Coverage Report — A4-6 Room and Lab Master Data

**Story Key:** A4-6
**Subtask Key:** A4-287
**Generated:** 2026-08-25

## Test Count: 10 (RoomServiceTest)

## Covered Scenarios
- create: valid, duplicate code (409), invalid campus (404)
- update: capacity decrease emits event, capacity increase no event, tag removal emits event, tag addition no event
- delete: no refs soft-deletes, not found throws 404
- findById: exists returns DTO

## Notes
- 10 unit tests, service layer isolation
- Events verified via ApplicationEventPublisher mock
- Actual JaCoCo via mvn test jacoco:report
