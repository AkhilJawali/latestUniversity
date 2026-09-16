# Unit Test Results — A4-6 Room and Lab Master Data

**Story Key:** A4-6
**Generated:** 2026-08-26
**Framework:** JUnit 5 + Mockito
**Status:** Tests written — awaiting execution in IntelliJ

## Results Summary

| Test Class | Tests | Expected Pass | Notes |
|---|---|---|---|
| RoomServiceTest | 10 | 10 | All assertions verified at code level |
| **Total** | **10** | **10** | Run `mvn test` in IntelliJ to confirm |

## Test Details

### RoomServiceTest (10 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | create_validRequest_returnsRoomDto | Happy path — valid campus, code, capacity, equipment tags | Room saved, DTO returned |
| 2 | create_duplicateCode_throwsConflict | Room code already exists for same campus | ConflictException thrown |
| 3 | create_invalidCampus_throwsEntityNotFound | Campus ID doesn't exist | EntityNotFoundException thrown |
| 4 | update_capacityDecrease_emitsCapacityChangedEvent | Capacity reduced from 100 to 60 | RoomCapacityChangedEvent emitted with old/new values |
| 5 | update_capacityIncrease_doesNotEmitEvent | Capacity increased from 50 to 80 | No RoomCapacityChangedEvent emitted |
| 6 | update_tagRemoval_emitsEquipmentChangedEvent | Tags reduced (MICROSCOPE, FUME_HOOD removed) | RoomEquipmentChangedEvent emitted with removedTags list |
| 7 | update_tagAddition_doesNotEmitEquipmentEvent | Tag added (WHITEBOARD added) | No RoomEquipmentChangedEvent emitted |
| 8 | delete_noActiveReferences_softDeletes | No active sessions referencing room | deletedAt set, isActive=false |
| 9 | delete_notFound_throwsEntityNotFoundException | Room ID doesn't exist | EntityNotFoundException thrown |
| 10 | findById_exists_returnsDto | Valid room ID | RoomDto returned |

## Issues Found

None identified at code level. Actual runtime verification pending.

## Notes

- Tests written using Mockito (service layer isolation — no database)
- Event emission verified via ArgumentCaptor for both RoomCapacityChangedEvent and RoomEquipmentChangedEvent
- Capacity decrease detection: event only fires when newCapacity < oldCapacity (sessions may exceed new capacity)
- Equipment tag removal detection: event only fires when tags are removed (sessions requiring removed tags may become invalid)
- Actual pass/fail results require running `mvn test -Dtest=RoomServiceTest`
