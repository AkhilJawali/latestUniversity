---
inclusion: fileMatch
fileMatchPattern: "**/*.java"
---

# Backend Development Standards — UTMS (Java / Spring Boot)

## Package Structure
```
com.utms/
├── common/                    # Cross-cutting concerns
│   ├── config/                # Spring configuration classes
│   ├── exception/             # Global exception handler, custom exceptions
│   ├── security/              # JWT filter, SecurityConfig, UserDetails
│   ├── audit/                 # Auditing config (created_by, updated_by)
│   ├── dto/                   # Shared DTOs (PagedResponse, ErrorResponse)
│   └── util/                  # Utility classes
├── masterdata/                # Master Data module
│   ├── campus/
│   │   ├── CampusController.java
│   │   ├── CampusService.java
│   │   ├── CampusRepository.java
│   │   ├── Campus.java              # Entity
│   │   ├── CampusDto.java           # Response DTO
│   │   └── CreateCampusRequest.java # Request DTO
│   ├── department/
│   ├── program/
│   ├── course/
│   ├── faculty/
│   ├── room/
│   ├── batch/
│   └── academiccalendar/
├── scheduling/                # Scheduling Engine module
│   ├── engine/                # Constraint solver, generation logic
│   ├── timetable/             # Timetable CRUD, sessions
│   ├── conflict/              # Conflict detection and resolution
│   └── constraint/            # Hard/soft constraint definitions
├── workload/                  # Faculty Workload module
├── allocation/                # Room/Lab Allocation module
├── approval/                  # Approval Workflow module
├── notification/              # Notification module (Phase 2 placeholder)
└── reporting/                 # Reports and analytics module
```

## Layered Architecture
- **Controller** — HTTP concerns only: request parsing, validation trigger, response mapping, security annotations
- **Service** — Business logic, orchestration, transaction boundaries (`@Transactional`)
- **Repository** — Data access only (Spring Data JPA interfaces)
- **Entity** — JPA entity, DB mapping, no business logic
- **DTO** — Request/response objects, never expose entities directly

### Rules
- Controllers never call repositories directly
- Services never access `HttpServletRequest` or return `ResponseEntity`
- Entities never leave the service layer — always map to DTOs via MapStruct
- Cross-module communication goes through service interfaces, never repository-to-repository

## Coding Conventions

### Naming
- Classes: PascalCase (`CourseService`, `FacultyController`)
- Methods: camelCase, verb-first (`createCourse`, `findByDepartmentId`, `validateWorkload`)
- Constants: UPPER_SNAKE_CASE (`MAX_WEEKLY_HOURS`, `DEFAULT_PAGE_SIZE`)
- Packages: lowercase, singular (`course`, `faculty`, not `courses`, `faculties`)

### Lombok Usage
- Use `@RequiredArgsConstructor` for dependency injection (constructor injection)
- Use `@Getter` / `@Setter` on DTOs
- Use `@Builder` on DTOs and request objects
- Do NOT use `@Data` on JPA entities (broken equals/hashCode with lazy loading)
- Use `@Slf4j` for logging

### Dependency Injection
- Always use constructor injection (via `@RequiredArgsConstructor`)
- Never use `@Autowired` on fields
- Keep constructors clean — if > 5 dependencies, the class likely needs splitting

### MapStruct Mapper Rules
- Every mapper must reference `BaseMapperConfig`: `@Mapper(componentModel = "spring", config = BaseMapperConfig.class)`
- `BaseMapperConfig` uses `unmappedTargetPolicy = ReportingPolicy.IGNORE` to skip BaseEntity fields (id, createdAt, updatedAt, createdBy, updatedBy, deletedAt, isActive)
- Never create a mapper without the config — it will fail at compile time with "unmapped target properties" errors
- For `toEntity()` methods: always add `@Mapping(target = "<relationship>", ignore = true)` for parent FK associations (e.g., department, campus) since they are set manually in the service layer
- Location of BaseMapperConfig: `com.utms.common.mapper.BaseMapperConfig`

### JPA Specification (Generic Type) Rules
- When chaining Specification methods across static calls, always assign to a typed variable first:
  ```java
  // CORRECT:
  Specification<T> notDel = notDeleted();
  Specification<T> scope = byCampusId();
  return Specification.where(notDel).and(scope);

  // WRONG (causes type inference failure):
  return notDeleted().and(byCampusId());
  ```
