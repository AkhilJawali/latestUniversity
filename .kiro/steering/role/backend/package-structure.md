---
inclusion: fileMatch
fileMatchPattern: "**/*.java"
---

# Backend Package Structure — Layer-Based Architecture

This steering file defines the mandatory package structure for all Java/Spring Boot backend projects. This structure applies to all new projects and must be followed consistently.

---

## Request Flow

Every API request follows this linear flow:

```
API request
    ↓
Controller
    ↓
Request DTO
    ↓
Validation
    ↓
Service
    ↓
Business Logic
    ↓
Helper / Utility / Domain Service (only if required)
    ↓
Repository
    ↓
Database
```

**Rules:**
- Controllers never skip layers (no direct repository calls)
- Services never access HTTP context (no `HttpServletRequest`, no `ResponseEntity`)
- Entities never leave the service layer — always map to DTOs
- Helpers/Utilities are optional — only create when genuinely needed

---

## Package Structure

All code is consolidated under `com.utms` (or the appropriate base package for the project). Each layer is a top-level package:

```
com.utms/
│
├── controller/                 # REST Controllers — HTTP layer only
│   ├── UserController.java
│   ├── OrderController.java
│   └── MeetingController.java
│
├── dto/                        # Data Transfer Objects
│   ├── request/                # Request DTOs (input)
│   │   ├── UserRequest.java
│   │   ├── OrderRequest.java
│   │   └── MeetingRequest.java
│   │
│   ├── response/               # Response DTOs (output)
│   │   ├── UserResponse.java
│   │   ├── OrderResponse.java
│   │   └── MeetingResponse.java
│   │
│   └── mapper/                 # MapStruct mappers (with corresponding DTOs)
│       ├── UserMapper.java
│       ├── OrderMapper.java
│       └── MeetingMapper.java
│
├── entity/                     # JPA Entities — database mapping
│   ├── User.java
│   ├── Order.java
│   └── Meeting.java
│
├── repository/                 # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── OrderRepository.java
│   └── MeetingRepository.java
│
├── service/                    # Business logic layer
│   ├── UserService.java
│   ├── OrderService.java
│   ├── MeetingService.java
│   │
│   └── helper/                 # Domain helpers (optional, at same level as service)
│       ├── OrderCalculationHelper.java
│       └── MeetingScheduleHelper.java
│
├── validator/                  # Input validators (separate package)
│   ├── UserValidator.java
│   ├── OrderValidator.java
│   └── MeetingValidator.java
│
├── exception/                  # Exception handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BusinessException.java
│   └── ErrorResponse.java
│
├── client/                     # External API clients (Feign, RestTemplate, WebClient)
│   ├── PaymentClient.java
│   ├── NotificationClient.java
│   └── ExternalApiClient.java
│
├── config/                     # Spring configuration classes
│   ├── SecurityConfig.java
│   ├── DatabaseConfig.java
│   └── OpenApiConfig.java
│
├── security/                   # Authentication & authorization
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── SecurityService.java
│
├── util/                       # Utility classes (stateless helpers)
│   ├── DateUtil.java
│   ├── JsonUtil.java
│   └── StringUtil.java
│
├── constant/                   # Application constants
│   ├── AppConstants.java
│   ├── ErrorConstants.java
│   └── StatusConstants.java
│
├── enums/                      # Enumerations
│   ├── OrderStatus.java
│   ├── UserStatus.java
│   └── MeetingStatus.java
│
└── common/                     # Shared cross-cutting concerns
    ├── config/                 # Shared configuration
    ├── exception/              # Shared exceptions
    ├── security/               # Shared security components
    ├── audit/                  # Auditing configuration
    ├── dto/                    # Shared DTOs (PagedResponse, ErrorResponse)
    └── util/                   # Shared utilities
```

---

## Layer Responsibilities

### Controller
- HTTP layer only — receive requests, return responses
- Parse request body, path variables, query parameters
- Trigger validation (via `@Valid` annotations)
- Call service layer methods
- Map service results to response DTOs
- Apply security annotations (`@PreAuthorize`, `@Secured`)
- **Never:** call repositories directly, contain business logic, access HTTP context in services

