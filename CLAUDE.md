# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RappiDrive** is a white-label ride-hailing platform (MVP) built in **Java 21 + Spring Boot 3.4.5** following strict **Hexagonal Architecture** (Ports & Adapters). Multi-tenancy is a first-class concern — every aggregate root carries a `TenantId`.

---

## Commands

```bash
# Full build with all tests
mvn clean install

# Run application (dev profile)
mvn spring-boot:run

# Unit tests only (classes matching **/*Test.java via Surefire)
mvn test

# Unit + integration + E2E tests (classes matching **/*IT.java via Failsafe)
mvn verify

# Package JAR, skip tests
mvn package -DskipTests

# Run a single test class
mvn test -Dtest=CreateTripUseCaseTest

# Run a single integration test class
mvn failsafe:integration-test -Dit.test=TripRepositoryIT
```

**Prerequisites:**
- Java 21 (LTS) — required for virtual threads
- Maven 3.8+ (Compiler args: `-parameters` required for record mapping)
- Docker — required for `mvn verify` (Testcontainers spins up PostgreSQL + Keycloak)
- `docker-compose up -d` starts PostgreSQL 16 + PostGIS at `localhost:5432` and pgAdmin at `localhost:5050`

---

## Architecture: Layer Map

```
com.rappidrive/
├── domain/
│   ├── entities/     ← Aggregate roots (Driver, Trip, Passenger, Vehicle, Payment, Rating, Notification, Tenant, DriverApproval, Fare)
│   ├── valueobjects/ ← Immutable value types (all must be final)
│   ├── services/     ← Pure domain logic (FareCalculator, CancellationPolicyService, TripCompletionService, RatingValidationService)
│   ├── events/       ← Domain event types + DomainEventsCollector + DomainEventPublisher
│   ├── outbox/       ← OutboxEvent (persisted for async dispatch)
│   └── exceptions/   ← DomainException hierarchy
├── application/
│   ├── ports/input/  ← InputPort interfaces with inner Command records, organized by domain subdirectory
│   ├── ports/output/ ← RepositoryPort and service port interfaces
│   ├── usecases/     ← Plain-Java use case implementations, organized by domain subdirectory
│   ├── concurrency/  ← ParallelExecutor
│   └── metrics/      ← Metrics port interfaces (e.g., DriverAssignmentMetricsPort)
├── infrastructure/   ← Spring, JPA, Keycloak, Micrometer, messaging
│   ├── config/       ← All @Configuration classes. UseCaseConfiguration.java wires all use-case beans.
│   │                    BeanConfiguration.java is currently a stub — do NOT add beans there.
│   ├── persistence/
│   │   ├── adapters/ ← JPA repository adapters (implement output ports, annotated @Component)
│   │   ├── entities/ ← JPA @Entity classes
│   │   ├── mappers/  ← MapStruct JPA↔Domain mappers
│   │   ├── converters/ ← AttributeConverter for value objects
│   │   └── fare/     ← Fare-specific persistence (adapter, mapper, Spring Data repo)
│   ├── adapters/     ← External service adapters (Keycloak provisioning)
│   ├── security/     ← SecurityConfiguration, JWT converter, CurrentUser adapter
│   ├── messaging/    ← OutboxEventProcessor (async dispatch)
│   ├── observability/← Micrometer telemetry adapters
│   └── web/filters/  ← TenantResolverFilter
└── presentation/
    ├── controllers/        ← Standard REST controllers (one per domain)
    ├── controllers/admin/  ← Admin-only endpoints (SuperAdminController, FareConfigurationController)
    ├── dto/                ← Request/Response DTOs (organized by request/, response/, approval/, common/)
    ├── mappers/            ← Domain↔DTO MapStruct mappers
    └── exception/          ← GlobalExceptionHandler → ErrorResponse
```

**Dependency direction is inward only.** Infrastructure → Application → Domain. Domain knows nothing outside itself. This is enforced by 24 ArchUnit rules in `HexagonalArchitectureTest` — violations fail the build.

---

## Non-Negotiable Architecture Rules

These are enforced at build time by ArchUnit. Breaking them causes test failures.

| Rule | Detail |
|---|---|
| Domain purity | No Spring, JPA (`jakarta.persistence`), or Jackson imports anywhere in `domain/` |
| No `@Entity` in domain | JPA entities live exclusively in `infrastructure.persistence.entities` |
| No `@Service`/`@Component` in domain | Domain services and use cases are plain Java, wired via `@Configuration` |
| Value objects must be `final` | All classes in `domain.valueobjects` must be `final` |
| Ports must be interfaces | All classes in `application.ports.*` ending with `Port` or `InputPort` must be interfaces |
| Use cases implement input ports | Every `*UseCase` must implement its corresponding `*InputPort` |
| Repository adapters implement output ports | Every `*Adapter` in `infrastructure.persistence.adapters` must implement a `*RepositoryPort` |
| Exceptions in correct hierarchy | `domain.exceptions.*` extends `DomainException`; `application.exceptions.*` extends `ApplicationException` |
| DTOs stay in presentation | Top-level `*Request` and `*Response` classes belong only in `presentation.dto` |
| Controllers stay in presentation | `@RestController` classes belong only in `presentation.controllers` |
| `@Configuration` stays in infrastructure | All `@Configuration` classes belong in `infrastructure.config` |

