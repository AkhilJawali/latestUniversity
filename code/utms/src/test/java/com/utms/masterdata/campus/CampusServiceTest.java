package com.utms.masterdata.campus;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private CampusMapper campusMapper;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private CampusService campusService;

    // --- create ---

    @Test
    void create_validRequest_returnsCampusDto() {
        CreateCampusRequest request = CreateCampusRequest.builder()
                .name("Main Campus")
                .code("MAIN")
                .location("City Center")
                .build();

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");
        campus.setLocation("City Center");
        campus.setIsActive(true);

        CampusDto expectedDto = CampusDto.builder()
                .id(1L)
                .name("Main Campus")
                .code("MAIN")
                .location("City Center")
                .build();

        when(campusRepository.existsByCodeAndDeletedAtIsNull("MAIN")).thenReturn(false);
        when(campusMapper.toEntity(request)).thenReturn(campus);
        when(campusRepository.save(any(Campus.class))).thenReturn(campus);
        when(campusMapper.toDto(campus)).thenReturn(expectedDto);

        CampusDto result = campusService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Main Campus", result.getName());
        assertEquals("MAIN", result.getCode());
        verify(campusRepository).save(any(Campus.class));
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        CreateCampusRequest request = CreateCampusRequest.builder()
                .name("Another Campus")
                .code("MAIN")
                .location("Suburb")
                .build();

        when(campusRepository.existsByCodeAndDeletedAtIsNull("MAIN")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> campusService.create(request));

        assertTrue(exception.getMessage().contains("MAIN"));
        verify(campusRepository, never()).save(any());
    }

    // --- findById ---

    @Test
    void findById_exists_returnsCampusDto() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");

        CampusDto expectedDto = CampusDto.builder()
                .id(1L)
                .name("Main Campus")
                .code("MAIN")
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(campusMapper.toDto(campus)).thenReturn(expectedDto);

        CampusDto result = campusService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Main Campus", result.getName());
    }

    @Test
    void findById_notFound_throwsEntityNotFoundException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> campusService.findById(99L));

        assertTrue(exception.getMessage().contains("Campus"));
        assertTrue(exception.getMessage().contains("99"));
    }

    // --- findByCode ---

    @Test
    void findByCode_exists_returnsCampusDto() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");

        CampusDto expectedDto = CampusDto.builder()
                .id(1L)
                .name("Main Campus")
                .code("MAIN")
                .build();

        when(campusRepository.findByCodeAndDeletedAtIsNull("MAIN")).thenReturn(Optional.of(campus));
        when(campusMapper.toDto(campus)).thenReturn(expectedDto);

        CampusDto result = campusService.findByCode("MAIN");

        assertNotNull(result);
        assertEquals("MAIN", result.getCode());
    }

    @Test
    void findByCode_notFound_throwsEntityNotFoundException() {
        when(campusRepository.findByCodeAndDeletedAtIsNull("NONEXIST")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> campusService.findByCode("NONEXIST"));

        assertTrue(exception.getMessage().contains("Campus"));
        assertTrue(exception.getMessage().contains("NONEXIST"));
    }

    // --- update ---

    @Test
    void update_validRequest_returnsCampusDto() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Old Name");
        campus.setCode("MAIN");
        campus.setLocation("Old Location");

        UpdateCampusRequest request = UpdateCampusRequest.builder()
                .name("Updated Name")
                .location("New Location")
                .build();

        CampusDto expectedDto = CampusDto.builder()
                .id(1L)
                .name("Updated Name")
                .code("MAIN")
                .location("New Location")
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(campusRepository.save(any(Campus.class))).thenReturn(campus);
        when(campusMapper.toDto(campus)).thenReturn(expectedDto);

        CampusDto result = campusService.update(1L, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        verify(campusMapper).updateEntity(eq(request), eq(campus));
        verify(campusRepository).save(campus);
    }

    @Test
    void update_notFound_throwsEntityNotFoundException() {
        UpdateCampusRequest request = UpdateCampusRequest.builder()
                .name("Updated Name")
                .location("New Location")
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> campusService.update(99L, request));

        verify(campusRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_noReferences_softDeletes() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setCode("MAIN");
        campus.setIsActive(true);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.countByCampusIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        campusService.delete(1L);

        assertNotNull(campus.getDeletedAt());
        assertFalse(campus.getIsActive());
        verify(campusRepository).save(campus);
    }

    @Test
    void delete_hasDepartments_throwsBusinessRuleViolation() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setCode("MAIN");
        campus.setIsActive(true);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.countByCampusIdAndDeletedAtIsNull(1L)).thenReturn(3L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> campusService.delete(1L));

        assertTrue(exception.getMessage().contains("Cannot delete campus"));
        assertFalse(exception.getDetails().isEmpty());
        verify(campusRepository, never()).save(any());
    }
}
