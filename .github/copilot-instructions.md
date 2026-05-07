# Copilot Instructions - Hexagonal Backend (Java)

## Project Context

**RappiDrive** is a white-label ride-hailing platform (Uber-like business model) MVP built with **Hexagonal Architecture** (Ports & Adapters).

**Current State (as of May 2026):**
- ✅ Full hexagonal architecture scaffold complete
- ✅ Core entities implemented: `Driver`, `Passenger`, `Trip`, `Vehicle`, `Payment`, `Rating`, `Notification`
- ✅ 20+ use cases across driver, trip, payment, rating domains
- ✅ Domain events & Outbox pattern for reliable event publishing
- ✅ Virtual Threads (Java 21) enabled for HTTP requests and async operations
- ✅ Parallel query execution using `ParallelExecutor` utility
- ✅ PostGIS integration for geospatial driver queries (>10K drivers, <50ms queries)
- ✅ Comprehensive test coverage (unit, integration, E2E, architecture tests)
- ✅ Upgraded to **Spring Boot 3.4.5** and **Spring Cloud 2024.0.0**

**Key Characteristics:**
- **Multi-tenancy**: All aggregates include `TenantId` for data isolation
- **Performance**: Virtual threads, parallel I/O, PostGIS spatial indexes
- **Reliability**: Outbox pattern ensures event delivery even on service crashes
- **Domain-driven**: Rich domain models with behavior-driven entities

## Architecture Principles - NON-NEGOTIABLE

This project follows **Hexagonal Architecture** (Ports & Adapters) and **SOLID principles** with ZERO tolerance for violations.

### Mandatory Rules
1. **Domain Layer Purity**: NEVER import Spring, JPA, Jackson, or ANY framework in `domain/` package
2. **Dependency Direction**: Dependencies MUST flow inward only (Infrastructure → Application → Domain)
3. **Interface Segregation**: Ports must be small, focused interfaces - one responsibility per port
4. **No Leaky Abstractions**: JPA entities, HTTP requests, external APIs stay in infrastructure
5. **Constructor Injection Only**: All dependencies injected via constructor (enables testability)
6. **Domain Logic in Domain**: Business rules, validations, invariants belong ONLY in domain layer
7. **Immutable Value Objects**: Value objects like `Email`, `CPF`, `Money`, `Location` are immutable

### Violations That Will Be Rejected
- `@Entity`, `@Table`, `@Column` annotations in domain entities
- `@RestController`, `@Service`, `@Component` in domain or application layers
- Direct database access from use cases (must go through ports)
- Use cases depending on concrete implementations instead of ports
- Domain entities with public setters or anemic models (no behavior)
- Framework exceptions bubbling up to domain layer
- Framework imports in any domain/ classes

## Architecture Overview

This project strictly follows **Hexagonal Architecture** (Ports & Adapters):

- **Domain Layer** (`com.rappidrive.domain`): Pure business logic, rich entities, value objects, domain services, enums. Zero framework dependencies.
- **Application Layer** (`com.rappidrive.application`): Use cases implementing input ports, application services, `ParallelExecutor` for concurrent operations, custom exceptions.
- **Infrastructure Layer** (`com.rappidrive.infrastructure`): Port adapters (JPA repositories, external service clients), mappers, Spring configuration, messaging, monitoring.
- **Presentation Layer** (`com.rappidrive.presentation`): REST controllers, DTOs, mappers, exception handlers.

### Dependency Rule (Sacred)
Dependencies flow **inward only**: Infrastructure → Application → Domain. Domain is completely isolated from external dependencies.

## Key Patterns & Conventions

### Package Structure
```
src/main/java/com/rappidrive/
├── domain/                    # Business logic (framework-free)
│   ├── entities/              # Domain entities
│   ├── valueobjects/          # Value objects (Email, CPF, Money, etc.)
│   ├── services/              # Domain services
│   └── exceptions/            # Domain exceptions
├── application/               # Use cases and port interfaces
│   ├── usecases/              # Use case implementations
│   └── ports/
│       ├── input/             # Driving ports (use case interfaces)
│       └── output/            # Driven ports (repository, external service interfaces)
├── infrastructure/            # Port implementations (framework-dependent)
│   ├── persistence/           # JPA entities, repositories
│   ├── adapters/              # External service adapters
│   ├── messaging/             # Message queue adapters
│   └── config/                # Spring configuration, beans
└── presentation/              # REST API layer
    ├── controllers/           # Spring REST controllers
    ├── dto/                   # Request/Response DTOs
    └── mappers/               # DTO ↔ Domain mappers
```

### Naming Conventions
- **Use Cases**: `{Action}{Entity}UseCase` (e.g., `CreateUserUseCase`, `GetOrderByIdUseCase`)
- **Input Ports**: `{Action}{Entity}InputPort` (e.g., `CreateUserInputPort`)
- **Output Ports**: `{Entity}RepositoryPort`, `{Service}Port` (e.g., `UserRepositoryPort`, `EmailServicePort`)
- **Adapters**: `{Implementation}{Adapter}` (e.g., `JpaUserRepositoryAdapter`, `SendGridEmailAdapter`)
- **DTOs**: `{Entity}{Action}Request/Response` (e.g., `CreateUserRequest`, `UserResponse`)
- **Domain Entities**: PascalCase without suffixes (e.g., `User`, `Order`, `Product`)
- **Value Objects**: Descriptive names (e.g., `Email`, `CPF`, `Money`, `Address`)