### DTO (Data Transfer Object)
- **Request DTOs:** Input validation annotations, builder pattern, immutable
- **Response DTOs:** Output shape, never expose entities
- **Mappers:** MapStruct interfaces for Entity ↔ DTO conversion
- **Location:** Mappers go in `dto/mapper/` with the DTOs they map
- **Naming:** Generic naming (`UserRequest`, not `CreateUserRequest` or `UpdateUserRequest`)

### Entity
- JPA entity classes — database table mapping
- No business logic
- Extend `BaseEntity` for audit fields
- Use Lombok `@Getter`, `@Setter` (not `@Data`)
- Relationships defined here, but managed in service layer

### Repository
- Spring Data JPA interfaces only
- Custom queries use `@Query` with parameterized queries
- Specifications for dynamic queries
- No business logic

### Service
- Business logic and orchestration
- Transaction boundaries (`@Transactional`)
- Call repositories, validators, helpers, other services
- Map entities to DTOs before returning
- **Never:** access `HttpServletRequest`, return `ResponseEntity`

### Helper / Utility
- **Helpers:** Domain-specific logic, stateless, called from services (located in `service/helper/`)
- **Utilities:** Generic reusable functions (located in `util/`)
- Only create when genuinely needed — not every service needs a helper

### Validator
- Input validation logic beyond bean validation
- Called from controller or service layer
- Separate package — not embedded in service
- Use Spring Validation annotations on DTOs for basic validation

### Exception
- Global exception handler (`@RestControllerAdvice`)
- Custom exception classes for specific error cases
- Never expose stack traces in responses

### Client
- External API clients (Feign, RestTemplate, WebClient)
- Circuit breaker, retry, timeout configuration
- Centralize external system integration

### Config
- Spring configuration classes
- Security, database, caching, OpenAPI, etc.

### Security
- JWT handling, filters, authentication/authorization logic
- User details, security context

### Common
- Shared components used across the entire application
- Cross-cutting concerns (auditing, logging, shared DTOs)
- Base classes and configurations

---

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Controller | `<Entity>Controller` | `UserController` |
| Service | `<Entity>Service` | `UserService` |
| Repository | `<Entity>Repository` | `UserRepository` |
| Entity | `<Entity>` | `User` |
| Request DTO | `<Entity>Request` | `UserRequest` |
| Response DTO | `<Entity>Response` | `UserResponse` |
| Mapper | `<Entity>Mapper` | `UserMapper` |
| Validator | `<Entity>Validator` | `UserValidator` |
| Helper | `<Entity><Purpose>Helper` | `OrderCalculationHelper` |
| Client | `<Purpose>Client` | `PaymentClient` |

---

## Dependency Injection

- Always use constructor injection (via `@RequiredArgsConstructor`)
- Never use `@Autowired` on fields
- If a class has > 5 dependencies, consider splitting it

---

## MapStruct Rules

- Every mapper must reference `BaseMapperConfig`: `@Mapper(componentModel = "spring", config = BaseMapperConfig.class)`
- Location: `dto/mapper/` package
- For `toEntity()` methods: always add `@Mapping(target = "<relationship>", ignore = true)` for parent FK associations

---

## Validation Rules

- Use Jakarta Bean Validation annotations on request DTOs for basic validation
- Complex validation logic goes in separate `validator/` classes
- Validate early — reject malformed input at the controller layer

---

## Module Boundaries

For modular monolith architecture, all layers are consolidated at the `com.utms` level. There are NO module-specific packages (like `com.utms.masterdata`, `com.utms.scheduling`). Instead:

- All controllers go in `controller/`
- All services go in `service/`
- All entities go in `entity/`
- etc.

This applies even for large projects with multiple functional areas.

---

## Rules

1. **No domain-based packages** — all code is organized by layer, not by feature
2. **Consolidated structure** — one `controller/`, one `service/`, one `repository/` for the entire application
3. **DTOs separate from entities** — never expose entities in API responses
4. **Validators in their own package** — not embedded in service or controller
5. **Mappers with DTOs** — MapStruct interfaces go in `dto/mapper/`
6. **Helpers optional** — only create `service/helper/` when genuinely needed
7. **Common for shared code** — cross-cutting concerns go in `common/`
8. **Generic DTO naming** — `UserRequest`, not `CreateUserRequest`

---

## Enforcement

This structure is mandatory for all new backend projects. Any deviation must be explicitly approved by the tech lead. Kiro must refuse to generate code that violates this structure.
