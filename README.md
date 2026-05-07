# RappiDrive Backend - Hexagonal Architecture

White-label ride-hailing platform (MVP) built with Hexagonal Architecture (Ports & Adapters).

## 📚 Documentation

- Mapa da documentação: [docs/INDEX.md](docs/INDEX.md)
- Referência atual de domínio e arquitetura: [docs/HIST-2026-012.md](docs/HIST-2026-012.md)
- Diagramas atualizados: [docs/HIST-2026-012-DIAGRAMS.md](docs/HIST-2026-012-DIAGRAMS.md)

## 🏗️ Architecture

This project follows **Hexagonal Architecture** principles with strict separation of concerns:

```
src/main/java/com/rappidrive//
├── domain/                    # Pure business logic (framework-free)
│   ├── entities/              # Domain entities
│   ├── valueobjects/          # Value objects (immutable)
│   ├── services/              # Domain services
│   └── exceptions/            # Domain exceptions
├── application/               # Use cases and orchestration
│   ├── usecases/              # Use case implementations
│   └── ports/
│       ├── input/             # Driving ports (interfaces)
│       └── output/            # Driven ports (repository, external services)
├── infrastructure/            # Framework & external integrations
│   ├── persistence/           # JPA entities, repositories
│   ├── adapters/              # External service adapters
│   └── config/                # Spring configuration
└── presentation/              # REST API
    ├── controllers/           # REST controllers
    ├── dto/                   # Request/Response DTOs
    └── mappers/               # DTO ↔ Domain mappers
```

**Key Principles:**
- ✅ Domain layer is **100% framework-free** (no Spring, JPA, Jackson)
- ✅ Dependencies flow **inward only** (Infrastructure → Application → Domain)
- ✅ All dependencies injected via **constructor**
- ✅ Architecture tests enforce boundaries with **ArchUnit**

## 🚀 Quick Start

### Prerequisites
- Java 21+ (LTS)
- Maven 3.8+
- Docker & Docker Compose (for PostgreSQL)

**Installing Java 21:**
- macOS: `brew install openjdk@21`
- SDKMAN: `sdk install java 21-tem`
- Verify: `java -version` (should show 21.x.x)

### 1. Start Database
```bash
docker-compose up -d
```

This starts:
- PostgreSQL 16 with PostGIS extension (port 5432)
- pgAdmin 4 (port 5050, admin@rappidrive.com / admin)

### 2. Build Project
```bash
mvn clean install
```

### 3. Run Application
```bash
mvn spring-boot:run
```

Application runs at: http://localhost:8080

Health check: http://localhost:8080/api/health

### 4. Observability Stack

```bash
docker-compose up -d zipkin prometheus
```

- **Tracing**: Zipkin UI at http://localhost:9411 (backend exports spans via OpenTelemetry)
- **Metrics**: Prometheus at http://localhost:9090 scraping `/actuator/prometheus`
- **Logging**: Structured JSON logging enabled in `prod` profile.

### 5. Run Tests
```bash
# Unit tests
mvn test

# Integration tests (uses Testcontainers)
mvn verify

# Architecture tests
mvn test -Dtest=HexagonalArchitectureTest
```

## 🛠️ Tech Stack

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4.5, Spring Cloud 2024.0.0
- **Concurrency**: Virtual Threads (Project Loom)
- **Database**: PostgreSQL 16 + PostGIS (geospatial), Hibernate 6.6
- **Migrations**: Flyway 10
- **Testing**: JUnit 5, Mockito, Testcontainers, ArchUnit, RestAssured
- **Mapping**: MapStruct 1.6.2
- **Validation**: Bean Validation (Jakarta)

## ⚡ Performance Features

### Virtual Threads (Java 21+)
This application uses **Virtual Threads** for improved scalability:
- Every HTTP request runs on a virtual thread
- `@Async` methods use virtual threads
- Supports millions of concurrent operations with minimal overhead

**Configuration**: `spring.threads.virtual.enabled=true` in `application.yml`

### Parallel Execution with CompletableFuture
Leverages virtual threads for parallel I/O operations:
- Driver search across multiple geographic zones
- Parallel validation workflows

**Pattern**: `ParallelExecutor` utility class provides `executeAll()`, `executeRace()`, `mapParallel()`.

### PostGIS Spatial Indexes
Optimized geospatial queries for driver location search:
- GIST indexes on driver locations
- KNN operator (`<->`) for ultra-fast nearest neighbor search

## 🔧 Development Workflow

### Spring Boot 3.4+ Migration Notes
1. **Security**: Unified in `SecurityConfiguration.java`. Use `rappidrive.security.test-mode: true` in tests.
2. **Hibernate**: Manual IDs (UUID) must NOT have `@GeneratedValue` in JPA entities to avoid `StaleObjectStateException`.
3. **Flyway**: Modularized drivers (requires `flyway-database-postgresql`).
4. **Records**: Compiler flag `-parameters` required for record JSON mapping.

### Adding a New Feature
1. **Domain First**: Create entities/value objects in `domain/` (pure Java)
2. **Output Ports**: Define interfaces in `application/ports/output/`
3. **Input Port**: Create use case interface in `application/ports/input/`
4. **Use Case**: Implement in `application/usecases/`
5. **Adapters**: Implement ports in `infrastructure/` (mapping between JPA and Domain)
6. **Configuration**: Wire beans in `infrastructure/config/UseCaseConfiguration.java`
7. **Presentation**: Create REST controller + DTOs + Mapper in `presentation/`

## 📝 Code Standards

### Mandatory Rules
1. **Domain layer purity**: NO Spring, JPA, or framework imports in `domain/`
2. **Constructor injection**: All dependencies via constructor
3. **No public setters**: Domain entities use behavior methods
4. **Value objects**: Use for domain concepts (Email, Money, Location, etc.)
5. **Mappers**: Always map between layers (DTO ↔ Domain, JPA ↔ Domain)

---

**HIST-2025-001** - Initial project setup ✅
**HIST-2026-030** - Driver onboarding hardening ✅
**HIST-2026-041** - Upgrade to Spring Boot 3.4.5 ✅
