---
inclusion: fileMatch
fileMatchPattern: "**/*{controller,route,handler,endpoint,api,Controller}*"
---

# API Design Standards — UTMS Backend

## Base URL & Versioning
- Base path: `/api/v1/`
- Version in URL path (not headers): `/api/v1/campuses`, `/api/v2/...`
- All controllers annotated with `@RequestMapping("/api/v1/<resource>")`

## URL Conventions
- Use plural nouns for resources: `/campuses`, `/departments`, `/courses`, `/faculty`, `/rooms`, `/timetables`
- Use kebab-case for multi-word paths: `/time-slots`, `/exam-schedules`, `/approval-workflows`
- Nest related resources: `/departments/{deptId}/courses`, `/campuses/{campusId}/rooms`
- Use query parameters for filtering, sorting, pagination: `?page=0&size=20&sort=name,asc`
- Action endpoints use verbs as sub-resources: `/timetables/{id}/publish`, `/timetables/{id}/approve`

## UTMS Resource Naming

| Domain Entity | API Resource Path |
|---------------|-------------------|
| Campus | `/api/v1/campuses` |
| Department | `/api/v1/departments` |
| Program | `/api/v1/programs` |
| Course/Subject | `/api/v1/courses` |
| Faculty | `/api/v1/faculty` |
| Room/Lab | `/api/v1/rooms` |
| Batch/Section | `/api/v1/batches` |
| Academic Calendar | `/api/v1/academic-calendars` |
| Time Slot Grid | `/api/v1/time-slots` |
| Timetable | `/api/v1/timetables` |
| Scheduled Session | `/api/v1/timetables/{id}/sessions` |
| Conflict | `/api/v1/conflicts` |
| Approval | `/api/v1/approvals` |
| Faculty Availability | `/api/v1/faculty/{id}/availability` |
| Room Allocation | `/api/v1/rooms/{id}/allocations` |
| Workload | `/api/v1/faculty/{id}/workload` |

## HTTP Methods
- `GET` — retrieve resources (idempotent, cacheable)
- `POST` — create resources or trigger actions (e.g., `/timetables/generate`)
- `PUT` — full replacement of a resource
- `PATCH` — partial update (use for field-level edits like locking a session)
- `DELETE` — soft-delete (sets `deleted_at`, does not remove data)

## Request/Response Format

### Success Response
```json
{
  "data": {},
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Error Response
```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/courses",
  "details": [
    {
      "field": "creditHours",
      "message": "must be greater than 0",
      "rejectedValue": -1
    }
  ]
}
```

## Status Codes
| Code | Usage in UTMS |
|------|---------------|
| 200 | Success (GET, PUT, PATCH) |
| 201 | Created (POST new resource) |
| 204 | No Content (DELETE success) |
| 400 | Validation errors (invalid input, constraint violations) |
| 401 | Unauthorized (no/invalid JWT) |
| 403 | Forbidden (role insufficient — e.g., coordinator trying to approve) |
| 404 | Resource not found |
| 409 | Conflict (e.g., scheduling clash detected, concurrent modification) |
| 422 | Unprocessable (business rule violation — e.g., faculty exceeds max load) |
| 500 | Internal server error (never expose stack trace) |

## Pagination
- Use Spring Data `Pageable`: `?page=0&size=20&sort=name,asc`
- Default page size: 20, max: 100
- Response includes `meta` with `totalElements`, `totalPages`, `page`, `size`

## Filtering & Search
- Simple filters as query params: `?campusId=1&departmentId=3&type=LAB`
- Date ranges: `?startDate=2025-01-01&endDate=2025-06-30`
- Full-text search: `?search=computer+science`
- Use Spring Specification pattern for complex dynamic filters

## Validation
- All input validated at controller layer using Jakarta Validation annotations (`@NotNull`, `@Size`, `@Min`, `@Max`, `@Pattern`)
- Custom validators for domain rules (e.g., `@ValidLTP` for L-T-P structure, `@ValidSlotRange`)
- Use `@Valid` on `@RequestBody` parameters
- Allowlist-based validation (org security standard) — reject unexpected fields
- Never trust client-sent IDs for authorization; always verify ownership server-side

## Security at API Layer
- All endpoints require authentication (JWT in `Authorization: Bearer <token>` header)
- Role-based access enforced via `@PreAuthorize` annotations:
  ```java
  @PreAuthorize("hasRole('HOD') or hasRole('REGISTRAR')")
  @PostMapping("/{id}/approve")
  ```
- Input sanitization on all string fields (prevent XSS in stored data)
- Rate limiting on generation endpoints (`/timetables/generate`)
- CORS configured to allow only the frontend origin

## Documentation
- All endpoints documented via Springdoc OpenAPI annotations (`@Operation`, `@ApiResponse`, `@Schema`)
- Swagger UI available at `/swagger-ui.html` (dev/staging only, disabled in production)
- Every DTO annotated with `@Schema` describing field purpose and constraints
- Request/response examples provided via `@ExampleObject`

## Controller Implementation Pattern
```java
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course/Subject master data management")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "List courses with filtering and pagination")
    public ResponseEntity<PagedResponse<CourseDto>> list(
            @Valid CourseFilterRequest filter,
            Pageable pageable) {
        return ResponseEntity.ok(courseService.findAll(filter, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Create a new course")
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.create(request));
    }
}
```

## Naming Conventions (Code)
- Controllers: `<Entity>Controller.java`
- Request DTOs: `Create<Entity>Request`, `Update<Entity>Request`, `<Entity>FilterRequest`
- Response DTOs: `<Entity>Dto`, `<Entity>SummaryDto`
- Wrapper: `PagedResponse<T>` for paginated lists
