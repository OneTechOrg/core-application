# GEMINI.md - Instructional Context for RappiDrive Backend

This document provides foundational mandates and technical context for AI agents working on the RappiDrive Backend. All interactions must adhere to the architecture and standards defined herein.

---

## 🚀 Project Overview

**RappiDrive** is a multi-tenant, white-label ride-hailing platform (Uber-like MVP).  
- **Runtime:** Java 21 (Project Loom / Virtual Threads enabled)
- **Framework:** Spring Boot 3.4.5 + Spring Cloud 2024.0.0
- **Architecture:** Strict **Hexagonal Architecture** (Ports & Adapters)
- **Database:** PostgreSQL 16 + PostGIS 3.4
- **Security:** Keycloak 23 (OAuth2/OIDC Resource Server)

---

## 🏗️ Architecture Mandates

The project enforces strict separation of concerns via **ArchUnit** (`HexagonalArchitectureTest`). Violations fail the build.

### Layer Map & Dependencies
1.  **Domain Layer** (`domain/`): Pure business logic. **ZERO** framework imports (No Spring, JPA, or Jackson). Entities are behavior-rich, not anemic.
2.  **Application Layer** (`application/`): Orchestration via Use Cases. Contains Port interfaces (Input/Output).
3.  **Infrastructure Layer** (`infrastructure/`): Framework implementations (JPA Adapters, Keycloak, Messaging).
4.  **Presentation Layer** (`presentation/`): REST Controllers, DTOs, and Mappers.

**Dependency Rule:** Dependencies must flow **inward only** (Infra → App → Domain). Domain knows nothing about external layers.

---

## 🛠️ Key Technical Patterns

### 1. Persistence (Hibernate 6.6+)
- **Manual IDs:** All aggregate roots assign IDs in the Domain layer.
- **Mandate:** **DO NOT** use `@GeneratedValue` in JPA entities for these IDs. Hibernate 6.6 will throw `StaleObjectStateException` if detected on manually assigned IDs for new entities.

### 2. Security Configuration (SB 3.4+)
- **Unified Security:** All security rules live in a single `SecurityConfiguration.java`.
- **Switching Logic:** Use properties for mode switching, NOT conflicting `SecurityFilterChain` beans.
    - `rappidrive.security.test-mode=true`: Bypasses auth for tests.
    - `rappidrive.security.enabled=false`: Disables security (dev mode).

### 3. Resilience & Performance
- **Virtual Threads:** Enabled globally (`spring.threads.virtual.enabled=true`). Every HTTP request runs on a virtual thread.
- **Outbox Pattern:** Reliable event publishing. Events are captured in the same transaction as state changes and dispatched asynchronously.
- **Parallel I/O:** Use `ParallelExecutor` (Application layer) for concurrent tasks (e.g., geospatial searches across zones).

### 4. Multi-Tenancy
- Every entity carries a `TenantId`.
- Isolation is enforced at the JPA level via Hibernate `@Filter(condition = "tenant_id = :tenantId")`.
- The current tenant is resolved from the JWT claim and propagated via `TenantContext`.

---

## 📦 Building and Running

| Task | Command |
|---|---|
| Full Build | `mvn clean install` |
| Run App | `mvn spring-boot:run` |
| Unit Tests | `mvn test` |
| Integration/E2E | `mvn verify` (Requires Docker for Testcontainers) |
| Start Infra | `docker-compose up -d` |

**Compiler Flag:** `-parameters` is mandatory in Maven to ensure Java Records map correctly to JSON fields without redundant `@JsonProperty` annotations.

---

## 📝 Coding Standards

- **Constructor Injection:** The only acceptable form of DI.
- **Immutability:** Value Objects and DTOs (Records) must be immutable.
- **Mapping:** Always use Mappers (MapStruct) at layer boundaries (DTO ↔ Domain ↔ JPA).
- **Exceptions:**
    - `DomainException`: Business rules (maps to 400).
    - `EntityAlreadyExistsException`: Duplicate checks (maps to 409).
    - `ApplicationException`: Use case failures.
- **Flyway:** Versioned migrations in `src/main/resources/db/migration/`. Requires `flyway-database-postgresql` for modular support.

---

**Current Status:** HIST-2026-030 (Hardening) and HIST-2026-041 (SB 3.4 Upgrade) completed. ✅
