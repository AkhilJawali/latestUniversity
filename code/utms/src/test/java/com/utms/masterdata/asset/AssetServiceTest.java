package com.utms.masterdata.asset;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private SchedulableAssetRepository assetRepository;

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private AssetService assetService;

    // --- create ---

    @Test
    void create_validRequest_returnsAssetDto() {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Projector Unit A")
                .identifier("PROJ-A1")
                .assetType("PROJECTOR")
                .owningDepartmentId(1L)
                .campusId(1L)
                .availabilityWindows(List.of(
                        CreateAvailabilityWindowRequest.builder()
                                .dayOfWeek("MONDAY")
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(17, 0))
                                .build()
                ))
                .build();

        Department department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");

        SchedulableAsset asset = new SchedulableAsset();
        asset.setId(1L);
        asset.setName("Projector Unit A");
        asset.setIdentifier("PROJ-A1");
        asset.setAssetType("PROJECTOR");
        asset.setOwningDepartment(department);
        asset.setCampus(campus);
        asset.setIsActive(true);
        asset.setAvailabilityWindows(new ArrayList<>());

        AssetDto expectedDto = AssetDto.builder()
                .id(1L)
                .name("Projector Unit A")
                .identifier("PROJ-A1")
                .assetType("PROJECTOR")
                .owningDepartmentId(1L)
                .owningDepartmentName("Computer Science")
                .campusId(1L)
                .campusName("Main Campus")
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(assetRepository.existsByIdentifierAndDeletedAtIsNull("PROJ-A1")).thenReturn(false);
        when(assetMapper.toEntity(request)).thenReturn(asset);
        when(assetRepository.save(any(SchedulableAsset.class))).thenReturn(asset);
        when(assetMapper.toDto(asset)).thenReturn(expectedDto);

        AssetDto result = assetService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Projector Unit A", result.getName());
        assertEquals("PROJ-A1", result.getIdentifier());
        assertEquals("PROJECTOR", result.getAssetType());
        assertEquals(1L, result.getOwningDepartmentId());
        verify(assetRepository).save(any(SchedulableAsset.class));
    }

    @Test
    void create_invalidDepartment_throwsEntityNotFound() {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Projector Unit A")
                .identifier("PROJ-A1")
                .assetType("PROJECTOR")
                .owningDepartmentId(99L)
                .campusId(1L)
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> assetService.create(request));

        assertTrue(exception.getMessage().contains("Department"));
        verify(assetRepository, never()).save(any());
    }

    @Test
    void create_duplicateIdentifier_throwsConflict() {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Projector Unit A")
                .identifier("PROJ-A1")
                .assetType("PROJECTOR")
                .owningDepartmentId(1L)
                .campusId(1L)
                .build();

        Department department = new Department();
        department.setId(1L);

        Campus campus = new Campus();
        campus.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(assetRepository.existsByIdentifierAndDeletedAtIsNull("PROJ-A1")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> assetService.create(request));

        assertTrue(exception.getMessage().contains("PROJ-A1"));
        verify(assetRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_validRequest_replacesWindows() {
        SchedulableAsset asset = new SchedulableAsset();
        asset.setId(1L);
        asset.setName("Projector Unit A");
        asset.setIdentifier("PROJ-A1");
        asset.setAssetType("PROJECTOR");
        asset.setIsActive(true);
        asset.setAvailabilityWindows(new ArrayList<>());

        Department department = new Department();
        department.setId(1L);
        department.setName("CS");
        asset.setOwningDepartment(department);

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main");
        asset.setCampus(campus);

        UpdateAssetRequest request = UpdateAssetRequest.builder()
                .name("Projector Unit A Updated")
                .assetType("PROJECTOR")
                .availabilityWindows(List.of(
                        CreateAvailabilityWindowRequest.builder()
                                .dayOfWeek("TUESDAY")
                                .startTime(LocalTime.of(9, 0))
                                .endTime(LocalTime.of(16, 0))
                                .build()
                ))
                .build();

        AssetDto expectedDto = AssetDto.builder()
                .id(1L)
                .name("Projector Unit A Updated")
                .build();

        when(assetRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(SchedulableAsset.class))).thenReturn(asset);
        when(assetMapper.toDto(asset)).thenReturn(expectedDto);

        AssetDto result = assetService.update(1L, request);

        assertNotNull(result);
        assertEquals(1, asset.getAvailabilityWindows().size());
        assertEquals("TUESDAY", asset.getAvailabilityWindows().get(0).getDayOfWeek());
        verify(assetRepository).save(any(SchedulableAsset.class));
    }

    // --- delete ---

    @Test
    void delete_noActiveBlocks_softDeletes() {
        SchedulableAsset asset = new SchedulableAsset();
        asset.setId(1L);
        asset.setIdentifier("PROJ-A1");
        asset.setIsActive(true);
        asset.setAvailabilityWindows(new ArrayList<>());

        when(assetRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(asset));

        assetService.delete(1L);

        assertNotNull(asset.getDeletedAt());
        assertFalse(asset.getIsActive());
        verify(assetRepository).save(asset);
    }

    @Test
    void delete_notFound_throwsEntityNotFoundException() {
        when(assetRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> assetService.delete(99L));

        verify(assetRepository, never()).save(any());
    }
}
