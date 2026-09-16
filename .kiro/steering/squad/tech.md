---
inclusion: always
---

# Technology Context — Timetable Management Squad

## Tech Stack
- **Language (Backend):** Java 17+
- **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security)
- **Database:** PostgreSQL 15+
- **Frontend:** React 18+ (use plane JSX, No Typescript)
- **Build (Backend):** Maven (single-module, expandable to multi-module)
- **Build (Frontend):** pnpm
- **API Style:** RESTful (OpenAPI 3.0 spec-first)
- **Authentication:** Spring Security with JWT (stateless sessions)
- **Migration:** Flyway (versioned SQL migrations)

## Frontend Language Rule

**The frontend uses plain JavaScript (JSX) — NOT TypeScript.**

- All frontend files use `.jsx` and `.js` extensions, never `.tsx` or `.ts`
- Use PropTypes for prop validation instead of TypeScript interfaces
- Use JSDoc comments for documentation where needed
- Zod schemas provide runtime validation (replacing compile-time type checks)
- This is a hard rule — no TypeScript in the frontend codebase

## Architecture Pattern
- **Modular Monolith** (Phase 1) — single deployable Spring Boot application with clear module boundaries
- Modules map to BRD functional areas: `master-data`, `scheduling-engine`, `timetable`, `exam`, `approval-workflow`, `notification`, `reporting`
- Each module has its own package namespace: `com.utms.<module>`
- Frontend is a separate SPA that communicates via REST APIs
- Future: modules can be extracted into microservices if scale demands

## Key Libraries

| Library | Purpose | Notes |
|---------|---------|-------|
| Spring Data JPA + Hibernate | ORM / data access | Parameterized queries only (org security standards) |
| Flyway | DB migrations | All migrations versioned and reversible |
| MapStruct | DTO ↔ Entity mapping | Compile-time, no reflection |
| Spring Validation (Jakarta) | Input validation | Allowlist-based, server-side |
| Spring Security + jjwt | Auth & authorization | RBAC aligned to BRD Section 4 roles |
| Lombok | Boilerplate reduction | Use judiciously — no `@Data` on entities |
| JUnit 5 + Mockito | Unit testing | 80% coverage target on new code |
| Testcontainers | Integration testing | Postgres container for DB tests |
| React Router | Client-side routing | v6+ |
| TanStack Query | Server state management | Cache, refetch, optimistic updates |
| Zustand | Client state management | Lightweight, no boilerplate |
| Zod | Runtime validation (frontend) | Schema-first form validation |
| DOMPurify | XSS sanitization (frontend) | Required by org security standards |
| FullCalendar or react-big-calendar | Timetable UI | Drag-and-drop schedule editing |

## Infrastructure (Phase 1)
- **Hosting:** Docker containers (Docker Compose for local dev; deployment target TBD)
- **CI/CD:** GitHub Actions (build → test → lint → security scan → deploy)
- **Monitoring:** Spring Boot Actuator + Prometheus + Grafana (planned)
- **Logging:** SLF4J + Logback → structured JSON logs
- **API Docs:** Springdoc OpenAPI (Swagger UI at `/swagger-ui.html`)

## Database Conventions
- Schema: `utms` (default schema for all application tables)
- Naming: `snake_case` for tables and columns
- Primary keys: `bigserial` (`id` column)
- Audit columns on every table: `created_at`, `updated_at`, `created_by`, `updated_by`
- Soft deletes where business requires audit trail: `deleted_at` column
- All foreign keys explicitly named: `fk_<table>_<referenced_table>`
- Indexes on all foreign keys and frequently filtered columns

## Development Environment
- **Java version:** 17+ (LTS)
- **Node version:** 20+ (LTS)
- **Package manager (frontend):** pnpm
- **IDE:** IntelliJ IDEA / VS Code + Kiro
- **Required extensions:** Lombok plugin, Spring Boot tools, ESLint, Prettier
- **Local DB:** Docker Compose with Postgres 15 container
- **API testing:** Bruno or Postman (collection committed to repo)

## Performance Targets (from BRD NFRs)
- Timetable generation for ~40 sections: < 2 minutes
- Conflict detection response: < 2 seconds
- Concurrent coordinators: 50+
- Scale: 10,000+ students, 500+ faculty, 200+ rooms