### Use Case Pattern
Each use case should:
1. Implement an input port interface
2. Depend only on output ports (constructor injection)
3. Return domain entities or custom response objects
4. Handle a single business operation
5. Be annotated with `@UseCase` (custom annotation) or declared as `@Bean` in infrastructure layer

Example:
```java
// application/ports/input/CreateUserInputPort.java
public interface CreateUserInputPort {
    User execute(CreateUserCommand command);
}

// application/usecases/CreateUserUseCase.java
public class CreateUserUseCase implements CreateUserInputPort {
    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    
    public CreateUserUseCase(UserRepositoryPort userRepository, 
                            EmailServicePort emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    @Override
    public User execute(CreateUserCommand command) {
        // Domain logic here
        User user = new User(command.name(), command.email());
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail());
        return savedUser;
    }
}
```

### Repository Pattern
- Define repository interfaces in `application/ports/output/` (e.g., `UserRepositoryPort`)
- Implement in `infrastructure/persistence/` (e.g., `JpaUserRepositoryAdapter`)
- Ports work with domain entities only
- Adapters convert between JPA entities and domain entities
- Use Spring Data JPA in adapters, never expose JPA annotations to domain

### Error Handling
- **Domain exceptions**: Extend `DomainException` (unchecked), placed in `domain/exceptions/`
  - Examples: `InvalidEmailException`, `UserNotFoundException`, `EntityAlreadyExistsException` (409)
- **Application exceptions**: Use case specific, extend `ApplicationException`
- **Infrastructure exceptions**: Wrap external errors at adapter boundaries (never leak JPA/HTTP exceptions)
- Use `@RestControllerAdvice` in presentation layer to handle exceptions globally
- Return proper HTTP status codes: 400 for validation/rules, 404 for not found, 409 for conflicts, 500 for unexpected errors

## Development Workflows

### Prerequisites & Setup
- **Java**: 21+ (LTS) - Required for virtual threads
- **Build**: Maven 3.8+ (Compiler args: `-parameters` mandatory)
- **Database**: PostgreSQL 16 + PostGIS 3.4 (via Docker)

### Build & Run Commands
```bash
# Full build (includes tests)
mvn clean install

# Run application
mvn spring-boot:run

# Run only unit tests
mvn test

# Run integration + E2E tests (uses Testcontainers)
mvn verify
```

### Spring Boot 3.4+ Migration Gotchas
1. **Security**: Unified in `SecurityConfiguration.java`. Use `rappidrive.security.test-mode: true` in test profiles to bypass auth. Avoid separate `TestSecurityConfiguration` classes.
2. **Hibernate 6.6**: If an ID is assigned in the domain (manually), REMOVE `@GeneratedValue` from the JPA entity to avoid `StaleObjectStateException`.
3. **Flyway 10**: Require `flyway-database-postgresql` dependency for PostgreSQL support.
4. **Records**: Ensure `-parameters` is set in `maven-compiler-plugin` for Jackson to correctly map record components to JSON fields.

### Key Project Features to Understand Before Coding
1. **Virtual Threads** (Java 21): Enabled by default (`spring.threads.virtual.enabled=true`)
2. **ParallelExecutor** (Concurrent Operations): utility for I/O-heavy parallel tasks
3. **Outbox Pattern** (Reliable Event Publishing): captured in `outbox_event` table, dispatched asynchronously
4. **PostGIS Spatial Queries**: KNN operator (`<->`) + GIST index for fast driver matching
5. **Multi-Tenancy**: Tenant context resolved from JWT/Header, enforced via Hibernate `@Filter`

### Adding New Features (Step-by-Step)
1. **Domain First**: Entities/Value Objects in `domain/` (behavior-rich)
2. **Output Ports**: repository/service interfaces in `application/ports/output/`
3. **Input Port**: use case interface + inner `Command` record in `application/ports/input/`
4. **Use Case**: implementation in `application/usecases/` (Constructor injection)
5. **Adapters**: implementation in `infrastructure/` (always map entities)
6. **Configuration**: Wire beans in `infrastructure/config/UseCaseConfiguration.java`
7. **Presentation**: REST controller + DTOs + Mapper in `presentation/`

### Testing Strategy
- **Unit Tests**: Mockito mocks for output ports, test domain/use case logic
- **Integration Tests**: Testcontainers for repositories/adapters
- **E2E Tests**: RestAssured + production DTOs to validate full workflows
- **Architecture Tests**: ArchUnit enforces hexagonal boundaries

## Technology Stack

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4.5, Spring Cloud 2024.0.0
- **Database**: PostgreSQL 16, Hibernate 6.6, Flyway 10
- **Testing**: JUnit 5, Mockito, Testcontainers, ArchUnit
- **Mapping**: MapStruct 1.6.2
- **Boilerplate**: Lombok 1.18.34
- **Documentation**: SpringDoc 2.8.1

## References

- Hexagonal Architecture pattern by Alistair Cockburn
- Get Your Hands Dirty on Clean Architecture by Tom Hombergs
- Spring Boot 3.4 Release Notes