- Use `Specification.where()` as the entry point for combining specifications
- Never return raw `Specification<Object>` — always ensure generic type `<T>` flows through

### Flyway Migration Rules (BaseEntity Alignment)
- **Every table** that maps to an entity extending `BaseEntity` must include ALL BaseEntity columns: `id`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`
- This applies to ALL tables — including junction tables, sub-resource tables, and config tables
- Before writing a migration, verify the column list against `BaseEntity.java` fields
- If Hibernate `ddl-auto: validate` fails with "missing column", a new ALTER migration is needed — never modify an already-applied migration

## Error Handling

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // MethodArgumentNotValidException → 400
    // EntityNotFoundException → 404
    // SchedulingConflictException → 409
    // BusinessRuleViolationException → 422
    // AccessDeniedException → 403
    // Exception → 500 (log full trace, return generic message)
}
```

### Custom Exceptions
- `EntityNotFoundException` — resource not found
- `SchedulingConflictException` — clash detected (faculty, room, batch)
- `BusinessRuleViolationException` — domain rule broken (workload exceeded, capacity exceeded)
- `ApprovalStateException` — invalid workflow transition

### Rules
- Never expose stack traces in responses (org security standard)
- Always log exceptions with context: entity type, ID, user, operation
- Use problem-specific exception classes, not generic `RuntimeException`

## Logging
- Framework: SLF4J + Logback
- Format: Structured JSON in deployed environments
- Include: `requestId`, `userId`, `operation`, `entityType`, `entityId`
- Levels:
  - `ERROR` — something failed that shouldn't have
  - `WARN` — recoverable issue, potential problem (e.g., near capacity limit)
  - `INFO` — significant business events (timetable generated, approval granted, session rescheduled)
  - `DEBUG` — detailed technical flow (disabled in production)
- Never log: passwords, tokens, PII, full request bodies containing sensitive data

## Testing

### Unit Tests (JUnit 5 + Mockito)
- Test service layer logic in isolation (mock repositories)
- Test validators, mappers, and utility classes
- Naming: `<MethodName>_<Scenario>_<ExpectedResult>`
  ```java
  @Test
  void validateWorkload_exceedsMaxHours_throwsBusinessRuleViolation() {}
  ```
- Minimum 80% line coverage on new code

### Integration Tests (Testcontainers + Spring Boot Test)
- Test API endpoints end-to-end with real Postgres container
- Test repository queries against actual DB
- Use `@Sql` or test fixtures for data setup
- Test security (unauthorized access, role enforcement)

### Test Data
- Use builder pattern for test fixtures
- Never rely on shared mutable test state
- Each test sets up its own data and cleans up (or uses `@Transactional` rollback)

## Configuration
- Use `application.yml` with Spring profiles: `local`, `dev`, `staging`, `prod`
- **Default active profile must be `local`**: Set `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}` in `application.yml` so the app always uses local config without needing a flag
- `application-local.yml` must point to the developer's local Postgres (localhost:5432)
- Secrets via environment variables (never in config files)
- Validate configuration at startup with `@ConfigurationProperties` + `@Validated`
- Provide sensible defaults for non-secret config

### Security Config (Local Dev)
- Exclude `UserDetailsServiceAutoConfiguration` in `@SpringBootApplication` to suppress the generated password warning
- Use a single clean `permitAll()` in `SecurityConfig` for local dev — no conflicting rules
- Add a `// TODO` comment marking where JWT filter will be added when the Auth module is built
- Never ship `permitAll()` to production — this is a dev-only configuration

### Local Dev Prerequisites
- The developer's local Postgres must have the target database already created (e.g., `CREATE DATABASE utms;`)
- PostgreSQL JDBC does NOT auto-create databases — if the database doesn't exist, the app must fail with a connection error
- Flyway creates the schema and tables inside the existing database
- No Docker Compose required if Postgres is installed locally

## Performance Guidelines
- Timetable generation endpoint: < 2 minutes for ~40 sections
- All read endpoints: < 500ms response time
- Use `@Cacheable` for frequently read, rarely changing master data
- Avoid N+1 queries — use `@EntityGraph` or `JOIN FETCH`
- Use pagination on all list endpoints (no unbounded queries)
- Profile with Spring Boot Actuator metrics in staging
