package com.utms.masterdata.room;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final CampusRepository campusRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public RoomDto create(CreateRoomRequest request) {
        // Validate campus exists
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new EntityNotFoundException("Campus", request.getCampusId()));

        // Check duplicate code within campus
        if (roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull(request.getCode(), request.getCampusId())) {
            throw new ConflictException("Room with code '" + request.getCode() + "' already exists in this campus");
        }

        Room room = roomMapper.toEntity(request);
        room.setCampus(campus);
        room.setIsActive(true);
        if (room.getEquipmentTags() == null) {
            room.setEquipmentTags(new ArrayList<>());
        }
        room = roomRepository.save(room);

        auditEventPublisher.publish(new AuditEvent(
                "Room", room.getId(), AuditEvent.Action.CREATED, null, room, "system", Instant.now()));

        log.info("Room created: id={}, code={}, campusId={}", room.getId(), room.getCode(), campus.getId());
        return roomMapper.toDto(room);
    }

    @Transactional(readOnly = true)
    public RoomDto findById(Long id) {
        Room room = findActiveByIdOrThrow(id);
        return roomMapper.toDto(room);
    }

    @Transactional(readOnly = true)
    public Page<RoomDto> findAll(Long campusId, RoomType roomType, String building,
                                 Integer minCapacity, String equipmentTag, Pageable pageable) {
        Specification<Room> spec = notDeleted();

        if (campusId != null) {
            Specification<Room> campusSpec = byCampusId(campusId);
            spec = spec.and(campusSpec);
        }
        if (roomType != null) {
            Specification<Room> typeSpec = byRoomType(roomType);
            spec = spec.and(typeSpec);
        }
        if (building != null && !building.isBlank()) {
            Specification<Room> buildingSpec = byBuilding(building);
            spec = spec.and(buildingSpec);
        }
        if (minCapacity != null) {
            Specification<Room> capSpec = byMinCapacity(minCapacity);
            spec = spec.and(capSpec);
        }
        if (equipmentTag != null && !equipmentTag.isBlank()) {
            Specification<Room> tagSpec = byEquipmentTag(equipmentTag);
            spec = spec.and(tagSpec);
        }

        return roomRepository.findAll(spec, pageable).map(roomMapper::toDto);
    }

    @Transactional
    public RoomDto update(Long id, UpdateRoomRequest request) {
        Room room = findActiveByIdOrThrow(id);

        Integer oldCapacity = room.getCapacity();
        List<String> oldTags = new ArrayList<>(room.getEquipmentTags());

        roomMapper.updateEntity(request, room);
        if (room.getEquipmentTags() == null) {
            room.setEquipmentTags(new ArrayList<>());
        }
        room = roomRepository.save(room);

        // Emit capacity changed event if capacity decreased
        if (request.getCapacity() < oldCapacity) {
            applicationEventPublisher.publishEvent(
                    new RoomCapacityChangedEvent(room.getId(), oldCapacity, request.getCapacity()));
            log.info("Room capacity decreased: id={}, old={}, new={}", room.getId(), oldCapacity, request.getCapacity());
        }

        // Emit equipment changed event if tags were removed
        List<String> newTags = room.getEquipmentTags();
        List<String> removedTags = oldTags.stream()
                .filter(tag -> !newTags.contains(tag))
                .toList();
        if (!removedTags.isEmpty()) {
            applicationEventPublisher.publishEvent(
                    new RoomEquipmentChangedEvent(room.getId(), removedTags));
            log.info("Room equipment tags removed: id={}, removed={}", room.getId(), removedTags);
        }

        auditEventPublisher.publish(new AuditEvent(
                "Room", room.getId(), AuditEvent.Action.UPDATED, oldCapacity, room, "system", Instant.now()));

        log.info("Room updated: id={}", room.getId());
        return roomMapper.toDto(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = findActiveByIdOrThrow(id);

        // Check active references only — historical refs do NOT block (KD-23)
        List<Map<String, Object>> references = new ArrayList<>();
        // TODO: Check active scheduled sessions referencing this room once session module is built
        // long activeSessionCount = sessionRepository.countByRoomIdAndDeletedAtIsNull(id);
        // if (activeSessionCount > 0) {
        //     references.add(Map.of("referenceType", "ScheduledSession", "count", activeSessionCount));
        // }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot delete room: has active references", references);
        }

        room.setDeletedAt(LocalDateTime.now());
        room.setIsActive(false);
        roomRepository.save(room);

        auditEventPublisher.publish(new AuditEvent(
                "Room", room.getId(), AuditEvent.Action.DELETED, room, null, "system", Instant.now()));

        log.info("Room soft-deleted: id={}, code={}", room.getId(), room.getCode());
    }

    private Room findActiveByIdOrThrow(Long id) {
        return roomRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Room", id));
    }

    private static Specification<Room> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Room> byCampusId(Long campusId) {
        return (root, query, cb) -> cb.equal(root.get("campus").get("id"), campusId);
    }

    private static Specification<Room> byRoomType(RoomType roomType) {
        return (root, query, cb) -> cb.equal(root.get("roomType"), roomType);
    }

    private static Specification<Room> byBuilding(String building) {
        return (root, query, cb) -> cb.equal(root.get("building"), building);
    }

    private static Specification<Room> byMinCapacity(Integer minCapacity) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("capacity"), minCapacity);
    }

    private static Specification<Room> byEquipmentTag(String equipmentTag) {
        return (root, query, cb) -> cb.isMember(equipmentTag, root.get("equipmentTags"));
    }
}