---

## Key Patterns

### Adding a Feature (ordered workflow)

1. **Domain** — entity or value object in `domain/entities/` or `domain/valueobjects/`. No annotations, no setters, behavior-rich.
2. **Output port** — interface in `application/ports/output/` (e.g., `OrderRepositoryPort`). Works only with domain types.
3. **Input port** — interface + inner `Command` record in `application/ports/input/{domain}/` (e.g., `CreateOrderInputPort`).
4. **Use case** — plain Java class in `application/usecases/{domain}/`, implementing the input port, constructor-injected output ports.
5. **Adapter** — JPA adapter in `infrastructure/persistence/adapters/` or external adapter in `infrastructure/adapters/`. Annotated `@Component`.
6. **Bean wiring** — declare use case `@Bean` in `infrastructure/config/UseCaseConfiguration.java`. Adapters are auto-detected via `@Component`.
7. **Presentation** — DTO in `presentation/dto/`, mapper in `presentation/mappers/`, controller in `presentation/controllers/`.

### Naming Conventions

| Artifact | Convention | Example |
|---|---|---|
| Use case | `{Action}{Entity}UseCase` | `CreateTripUseCase` |
| Input port | `{Action}{Entity}InputPort` | `CreateTripInputPort` |
| Output port | `{Entity}RepositoryPort` / `{Service}Port` | `TripRepositoryPort`, `PaymentGatewayPort` |
| Adapter | `Jpa{Entity}RepositoryAdapter` / `{Impl}{Adapter}` | `JpaDriverRepositoryAdapter`, `KeycloakProvisioningAdapter` |
| JPA entity | `{Entity}JpaEntity` | `DriverJpaEntity` |
| DTO | `{Entity}{Action}Request` / `{Entity}Response` | `CreateTripRequest`, `TripResponse` |

### Persistence & Hibernate 6.6+

- **Manual UUIDs**: Entities that assign IDs in the domain layer MUST NOT use `@GeneratedValue` in their JPA counterparts. Hibernate 6.6+ will throw `StaleObjectStateException` if it detects a manually assigned ID on a "new" entity with a generation strategy.
- **Flyway 10**: Uses modular database drivers. Ensure `flyway-database-postgresql` is in `pom.xml`.

### Security Configuration

Security is unified in `SecurityConfiguration.java`. Behavior is toggled via properties, NOT multiple conflicting `SecurityFilterChain` beans:
- `rappidrive.security.enabled`: Set to `false` in `dev` to disable security.
- `rappidrive.security.test-mode`: Set to `true` in `test`/`e2e` to bypass auth in automated tests.
- **NEVER** create separate `TestSecurityConfiguration` classes; they cause bean conflicts in Spring Boot 3.4+.

### Domain Events & Outbox Pattern

There are two event mechanisms — do not confuse them:

| Mechanism | Class | When to use |
|---|---|---|
| Synchronous in-process | `DomainEventPublisher` (ThreadLocal, observer pattern) | Immediate reactions within the same request |
| Async reliable dispatch | `DomainEventsCollector` → Outbox → `OutboxEventProcessor` | Cross-aggregate side-effects that must survive failures |

For the Outbox path: domain entities emit events via `DomainEventsCollector.collect(event)`. Events are persisted to `outbox_event` table in the same transaction, then `OutboxEventProcessor` dispatches them asynchronously (every 1 second, batch of 50, up to 5 retries). Key classes: `DomainEvent`, `OutboxPublisher`, `EventDispatcherPort`, `OutboxEventProcessor`.

### Driver Approval Workflow

New drivers go through a multi-step approval process: `DriverApproval` is a separate domain entity (not part of `Driver`) with its own lifecycle. Use cases live in `application/usecases/approval/`. Admin endpoints are in `presentation/controllers/ApprovalController.java`. The workflow emits `DriverApprovalSubmittedEvent`, `DriverApprovedEvent`, and `DriverRejectedEvent`.

### Parallel Execution

`ParallelExecutor` (in `application/concurrency/`) wraps `CompletableFuture` for parallel I/O. Used in `FindAvailableDriversUseCase` to query 4 geographic zones concurrently. Methods: `executeAll()`, `executeRace()`, `mapParallel()`. Works transparently with virtual threads.

### Virtual Threads

Enabled globally via `spring.threads.virtual.enabled=true`. All HTTP requests and `@Async` methods use virtual threads automatically. No special code required.

### PostGIS Spatial Queries

