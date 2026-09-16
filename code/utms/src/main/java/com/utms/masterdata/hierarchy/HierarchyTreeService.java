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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyTreeService {

    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final BatchRepository batchRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<CampusTreeDto> getFullTree() {
        Specification<Campus> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        List<Campus> campuses = campusRepository.findAll(spec);
        return campuses.stream()
                .map(this::buildCampusTree)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "hierarchyTree", key = "#campusId")
    public CampusTreeDto getCampusTree(Long campusId) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));
        return buildCampusTree(campus);
    }

    @Transactional(readOnly = true)
    public DepartmentTreeDto getDepartmentTree(Long departmentId) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department", departmentId));
        return buildDepartmentTree(department);
    }

    @Transactional(readOnly = true)
    public ProgramTreeDto getProgramTree(Long programId) {
        Program program = programRepository.findByIdAndDeletedAtIsNull(programId)
                .orElseThrow(() -> new EntityNotFoundException("Program", programId));
        return buildProgramTree(program);
    }

    @Transactional(readOnly = true)
    public BatchTreeDto getBatchTree(Long batchId) {
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch", batchId));
        return buildBatchTree(batch);
    }

    private CampusTreeDto buildCampusTree(Campus campus) {
        Specification<Department> deptSpec = (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.equal(root.get("campus").get("id"), campus.getId())
        );
        List<Department> departments = departmentRepository.findAll(deptSpec);

        List<DepartmentTreeDto> departmentTrees = departments.stream()
                .map(this::buildDepartmentTree)
                .toList();

        return CampusTreeDto.builder()
                .id(campus.getId())
                .name(campus.getName())
                .code(campus.getCode())
                .departments(departmentTrees)
                .build();
    }

    private DepartmentTreeDto buildDepartmentTree(Department department) {
        Specification<Program> progSpec = (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.equal(root.get("department").get("id"), department.getId())
        );
        List<Program> programs = programRepository.findAll(progSpec);

        List<ProgramTreeDto> programTrees = programs.stream()
                .map(this::buildProgramTree)
                .toList();

        return DepartmentTreeDto.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .programs(programTrees)
                .build();
    }

    private ProgramTreeDto buildProgramTree(Program program) {
        Specification<Batch> batchSpec = (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.equal(root.get("program").get("id"), program.getId())
        );
        List<Batch> batches = batchRepository.findAll(batchSpec);

        List<BatchTreeDto> batchTrees = batches.stream()
                .map(this::buildBatchTree)
                .toList();

        return ProgramTreeDto.builder()
                .id(program.getId())
                .name(program.getName())
                .code(program.getCode())
                .batches(batchTrees)
                .build();
    }

    private BatchTreeDto buildBatchTree(Batch batch) {
        List<Section> sections = sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batch.getId());

        List<SectionTreeDto> sectionTrees = sections.stream()
                .map(section -> SectionTreeDto.builder()
                        .id(section.getId())
                        .sectionIdentifier(section.getSectionIdentifier())
                        .subStrength(section.getSubStrength())
                        .build())
                .toList();

        return BatchTreeDto.builder()
                .id(batch.getId())
                .yearIdentifier(batch.getYearIdentifier())
                .strength(batch.getStrength())
                .sections(sectionTrees)
                .build();
    }
}
