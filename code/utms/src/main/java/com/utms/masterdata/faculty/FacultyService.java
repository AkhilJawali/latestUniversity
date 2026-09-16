package com.utms.masterdata.faculty;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyService {

    private static final List<String> VALID_DESIGNATIONS = List.of(
            "Professor", "Associate Professor", "Assistant Professor",
            "Lecturer", "Senior Lecturer", "Lab Instructor", "Visiting Faculty"
    );

    private final FacultyRepository facultyRepository;
    private final FacultyMapper facultyMapper;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final FacultyCompetencyService facultyCompetencyService;
    private final FacultyCampusAssociationService facultyCampusAssociationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public FacultyDto create(CreateFacultyRequest request) {
        // Validate department exists
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getHomeDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department", request.getHomeDepartmentId()));

        // Validate identifier uniqueness (plain UNIQUE per KD-12 — even soft-deleted records block)
        if (facultyRepository.existsByIdentifier(request.getIdentifier())) {
            throw new ConflictException(
                    "Faculty with identifier '" + request.getIdentifier() + "' already exists");
        }

        // Validate designation against configurable list
        validateDesignation(request.getDesignation());

        // Validate load min <= max
        validateLoadOrder(request.getMinWeeklyLoad(), request.getMaxWeeklyLoad());

        // Validate campus IDs exist
        for (Long campusId : request.getCampusIds()) {
            if (!campusRepository.findByIdAndDeletedAtIsNull(campusId).isPresent()) {
                throw new EntityNotFoundException("Campus", campusId);
            }
        }

        Faculty faculty = facultyMapper.toEntity(request);
        faculty.setHomeDepartment(department);
        faculty.setIsActive(true);
        faculty = facultyRepository.save(faculty);

        // Create campus associations
        for (Long campusId : request.getCampusIds()) {
            facultyCampusAssociationService.addAssociation(faculty.getId(), campusId);
        }

        // Create competencies if provided
        if (request.getCompetencyCourseIds() != null && !request.getCompetencyCourseIds().isEmpty()) {
            facultyCompetencyService.addCompetencies(faculty.getId(), request.getCompetencyCourseIds());
        }

        log.info("Faculty created: id={}, identifier={}, departmentId={}",
                faculty.getId(), faculty.getIdentifier(), department.getId());
        return facultyMapper.toDto(faculty);
    }

    @Transactional(readOnly = true)
    public FacultyDto findById(Long id) {
        Faculty faculty = findActiveByIdOrThrow(id);
        return facultyMapper.toDto(faculty);
    }

    @Transactional(readOnly = true)
    public FacultyDto findByIdentifier(String identifier) {
        Faculty faculty = facultyRepository.findByIdentifierAndDeletedAtIsNull(identifier)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", "identifier", identifier));
        return facultyMapper.toDto(faculty);
    }

    @Transactional(readOnly = true)
    public Page<FacultyDto> findAll(Long departmentId, Long campusId, String designation,
                                    Long competencyCourseId, Pageable pageable) {
        Specification<Faculty> spec = notDeleted();

        if (departmentId != null) {
            Specification<Faculty> deptSpec = byDepartmentId(departmentId);
            spec = Specification.where(spec).and(deptSpec);
        }
        if (campusId != null) {
            Specification<Faculty> campusSpec = byCampusId(campusId);
            spec = Specification.where(spec).and(campusSpec);
        }
        if (designation != null && !designation.isBlank()) {
            Specification<Faculty> desigSpec = byDesignation(designation);
            spec = Specification.where(spec).and(desigSpec);
        }
        if (competencyCourseId != null) {
            Specification<Faculty> compSpec = byCompetencyCourseId(competencyCourseId);
            spec = Specification.where(spec).and(compSpec);
        }

        return facultyRepository.findAll(spec, pageable)
                .map(facultyMapper::toDto);
    }

    @Transactional
    public FacultyDto update(Long id, UpdateFacultyRequest request) {
        Faculty faculty = findActiveByIdOrThrow(id);

        // Validate designation
        validateDesignation(request.getDesignation());

        // Validate load min <= max
        validateLoadOrder(request.getMinWeeklyLoad(), request.getMaxWeeklyLoad());

        // Detect department transfer
        Long oldDepartmentId = faculty.getHomeDepartment().getId();
        boolean deptTransfer = !oldDepartmentId.equals(request.getHomeDepartmentId());
        if (deptTransfer) {
            Department newDepartment = departmentRepository.findByIdAndDeletedAtIsNull(request.getHomeDepartmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department", request.getHomeDepartmentId()));
            faculty.setHomeDepartment(newDepartment);
            log.info("Faculty department transfer: id={}, from={}, to={}",
                    faculty.getId(), oldDepartmentId, request.getHomeDepartmentId());
        }

        // Detect designation change and emit event
        boolean designationChanged = !faculty.getDesignation().equals(request.getDesignation());
        String previousDesignation = faculty.getDesignation();

        facultyMapper.updateEntity(request, faculty);
        faculty = facultyRepository.save(faculty);

        if (designationChanged) {
            applicationEventPublisher.publishEvent(new FacultyDesignationChangedEvent(
                    faculty.getId(), previousDesignation, request.getDesignation()));
            log.info("Faculty designation changed: id={}, from={}, to={}",
                    faculty.getId(), previousDesignation, request.getDesignation());
        }

        log.info("Faculty updated: id={}", faculty.getId());
        return facultyMapper.toDto(faculty);
    }

    @Transactional
    public void delete(Long id) {
        Faculty faculty = findActiveByIdOrThrow(id);

        // TODO: Check references (scheduled sessions, etc.) before allowing delete

        faculty.setDeletedAt(LocalDateTime.now());
        faculty.setIsActive(false);
        facultyRepository.save(faculty);

        log.info("Faculty soft-deleted: id={}, identifier={}", faculty.getId(), faculty.getIdentifier());
    }

    private Faculty findActiveByIdOrThrow(Long id) {
        return facultyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", id));
    }

    private void validateDesignation(String designation) {
        if (!VALID_DESIGNATIONS.contains(designation)) {
            throw new BusinessRuleViolationException(
                    "Invalid designation '" + designation + "'. Valid values: " + VALID_DESIGNATIONS);
        }
    }

    private void validateLoadOrder(java.math.BigDecimal minWeeklyLoad, java.math.BigDecimal maxWeeklyLoad) {
        if (minWeeklyLoad != null && maxWeeklyLoad != null
                && minWeeklyLoad.compareTo(maxWeeklyLoad) > 0) {
            throw new BusinessRuleViolationException(
                    "Minimum weekly load (" + minWeeklyLoad + ") cannot exceed maximum weekly load (" + maxWeeklyLoad + ")");
        }
    }

    // --- Specification helpers ---

    private static Specification<Faculty> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Faculty> byDepartmentId(Long departmentId) {
        return (root, query, cb) -> cb.equal(root.get("homeDepartment").get("id"), departmentId);
    }

    private static Specification<Faculty> byCampusId(Long campusId) {
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var subRoot = subquery.from(FacultyCampusAssociation.class);
            subquery.select(subRoot.get("faculty").get("id"))
                    .where(
                            cb.equal(subRoot.get("campus").get("id"), campusId),
                            cb.isNull(subRoot.get("deletedAt"))
                    );
            return root.get("id").in(subquery);
        };
    }

    private static Specification<Faculty> byDesignation(String designation) {
        return (root, query, cb) -> cb.equal(root.get("designation"), designation);
    }

    private static Specification<Faculty> byCompetencyCourseId(Long courseId) {
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var subRoot = subquery.from(FacultyCompetency.class);
            subquery.select(subRoot.get("faculty").get("id"))
                    .where(
                            cb.equal(subRoot.get("course").get("id"), courseId),
                            cb.isNull(subRoot.get("deletedAt"))
                    );
            return root.get("id").in(subquery);
        };
    }
}
