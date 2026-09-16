package com.utms.masterdata.room;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAvailabilityQueryServiceImpl implements RoomAvailabilityQueryService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomAvailabilityStatus> getAvailability(Long campusId, DayOfWeek day,
                                                         LocalTime startTime, LocalTime endTime) {
        // Find all active rooms in the campus
        Specification<Room> spec = notDeleted();
        Specification<Room> campusSpec = byCampusId(campusId);
        spec = spec.and(campusSpec);

        List<Room> rooms = roomRepository.findAll(spec);

        // For each room, determine status by checking sessions and blocks
        return rooms.stream()
                .map(room -> determineStatus(room, day, startTime, endTime))
                .toList();
    }

    private RoomAvailabilityStatus determineStatus(Room room, DayOfWeek day,
                                                    LocalTime startTime, LocalTime endTime) {
        // TODO: Check scheduled sessions occupying this room at this time
        //       once the scheduling/session module is built

        // TODO: Check resource blocks on this room at this time
        //       once A4-8 (resource block module) is built

        // Default: all rooms are FREE until session/block modules are integrated
        return new RoomAvailabilityStatus(room.getId(), room.getName(), room.getCode(), RoomStatus.FREE);
    }

    private static Specification<Room> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Room> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campus").get("id"), campusId);
    }
}
