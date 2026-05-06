# Design Spec: RappiDrive MVP - Full Core Cycle and Structural Integrity

**Status**: Draft (Final Review)
**Date**: 2026-05-06
**Goal**: Establish a robust, secure, and financially transparent MVP for RappiDrive, covering the complete ride lifecycle from onboarding to payment, with high resilience against real-world mobile network conditions.

---

## 1. Scope & Critical Pillars

### 1.1 Core Lifecycle (Primary Flow)
- **Identity & Onboarding**: Automatic Keycloak account provisioning during registration; `/me` endpoint for profile resolution.
- **Geofencing**: Trip requests are validated against active `ServiceArea` polygons.
- **Mixed Dispatch (Waves)**: 3-stage expansion (3 drivers -> 10 drivers -> Broadcast) every 15 seconds.
- **Trip States**: REQUESTED -> DRIVER_ASSIGNED -> **DRIVER_ARRIVED** -> IN_PROGRESS -> COMPLETED.
- **Payments**: Automatic Stripe charging upon completion; fare based on distance + time.

### 1.2 Reliability & Compliance (The "Trust" Layer)
- **Idempotency**: `Idempotency-Key` header enforcement for Trip creation, Assignment, and Payment.
- **Vehicle Approval**: Mandatory document submission (CRLV) and admin approval before activation.
- **Ghost Driver Cleanup**: 2-minute heartbeat based on location updates; 15-minute auto-offline for stale drivers.
- **Driver Earnings**: Real-time ledger tracking gross fare, platform fees (20%), and net balance.

---

## 2. Architecture & Components

### 2.1 Identity & Auto-Provisioning
- **Sync Logic**: `CreateDriverUseCase` and `CreatePassengerUseCase` will orchestrate `IdentityProvisioningPort` calls to ensure Keycloak and PostgreSQL are in sync.
- **Profile Resolution**: `GetCurrentUserUseCase` extracts `keycloakId` from JWT `sub` to find the local profile.

### 2.2 Geofencing & Service Areas
- **Validation**: `CreateTripUseCase` will call `ServiceAreaRepositoryPort` to check if `origin` coordinates reside within an active polygon for the current Tenant.

### 2.3 Resilient Dispatch & Timers
- **Persistence**: `Trip` entity tracks `dispatchWave` and `nextWaveAt`.
- **Worker**: A scheduled `DispatchWaveMonitor` (Virtual Threads) scans for trips needing wave expansion, ensuring progress even after server restarts.

### 2.4 Idempotency Mechanism
- **Infrastructure Layer**: An `IdempotencyFilter` will check the `idempotency_records` table.
- **Behavior**: If a key exists, return the cached JSON response; otherwise, proceed and save the result.

### 2.5 Driver Wallet (Earnings)
- **Entities**: `DriverAccount` (Balance) and `LedgerEntry` (Transaction history).
- **Automation**: `UpdateDriverBalanceHandler` reacts to `TripCompletedEvent` to credit/debit the account.

---

## 3. Data Flow

1. **Integrated Onboarding**: User signs up -> Backend creates Keycloak Account + DB Profile + Initial Vehicle Record (INACTIVE).
2. **Approval**: Admin reviews Driver Docs + Vehicle CRLV -> Both activated.
3. **Trip Request**: Passenger requests ride -> System validates Geofencing + Idempotency -> Trip created as `REQUESTED`.
4. **Active Dispatch**: Wave 1 (15s) -> Wave 2 (15s) -> Wave 3 (Broadcast).
5. **Pickup Cycle**: Driver accepts -> Driver arrives (`DRIVER_ARRIVED`) -> Trip starts (`IN_PROGRESS`).
6. **Financial Closure**: Trip completes -> Fare calculated -> Stripe charged -> Driver balance updated -> Trip status `COMPLETED`.

---

## 4. Testing Strategy

- **Unit Tests**: State machine transitions in `Trip`, wave expansion logic, and fare calculation.
- **Integration Tests**: PostGIS polygon matching for Geofencing; Idempotency table persistence.
- **E2E Tests**:
    - **Concurrency**: Parallel acceptance of the same trip.
    - **Network Failure**: Retrying a payment request with the same `Idempotency-Key`.
    - **Fleet Management**: Heartbeat timeout and auto-deactivation.

---

## 5. Success Criteria
- [ ] End-to-end trip flow (Request to Payment) works flawlessly over simulated 3G latency.
- [ ] No driver can be activated without an approved vehicle and profile.
- [ ] Drivers see their updated earnings balance within 1 second of trip completion.
- [ ] 100% tenant data isolation across all new entities.
