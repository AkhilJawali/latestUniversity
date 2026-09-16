package com.utms.masterdata.hierarchy;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.section.Section;
import com.utms.masterdata.section.SectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HierarchyTreeService}, covering acceptance criterion 6 of the
 * Campus Hierarchy Master Data Management story (A4-2): the full hierarchy
 * (Campus -> Department -> Program -> Batch -> Section) must be traversable.
 */
@ExtendWith(MockitoExtension.class)
class HierarchyTreeServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private HierarchyTreeService hierarchyTreeService;

    private Campus buildCampus() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");
        return campus;
    }

    private Department buildDepartment() {
        Department department = new Department();
        department.setId(10L);
        department.setName("Computer Science");
        department.setCode("CS");
        return department;
    }

    private Program buildProgram() {
        Program program = new Program();
        program.setId(100L);
        program.setName("B.Tech CS");
        program.setCode("BTCS");
        return program;
    }

    private Batch buildBatch() {
        Batch batch = new Batch();
        batch.setId(1000L);
        batch.setYearIdentifier("2024-25");
        batch.setStrength(60);
        return batch;
    }

    private Section buildSection() {
        Section section = new Section();
        section.setId(10000L);
        section.setSectionIdentifier("A");
        section.setSubStrength(30);
        return section;
    }

    // --- getFullTree ---

    @Test
    @SuppressWarnings("unchecked")
    void getFullTree_populatedHierarchy_returnsFullyTraversableTree() {
        when(campusRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(buildCampus()));
        when(departmentRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(buildDepartment()));
        when(programRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(buildProgram()));
        when(batchRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(buildBatch()));
        when(sectionRepository.findAllByBatchIdAndDeletedAtIsNull(1000L))
                .thenReturn(List.of(buildSection()));

        List<CampusTreeDto> tree = hierarchyTreeService.getFullTree();

        assertEquals(1, tree.size());
        CampusTreeDto campus = tree.get(0);
        assertEquals("MAIN", campus.getCode());
        assertEquals(1, campus.getDepartments().size());

        DepartmentTreeDto department = campus.getDepartments().get(0);
        assertEquals("CS", department.getCode());
        assertEquals(1, department.getPrograms().size());

        ProgramTreeDto program = department.getPrograms().get(0);
        assertEquals("BTCS", program.getCode());
        assertEquals(1, program.getBatches().size());

        BatchTreeDto batch = program.getBatches().get(0);
        assertEquals("2024-25", batch.getYearIdentifier());
        assertEquals(1, batch.getSections().size());

        SectionTreeDto section = batch.getSections().get(0);
        assertEquals("A", section.getSectionIdentifier());
        assertEquals(30, section.getSubStrength());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFullTree_noCampuses_returnsEmptyList() {
        when(campusRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());

        List<CampusTreeDto> tree = hierarchyTreeService.getFullTree();

        assertTrue(tree.isEmpty());
    }

    // --- getCampusTree ---

    @Test
    @SuppressWarnings("unchecked")
    void getCampusTree_exists_returnsTreeForCampus() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(buildCampus()));
        when(departmentRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(buildDepartment()));
        when(programRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());

        CampusTreeDto campus = hierarchyTreeService.getCampusTree(1L);

        assertEquals(1L, campus.getId());
        assertEquals("MAIN", campus.getCode());
        assertEquals(1, campus.getDepartments().size());
        assertEquals("CS", campus.getDepartments().get(0).getCode());
    }

    @Test
    void getCampusTree_notFound_throwsEntityNotFoundException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> hierarchyTreeService.getCampusTree(99L));

        assertTrue(exception.getMessage().contains("Campus"));
        assertTrue(exception.getMessage().contains("99"));
    }

    // --- getDepartmentTree ---

    @Test
    void getDepartmentTree_notFound_throwsEntityNotFoundException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> hierarchyTreeService.getDepartmentTree(99L));

        assertTrue(exception.getMessage().contains("Department"));
    }
}
