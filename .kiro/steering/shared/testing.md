---
inclusion: always
---

# Testing Standards

## Test Pyramid

| Level | Tool | Purpose | Run When |
|-------|------|---------|----------|
| Unit Tests | JUnit 5 + Mockito | Isolated business logic (service layer) | Every build |
| Integration Tests | Testcontainers + REST Assured | API endpoints with real Postgres | CI pipeline |
| Performance Tests | Gatling | Load testing, response time SLAs | Before release / on-demand |
| Security Analysis | SpotBugs + FindSecBugs | Static security bug detection | Every build (verify phase) |
| Code Coverage | JaCoCo | Line/branch coverage reporting | Every build |

---

## Unit Tests (JUnit 5 + Mockito)

### Scope
- Test service layer logic in isolation (mock repositories)
- Test validators, mappers, and utility classes
- Test exception handling logic

### Naming Convention
- Test files: `<Class>Test.java` (e.g., `CampusServiceTest.java`)
- Test methods: `methodName_scenario_expectedResult`
  ```java
  @Test
  void create_duplicateCode_throwsConflictException() {}
  ```

### Rules
- Tests must be deterministic (no flaky tests)
- Tests must be independent (no shared mutable state)
- Use test fixtures and builders over raw data
- Mock external dependencies at service boundaries
- Prefer testing behavior over implementation details
- Minimum 80% line coverage on new code

### Run Command
```
mvn test
```

---

## Integration Tests (Testcontainers + REST Assured)

### Scope
- API endpoints end-to-end with real Postgres container
- Repository queries against actual DB
- Security (unauthorized access, role enforcement)
- Flyway migrations run correctly
- Full request/response validation

### Setup
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers
- REST Assured for fluent API assertions
- Each test class sets up its own data (or uses `@Transactional` rollback)

### Naming Convention
- Test files: `<Entity>IntegrationTest.java` (e.g., `CampusIntegrationTest.java`)
- Test methods: `endpoint_scenario_expectedStatusAndBody`
  ```java
  @Test
  void POST_campuses_validPayload_returns201WithCampusDto() {}
  ```

### Run Command
```
mvn verify -Pintegration-tests
```

---

## Performance Tests (Gatling)

### Scope
- Load testing for critical endpoints
- Response time SLA validation (< 200ms CRUD, < 500ms complex queries)
- Concurrent user simulation (50+ coordinators target)
- Throughput and error rate under load

### Location
- Gatling simulations: `src/test/gatling/` (separate from JUnit tests)
- Results: `target/gatling/` (HTML reports)

### Key Scenarios to Test
- CRUD operations under load (50 concurrent users)
- Hierarchy tree endpoint with large dataset
- Search/filter endpoints with pagination
- Concurrent writes to same entity (optimistic locking)

### Run Command
```
mvn gatling:test
```

---

## Security Analysis (SpotBugs + FindSecBugs)

### Scope
- Static analysis for common security vulnerabilities
- SQL injection patterns
- XSS vulnerabilities
- Insecure cryptography usage
- Path traversal risks
- Hardcoded credentials detection

### Configuration
- Effort: Max (thorough analysis)
- Threshold: Medium (report medium and above)
- Runs automatically on `mvn verify`

### Run Command
```
mvn spotbugs:check
```

To generate an HTML report:
```
mvn spotbugs:gui
```

---

## Code Coverage (JaCoCo)

### Targets
- Line coverage: 80%+ on new code
- Branch coverage: 70%+ on new code

### Run & View Report
```
mvn test jacoco:report
```
Report at: `target/site/jacoco/index.html`

---

## Principles
- Tests must be deterministic (no flaky tests)
- Tests must be independent (no shared mutable state)
- Use test fixtures and factories over raw data
- Mock external dependencies at integration boundaries
- Prefer testing behavior over implementation details
- Never rely on test execution order

## Coverage Requirements
- New code must include tests
- Critical paths require integration tests
- PRs that reduce coverage require justification

## Test Execution Summary

| Command | What It Runs |
|---------|-------------|
| `mvn test` | Unit tests only |
| `mvn verify` | Unit tests + SpotBugs security check |
| `mvn verify -Pintegration-tests` | Unit + Integration (Testcontainers) |
| `mvn gatling:test` | Performance/load tests |
| `mvn test jacoco:report` | Unit tests + coverage report |
| `mvn spotbugs:check` | Security static analysis |