Driver geolocation uses `ST_DWithin()` with KNN (`<->`) operator and a GIST index (`idx_drivers_location_gist`). Queries over 10K drivers complete in under 50ms. Slow queries (>100ms) are logged as warnings by the adapter. Location is stored as `Location` value object (lat/lon).

### Multi-Tenancy

`TenantId` appears in every aggregate: `Driver`, `Passenger`, `Trip`, `Vehicle`, `Payment`, `Rating`, `Notification`, `Tenant`. Tenant is extracted from the JWT `tenant_id` claim by `TenantResolverFilter` and stored in `TenantContext` (ThreadLocal). JPA enforces isolation at query level via Hibernate `@Filter(condition = "tenant_id = :tenantId")`. Never query without tenant scope.

### Mapping Boundaries

- **JPA ↔ Domain**: MapStruct mappers in `infrastructure/persistence/mappers/` (e.g., `DriverMapper`)
- **Domain ↔ DTO**: Mappers in `presentation/mappers/` (e.g., `TripDtoMapper`)
- Value objects are bridged via JPA converters in `infrastructure/persistence/converters/` (`CPFConverter`, `EmailConverter`, `TenantIdConverter`, etc.)

### Exception Handling

| Type | Base class | HTTP mapping |
|---|---|---|
| Business rule violation | `DomainException` | 400 |
| Entity already exists | `EntityAlreadyExistsException` | 409 |
| Entity not found | `DomainException` subtype | 404 |
| Application logic error | `ApplicationException` | 400 |
| Infrastructure error | Wrapped at adapter boundary | 500 |

`GlobalExceptionHandler` (`presentation/exception/`) maps all exceptions to `ErrorResponse` DTOs. Never let JPA or HTTP exceptions cross adapter boundaries.

### Validation

- **Input validation**: Bean Validation annotations on DTO classes (`presentation/dto/`)
- **Business invariants**: Enforced in domain entity constructors and methods (throw `DomainException` subtypes)
- **Value object validation**: Always in the constructor (e.g., `new Email("invalid")` throws immediately)

---

## Working in This Codebase

### Scope of Changes

Touch only what the task requires. In a hexagonal project with 24 ArchUnit rules enforced at build time, editing adjacent code — even to "clean it up" — risks introducing layer violations that break the build.

- Don't refactor code outside the scope of the task.
- If you notice unrelated issues (dead code, style inconsistencies), mention them — don't fix them.
- Remove only the imports, variables, or methods that **your** changes made unused.
- Match the existing style of the layer you're working in, even if you'd do it differently.

### Avoid Over-Engineering Ports

The hexagonal pattern makes it tempting to create ports and use cases for everything. Resist this:

- A simple read-only lookup doesn't always need its own `InputPort` + `UseCase` — evaluate whether it belongs in an existing use case or domain service.
- Don't create output ports for behavior that can live entirely in the domain layer.
- Don't add "future-proof" constructor parameters, configuration flags, or optional ports that no current use case needs.

### Definition of Done

A change is complete when:
1. `mvn test` passes — unit tests and ArchUnit architecture constraints green.
2. For persistence or adapter changes: `mvn verify` passes (Testcontainers integration tests).
3. No new layer violations introduced — confirm by checking `HexagonalArchitectureTest` output.

For multi-step tasks, state a brief plan with a concrete verify step for each stage before starting.

---

## Testing

| Layer | Location | Tooling |
|---|---|---|
| Domain & use case unit tests | `src/test/.../domain/`, `.../application/` | JUnit 5 + Mockito (mock output ports) |
| Infrastructure integration tests | `src/test/.../infrastructure/` | Testcontainers (real PostgreSQL + PostGIS) |
| E2E / API tests | `src/test/.../e2e/` | `@SpringBootTest` + REST Assured |
| Architecture tests | `src/test/.../architecture/HexagonalArchitectureTest.java` | ArchUnit (24 rules) |

- Test files ending in `*Test.java` → run via Surefire (`mvn test`)
- Test files ending in `*IT.java` → run via Failsafe (`mvn verify`), require Docker
- Mutual exclusivity of security modes is guaranteed by `SecurityConfiguration` logic.

---

## Technology Stack

| Concern | Technology |
|---|---|
| Language / Runtime | Java 21, virtual threads |
| Framework | Spring Boot 3.4.5, Spring Cloud 2024.0.0 |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| ORM / Migrations | Hibernate 6.6 + Flyway 10 (`src/main/resources/db/migration/`) |
| Auth | Keycloak 23 (OAuth2/OIDC resource server) |
| Mapping | MapStruct 1.6.2 |
| Boilerplate | Lombok 1.18.34 (infrastructure/presentation only) |
| Caching | Caffeine (`CacheConfiguration.java`) |
| Resilience | Resilience4j Circuit Breaker (payment gateway) |
| Metrics | Micrometer → Prometheus |
| Tracing | OpenTelemetry → Zipkin |
| Docs | SpringDoc OpenAPI 2.8.1 (`/swagger-ui.html`) |
