package com.utms.masterdata.room;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RoomService roomService;

    // --- create ---

    @Test
    void create_validRequest_returnsRoomDto() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .name("Room 101")
                .code("R101")
                .campusId(1L)
                .capacity(50)
                .roomType(RoomType.CLASSROOM)
                .equipmentTags(List.of("PROJECTOR", "WHITEBOARD"))
                .building("Block A")
                .floor("1")
                .build();

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");

        Room room = new Room();
        room.setId(1L);
        room.setName("Room 101");
        room.setCode("R101");
        room.setCampus(campus);
        room.setCapacity(50);
        room.setRoomType(RoomType.CLASSROOM);
        room.setEquipmentTags(List.of("PROJECTOR", "WHITEBOARD"));
        room.setBuilding("Block A");
        room.setFloor("1");
        room.setIsActive(true);

        RoomDto expectedDto = RoomDto.builder()
                .id(1L)
                .name("Room 101")
                .code("R101")
                .campusId(1L)
                .campusName("Main Campus")
                .capacity(50)
                .roomType(RoomType.CLASSROOM)
                .equipmentTags(List.of("PROJECTOR", "WHITEBOARD"))
                .building("Block A")
                .floor("1")
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("R101", 1L)).thenReturn(false);
        when(roomMapper.toEntity(request)).thenReturn(room);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        RoomDto result = roomService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Room 101", result.getName());
        assertEquals("R101", result.getCode());
        assertEquals(50, result.getCapacity());
        assertEquals(RoomType.CLASSROOM, result.getRoomType());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .name("Room 101")
                .code("R101")
                .campusId(1L)
                .capacity(50)
                .roomType(RoomType.CLASSROOM)
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("R101", 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> roomService.create(request));

        assertTrue(exception.getMessage().contains("R101"));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void create_invalidCampus_throwsEntityNotFound() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .name("Room 101")
                .code("R101")
                .campusId(99L)
                .capacity(50)
                .roomType(RoomType.LAB)
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> roomService.create(request));

        assertTrue(exception.getMessage().contains("Campus"));
        verify(roomRepository, never()).save(any());
    }

    // --- update with capacity decrease event ---

    @Test
    void update_capacityDecrease_emitsCapacityChangedEvent() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Room 101");
        room.setCode("R101");
        room.setCapacity(100);
        room.setRoomType(RoomType.CLASSROOM);
        room.setEquipmentTags(new ArrayList<>(List.of("PROJECTOR")));

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main");
        room.setCampus(campus);

        UpdateRoomRequest request = UpdateRoomRequest.builder()
                .name("Room 101")
                .capacity(60)
                .roomType(RoomType.CLASSROOM)
                .equipmentTags(List.of("PROJECTOR"))
                .build();

        RoomDto expectedDto = RoomDto.builder()
                .id(1L)
                .name("Room 101")
                .capacity(60)
                .build();

        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        roomService.update(1L, request);

        ArgumentCaptor<RoomCapacityChangedEvent> captor = ArgumentCaptor.forClass(RoomCapacityChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        RoomCapacityChangedEvent event = captor.getValue();
        assertEquals(1L, event.roomId());
        assertEquals(100, event.oldCapacity());
        assertEquals(60, event.newCapacity());
    }

    @Test
    void update_capacityIncrease_doesNotEmitEvent() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Room 101");
        room.setCode("R101");
        room.setCapacity(50);
        room.setRoomType(RoomType.CLASSROOM);
        room.setEquipmentTags(new ArrayList<>(List.of("PROJECTOR")));

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main");
        room.setCampus(campus);

        UpdateRoomRequest request = UpdateRoomRequest.builder()
                .name("Room 101")
                .capacity(80)
                .roomType(RoomType.CLASSROOM)
                .equipmentTags(List.of("PROJECTOR"))
                .build();

        RoomDto expectedDto = RoomDto.builder().id(1L).build();

        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        roomService.update(1L, request);

        verify(applicationEventPublisher, never()).publishEvent(any(RoomCapacityChangedEvent.class));
    }

    // --- update with tag removal event ---

    @Test
    void update_tagRemoval_emitsEquipmentChangedEvent() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Lab 1");
        room.setCode("LAB1");
        room.setCapacity(30);
        room.setRoomType(RoomType.LAB);
        room.setEquipmentTags(new ArrayList<>(List.of("PROJECTOR", "MICROSCOPE", "FUME_HOOD")));

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main");
        room.setCampus(campus);

        UpdateRoomRequest request = UpdateRoomRequest.builder()
                .name("Lab 1")
                .capacity(30)
                .roomType(RoomType.LAB)
                .equipmentTags(List.of("PROJECTOR"))
                .build();

        // Simulate mapper updating the entity
        doAnswer(invocation -> {
            Room target = invocation.getArgument(1);
            target.setEquipmentTags(new ArrayList<>(List.of("PROJECTOR")));
            return null;
        }).when(roomMapper).updateEntity(any(), any());

        RoomDto expectedDto = RoomDto.builder().id(1L).build();

        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        roomService.update(1L, request);

        ArgumentCaptor<RoomEquipmentChangedEvent> captor = ArgumentCaptor.forClass(RoomEquipmentChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        RoomEquipmentChangedEvent event = captor.getValue();
        assertEquals(1L, event.roomId());
        assertTrue(event.removedTags().contains("MICROSCOPE"));
        assertTrue(event.removedTags().contains("FUME_HOOD"));
        assertEquals(2, event.removedTags().size());
    }

    @Test
    void update_tagAddition_doesNotEmitEquipmentEvent() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Room 101");
        room.setCode("R101");
        room.setCapacity(50);
        room.setRoomType(RoomType.CLASSROOM);
        room.setEquipmentTags(new ArrayList<>(List.of("PROJECTOR")));

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main");
        room.setCampus(campus);

        UpdateRoomRequest request = UpdateRoomRequest.builder()
                .name("Room 101")
                .capacity(50)
                .roomType(RoomType.CLASSROOM)
                .equipmentTags(List.of("PROJECTOR", "WHITEBOARD"))
                .build();

        // Simulate mapper adding the new tag
        doAnswer(invocation -> {
            Room target = invocation.getArgument(1);
            target.setEquipmentTags(new ArrayList<>(List.of("PROJECTOR", "WHITEBOARD")));
            return null;
        }).when(roomMapper).updateEntity(any(), any());

        RoomDto expectedDto = RoomDto.builder().id(1L).build();

        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        roomService.update(1L, request);

        verify(applicationEventPublisher, never()).publishEvent(any(RoomEquipmentChangedEvent.class));
    }

    // --- delete ---

    @Test
    void delete_noActiveReferences_softDeletes() {
        Room room = new Room();
        room.setId(1L);
        room.setCode("R101");
        room.setIsActive(true);
        room.setEquipmentTags(new ArrayList<>());

        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));

        roomService.delete(1L);

        assertNotNull(room.getDeletedAt());
        assertFalse(room.getIsActive());
        verify(roomRepository).save(room);
    }

    @Test
    void delete_notFound_throwsEntityNotFoundException() {
        when(roomRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roomService.delete(99L));

        verify(roomRepository, never()).save(any());
    }

    // --- findById ---

    @Test
    void findById_exists_returnsRoomDto() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Room 101");
        room.setCode("R101");

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main");
        room.setCampus(campus);

        RoomDto expectedDto = RoomDto.builder()
                .id(1L)
                .name("Room 101")
                .code("R101")
                .campusId(1L)
                .campusName("Main")
                .build();

        when(roomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(room));
        when(roomMapper.toDto(room)).thenReturn(expectedDto);

        RoomDto result = roomService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Room 101", result.getName());
    }
}
