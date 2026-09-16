---
inclusion: fileMatch
fileMatchPattern: "**/*{repository,repo,dao,model,entity,migration,Repository,Entity}*"
---

# Data Access Standards — UTMS (Spring Data JPA / Postgres / Flyway)

## Repository Pattern

### Spring Data JPA Repositories
- Extend `JpaRepository<Entity, Long>` for standard CRUD
- Extend `JpaSpecificationExecutor<Entity>` for dynamic filtering
- Custom queries via `@Query` with JPQL (parameterized — never concatenate)
- Complex native queries only when JPQL is insufficient (annotate with `nativeQuery = true`)

### Naming
- Repository interface: `<Entity>Repository.java`
- Custom repository: `<Entity>RepositoryCustom` (interface) + `<Entity>RepositoryCustomImpl` (implementation)
- Query methods follow Spring Data naming: `findByDepartmentIdAndSemester(Long deptId, String semester)`

### Rules
- Repositories return entities or projections, never raw `Object[]`
- Never expose repository interfaces outside the module's service layer
- Use `Optional<Entity>` for single-result queries
- Always use parameterized queries — string concatenation in queries is forbidden (org security standard)

## Entity Design

### Base Entity
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

### Entity Conventions
- One entity per table, one file per entity
- Use `@Table(name = "table_name", schema = "utms")` explicitly
- Map relationships explicitly with `@ManyToOne`, `@OneToMany`, etc.
- Default fetch type: `LAZY` for all associations
- Use `@JoinColumn(name = "fk_column")` explicitly, never rely on defaults
- Override `equals()` and `hashCode()` using business key or `id` (not Lombok `@Data`)
- Use enums with `@Enumerated(EnumType.STRING)` — never ordinal

### UTMS Core Entities
| Entity | Table | Key Relationships |
|--------|-------|-------------------|
| Campus | `campuses` | has many departments, rooms |
| Department | `departments` | belongs to campus, has many programs, faculty |
| Program | `programs` | belongs to department, has many batches |
| Course | `courses` | belongs to department, has L-T-P structure |
| Faculty | `faculty` | belongs to department, has availability, workload |
| Room | `rooms` | belongs to campus, has capacity, type, equipment |
| Batch | `batches` | belongs to program, has strength |
| AcademicCalendar | `academic_calendars` | belongs to campus |
| TimeSlotGrid | `time_slot_grids` | belongs to campus |
| Timetable | `timetables` | belongs to department/semester, has sessions |
| ScheduledSession | `scheduled_sessions` | belongs to timetable, links course+faculty+room+slot |
| Conflict | `conflicts` | references sessions, has type and resolution |
| FacultyAvailability | `faculty_availability` | belongs to faculty, has slot + constraint type |
| ApprovalRecord | `approval_records` | belongs to timetable, has status + approver |

## Database Conventions (PostgreSQL)

### Naming
- Schema: `utms`
- Tables: plural, snake_case (`scheduled_sessions`, `faculty_availability`)
- Columns: snake_case (`department_id`, `credit_hours`, `max_weekly_load`)
- Primary keys: `id` (bigserial)
- Foreign keys: `<referenced_table_singular>_id` (e.g., `department_id`, `campus_id`)
- FK constraint names: `fk_<table>_<referenced_table>` (e.g., `fk_courses_departments`)
- Index names: `idx_<table>_<columns>` (e.g., `idx_faculty_department_id`)
- Unique constraints: `uq_<table>_<columns>`

### Standard Columns (every table)
```sql
id              BIGSERIAL PRIMARY KEY,
created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
created_by      VARCHAR(100) NOT NULL,
updated_by      VARCHAR(100) NOT NULL,
deleted_at      TIMESTAMP NULL  -- soft delete
```

### Indexing Strategy
- Index all foreign key columns
- Index columns used in WHERE clauses frequently (e.g., `semester`, `status`, `campus_id`)
- Composite indexes for common query patterns (e.g., `idx_sessions_timetable_day` on `(timetable_id, day_of_week)`)
- Partial indexes for active records: `WHERE deleted_at IS NULL`

## Flyway Migrations

### Naming
- Format: `V<version>__<description>.sql`
- Version: sequential integer (`V1`, `V2`, ...) or timestamp (`V20250115120000`)
- Description: snake_case, meaningful (`create_campuses_table`, `add_workload_columns_to_faculty`)

### Rules
- One logical change per migration file
- All migrations must be reversible (provide `ROLLBACK` comment or paired undo script)
- Never modify a migration that has been applied to any shared environment
- DDL and DML in separate migrations
- Include `IF NOT EXISTS` / `IF EXISTS` guards where appropriate
- Test migrations against production-scale data before deploying

### Example
```sql
-- V3__create_courses_table.sql
CREATE TABLE utms.courses (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    department_id   BIGINT NOT NULL REFERENCES utms.departments(id),
    credit_hours    INTEGER NOT NULL CHECK (credit_hours > 0),
    lecture_hours   INTEGER NOT NULL DEFAULT 0,
    tutorial_hours  INTEGER NOT NULL DEFAULT 0,
    practical_hours INTEGER NOT NULL DEFAULT 0,
    course_type     VARCHAR(20) NOT NULL DEFAULT 'CORE',  -- CORE, ELECTIVE, AUDIT
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT uq_courses_code UNIQUE (code),
    CONSTRAINT fk_courses_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id)
);

CREATE INDEX idx_courses_department_id ON utms.courses(department_id);
CREATE INDEX idx_courses_type ON utms.courses(course_type) WHERE deleted_at IS NULL;
```

## Query Performance
- Avoid N+1: use `@EntityGraph` or `JOIN FETCH` for associations needed in the response
- Use `@Query` with explicit joins over letting Hibernate generate queries for complex fetches
- Paginate all list queries — never return unbounded result sets
- Use `@QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "50"))` for large batch reads
- Log slow queries (> 500ms) via Postgres `log_min_duration_statement` and Hibernate statistics in dev

## Transactions
- Service methods that modify data are annotated with `@Transactional`
- Read-only methods: `@Transactional(readOnly = true)` (enables Hibernate optimizations)
- Keep transactions short — no external API calls inside a transaction
- Use optimistic locking (`@Version`) for entities with concurrent updates (e.g., timetable sessions)

## Test Data & Seeding
- Use Testcontainers (Postgres) for integration tests — no H2
- Test fixtures via builder pattern, not shared SQL scripts
- Seed data for local development in `src/main/resources/db/seed/` (separate from migrations)
- Never use production data in development or test environments
