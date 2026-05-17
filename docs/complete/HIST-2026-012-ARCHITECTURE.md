# HIST-2026-012: Architecture & Implementation Guide

## Complete Architecture Overview

### Layer Structure (Hexagonal)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER (REST API)                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ApprovalController                                                       │
│  ├── GET  /api/approvals/pending                                         │
│  ├── GET  /api/approvals/{id}                                            │
│  ├── POST /api/approvals/{id}/approve                                    │
│  ├── POST /api/approvals/{id}/reject                                     │
│  └── GET  /api/drivers/{driverId}/approval-status                        │
│                                                                           │
│  DTOs & Mappers:                                                         │
│  ├── ApproveApprovalRequest                                              │
│  ├── RejectApprovalRequest                                               │
│  ├── DriverApprovalResponse                                              │
│  └── DriverApprovalDtoMapper                                             │
│                                                                           │
└────────────────┬────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER (Use Cases)                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  INPUT PORTS (Interfaces):                                               │
│  ├── SubmitDriverApprovalInputPort                                       │
│  ├── ListPendingApprovalsInputPort                                       │
│  ├── ApproveDriverInputPort                                              │
│  └── RejectDriverInputPort                                               │
│                                                                           │
│  USE CASES (Implementations):                                            │
│  ├── SubmitDriverApprovalUseCase                                         │
│  │   └─ Cria DriverApprovalRequest, publica evento                       │
│  ├── ListPendingApprovalsUseCase                                         │
│  │   └─ Query paginated com tenant isolation                             │
│  ├── ApproveDriverUseCase                                                │
│  │   └─ Approve + Ativa driver + Publica evento                          │
│  └── RejectDriverUseCase                                                 │
│      └─ Reject + Bloqueia/inativa driver + Publica evento                │
│                                                                           │
│  OUTPUT PORTS (Interfaces):                                              │
│  ├── DriverApprovalRepositoryPort (CRUD + queries)                       │
│  ├── DriverRepositoryPort (já existe)                                    │
│  ├── AdminUserRepositoryPort (lookup)                                    │
│  ├── NotificationPort (já existe)                                        │
│  └── EventDispatcherPort (já existe)                                     │
│                                                                           │
└────────────────┬────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER (Pure Logic)                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  AGGREGATES:                                                             │
│  ├── DriverApprovalRequest                                               │
│  │   ├── approve(adminId) → status APPROVED                              │
│  │   ├── reject(adminId, reason) → status REJECTED                       │
│  │   └── Invariants:                                                     │
│  │       ├─ PENDING → APPROVED or PENDING → REJECTED only                │
│  │       ├─ rejectionReason required if REJECTED                         │
│  │       └─ reviewedByAdminId required if not PENDING                    │
│  │                                                                        │
│  │  (Also updates Driver via DriverRepositoryPort)                       │
│  │                                                                        │
│  ENUMS:                                                                  │
│  ├── ApprovalStatus: PENDING, APPROVED, REJECTED                         │
│  └── AdminRole: SUPER_ADMIN, COMPLIANCE_OFFICER, SUPPORT_ADMIN           │
│                                                                           │
│  VALUE OBJECTS:                                                          │
│  └── AdminUser (id, email, role, fullName, createdAt)                    │
│                                                                           │
│  DOMAIN EVENTS:                                                          │
│  ├── DriverApprovalSubmittedEvent                                        │
│  │   └─ Triggered: When driver submits approval                          │
│  ├── DriverApprovedEvent                                                 │
│  │   └─ Triggered: When admin approves                                   │
│  └── DriverRejectedEvent                                                 │
│      └─ Triggered: When admin rejects                                    │
│                                                                           │
│  DOMAIN SERVICES (if needed):                                            │
│  └── ApprovalValidationService (document verification)                   │
│                                                                           │
└────────────────┬────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE LAYER (Adapters)                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  PERSISTENCE ADAPTERS:                                                  │
│  ├── JpaDriverApprovalRepositoryAdapter                                  │
│  │   ├── DriverApprovalRequestJpaEntity (JPA mapping)                    │
│  │   ├── DriverApprovalRequestMapper (domain ↔ JPA)                      │
│  │   └── Methods:                                                        │
│  │       ├─ save(DriverApprovalRequest)                                  │
│  │       ├─ findById(id)                                                 │
│  │       ├─ findByDriverId(driverId)                                     │
│  │       ├─ findPendingByTenant(tenantId, page, size)                    │
│  │       └─ findRejectedByDriver(driverId)                               │
│  │                                                                        │
│  ├── JpaAdminUserRepositoryAdapter                                       │
│  │   ├── AdminUserJpaEntity (JPA mapping)                                │
│  │   ├── AdminUserMapper (domain ↔ JPA)                                  │
│  │   └── Methods:                                                        │
│  │       ├─ findById(id)                                                 │
│  │       ├─ findByEmail(email)                                           │
│  │       └─ validateRole(adminId, requiredRole)                          │
│  │                                                                        │
│  DATABASE TABLES:                                                        │
│  ├── driver_approval_requests                                            │
│  │   ├─ id (PK)                                                          │
│  │   ├─ driver_id (FK)                                                   │
│  │   ├─ tenant_id (FK, for multi-tenancy)                                │
│  │   ├─ status (ENUM: PENDING, APPROVED, REJECTED)                       │
│  │   ├─ submitted_documents (JSONB)                                      │
│  │   ├─ submitted_at (TIMESTAMP)                                         │
│  │   ├─ reviewed_at (TIMESTAMP)                                          │
│  │   ├─ reviewed_by_admin_id (FK)                                        │
│  │   ├─ rejection_reason (VARCHAR)                                       │
│  │   ├─ created_at, updated_at                                           │
│  │   └─ Indexes: (status, tenant_id), (driver_id)                        │
│  │                                                                        │
│  └── admin_users                                                         │
│      ├─ id (PK)                                                          │
│      ├─ email (UNIQUE)                                                   │
│      ├─ role (ENUM)                                                      │
│      ├─ full_name                                                        │
│      ├─ tenant_id (FK)                                                   │
│      ├─ is_active (BOOLEAN)                                              │
│      ├─ created_at, updated_at                                           │
│      └─ Indexes: (email), (role, tenant_id)                              │
│                                                                           │
│  CONFIGURATION:                                                          │
│  └── ApprovalBeanConfiguration.java                                      │
│      ├─ Wire use cases                                                   │
│      ├─ Wire adapters                                                    │
│      └─ Configure event dispatching                                      │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagrams

### Happy Path: Driver Approval

```
┌─────────────────────┐
│ Driver Registration │
│   (PENDING_APPROVAL)│
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│ SubmitDriverApprovalUseCase                  │
│                                              │
│ 1. Load driver from DriverRepositoryPort    │
│ 2. Validate driver is PENDING_APPROVAL      │
│ 3. Validate documents (min 2)               │
│ 4. Create DriverApprovalRequest (PENDING)   │
│ 5. Save via DriverApprovalRepositoryPort    │
│ 6. Publish DriverApprovalSubmittedEvent     │
└──────────┬───────────────────────────────────┘
           │
           ▼
    ┌─────────────────────┐
    │ Outbox Event Table  │
    │ (For reliability)   │
    └──────────┬──────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│ Admin Reviews Pending Approvals              │
│ ListPendingApprovalsUseCase                  │
│                                              │
│ 1. Validate admin role (not SUPPORT_ADMIN)   │
│ 2. Query DriverApprovalRepositoryPort        │
│    WHERE status = PENDING                    │
│    AND tenant_id = admin.tenant_id           │
│ 3. Return paginated results                  │
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│ Admin Approves Driver                        │
│ ApproveDriverUseCase                         │
│                                              │
│ 1. Validate admin (SUPER_ADMIN or OFFICER)   │
│ 2. Load DriverApprovalRequest (PENDING)      │
│ 3. Call approvalRequest.approve(adminId)     │
│    (domain validation + state change)        │
│ 4. Load Driver from DriverRepositoryPort     │
│ 5. Call driver.activate()                    │
│    (validates conditions, changes status)    │
│ 6. Save BOTH in transaction                  │
│ 7. Publish DriverApprovedEvent               │
│ 8. Send notification (email)                 │
└──────────┬───────────────────────────────────┘
           │
           ▼
    ┌─────────────────────┐
    │ Driver is ACTIVE    │
    │ Can accept trips    │
    └─────────────────────┘
```

### Error Path: Driver Rejection

```
┌──────────────────────────────────────────────┐
│ Admin Reviews Pending Approvals              │
│ (Same as above)                              │
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│ Admin Rejects Driver                         │
│ RejectDriverUseCase                          │
│                                              │
│ 1. Validate admin (SUPER_ADMIN or OFFICER)   │
│ 2. Load DriverApprovalRequest (PENDING)      │
│ 3. Call approvalRequest.reject(adminId,      │
│    rejectionReason)                          │
│    (domain validation)                       │
│ 4. Load Driver                               │
│ 5. If permanentBan=true:                     │
│    - Call driver.block() (status BLOCKED)    │
│    - Driver cannot reapply                   │
│    Else:                                     │
│    - Call driver.deactivate()                │
│      (status INACTIVE)                       │
│    - Driver can resubmit in future           │
│ 6. Save BOTH in transaction                  │
│ 7. Publish DriverRejectedEvent               │
│ 8. Send notification (email with reason)     │
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│ Driver Status Updated                        │
│ - INACTIVE: Can reapply after fixes          │
│ - BLOCKED: Permanent ban, cannot reapply     │
└──────────────────────────────────────────────┘
```

## State Machine: DriverApprovalRequest

```
                    ┌─────────────┐
                    │   PENDING   │ ◄─── Initial state
                    │  (Created)  │      after submission
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
      ┌────────────────┐        ┌────────────────┐
      │   APPROVED     │        │   REJECTED     │
      │ Admin approves │        │ Admin rejects  │
      │ Driver ACTIVE  │        │ Driver INACTIVE│
      │                │        │ or BLOCKED     │
      └────────────────┘        └────────────────┘
             (FINAL)                   (FINAL)

Note: No transitions allowed from APPROVED or REJECTED
      (immutable once finalized)
```

## State Machine: Driver (affected by approval)

```
┌──────────────────────┐
│ PENDING_APPROVAL     │ ◄─── When first registered
│ (from registration)  │
└──────────┬───────────┘
           │
           │ Admin approves (ApproveDriverUseCase)
           ▼
┌──────────────────────┐
│ ACTIVE               │ ◄─── Can accept trips
│ (ready for duty)     │
└──────────┬───────────┘
           │ Admin rejects with ban (RejectDriverUseCase)
           │
           ├─────────────────┬──────────────────┐
           │                 │                  │
     permanent_ban=false  permanent_ban=true    │
           │                 │                  │
           ▼                 ▼                  │
┌──────────────────────┐┌──────────────────────┐
│ INACTIVE             ││ BLOCKED              │
│ (Can resubmit)       ││ (Permanent ban)      │
│                      ││                      │
└──────────┬───────────┘└──────────────────────┘
           │
           │ Driver resubmits documentation
           │ (back to PENDING_APPROVAL cycle)
           │
           └──────► PENDING_APPROVAL
```

## Use Case: ApproveDriverUseCase - Detailed Logic

```java
@Service
public class ApproveDriverUseCase implements ApproveDriverInputPort {
    
    private final DriverApprovalRepositoryPort approvalRepo;
    private final DriverRepositoryPort driverRepo;
    private final AdminUserRepositoryPort adminRepo;
    private final NotificationPort notificationPort;
    private final EventDispatcherPort eventDispatcher;
    
    @Override
    @Transactional
    public void execute(ApproveDriverCommand command) {
        // PHASE 1: Validation
        AdminUser admin = adminRepo.findById(command.adminId())
            .orElseThrow(() -> new AdminNotFoundException());
        
        if (admin.role() != AdminRole.SUPER_ADMIN && 
            admin.role() != AdminRole.COMPLIANCE_OFFICER) {
            throw new AdminUnauthorizedException(
                "Role " + admin.role() + " cannot approve drivers");
        }
        
        // PHASE 2: Load & Validate Approval Request
        DriverApprovalRequest approval = approvalRepo
            .findById(command.approvalRequestId())
            .orElseThrow(() -> new ApprovalRequestNotFoundException());
        
        if (!approval.isPending()) {
            throw new InvalidApprovalStateException(
                "Approval is already " + approval.getStatus());
        }
        
        // PHASE 3: Domain Logic (Approve)
        approval.approve(command.adminId());  // Changes status to APPROVED
        // Invariant: now approval.isApproved() == true
        
        // PHASE 4: Load Driver & Activate
        Driver driver = driverRepo.findById(approval.driverId())
            .orElseThrow(() -> new DriverNotFoundException());
        
        if (driver.getStatus() != DriverStatus.PENDING_APPROVAL) {
            throw new InvalidDriverStateException(
                "Driver not in PENDING_APPROVAL state");
        }
        
        driver.activate();  // Changes status to ACTIVE
        // Invariant: now driver.isActive() == true
        
        // PHASE 5: Persist (Same Transaction)
        approvalRepo.save(approval);
        driverRepo.save(driver);
        
        // PHASE 6: Events & Notifications
        DriverApprovedEvent event = new DriverApprovedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            approval.driverId(),
            approval.id(),
            command.adminId(),
            admin.fullName()
        );
        eventDispatcher.dispatch(event);
        
        notificationPort.sendNotification(
            driver.email().value(),
            "approval_granted",
            Map.of(
                "driverName", driver.fullName(),
                "approverName", admin.fullName()
            )
        );
    }
}
```

## Security & Permission Model

### Role-Based Access Control (RBAC)

| Role | List Pending | Approve | Reject | View Analytics |
|------|:------------:|:-------:|:------:|:---------------:|
| SUPER_ADMIN | ✅ | ✅ | ✅ | ✅ |
| COMPLIANCE_OFFICER | ✅ | ✅ | ✅ | ❌ |
| SUPPORT_ADMIN | ❌ | ❌ | ❌ | ❌ |

### Multi-Tenancy Enforcement

```
1. Every approval request has tenant_id
2. Every admin user has tenant_id
3. Admin can only see approvals from their own tenant
4. Query always includes: WHERE tenant_id = current_user.tenant_id
5. No cross-tenant data leaks possible
```

## Testing Strategy

### Unit Tests
- **DriverApprovalRequest**: State transitions, invariants
- **ApproveDriverUseCase**: Happy path, role validation, state errors
- **RejectDriverUseCase**: Happy path, permanent ban logic
- **ListPendingApprovalsUseCase**: Pagination, tenant isolation
- **AdminUser**: Value object immutability

### Integration Tests
- **JpaDriverApprovalRepositoryAdapter**: Persistence & queries
- **JpaAdminUserRepositoryAdapter**: Lookup & validation
- **Database migrations**: Schema creation
- **Pessimistic locking**: Concurrent access handling

### E2E Tests
- **Happy path**: Submit → List → Approve → Driver ACTIVE
- **Rejection path**: Submit → List → Reject → Driver INACTIVE/BLOCKED
- **Permission check**: Support admin tries to approve → 403
- **Tenant isolation**: Admin A cannot see approval requests from Tenant B

## Performance Considerations

### Query Optimization
```
-- Find pending approvals efficiently
SELECT * FROM driver_approval_requests
WHERE status = 'PENDING'
  AND tenant_id = ?
  AND created_at DESC
LIMIT 50 OFFSET 0;

Index: (status, tenant_id, created_at DESC)
Expected: <50ms for 100k+ records
```

### Caching (Future)
```
- Cache admin roles (TTL: 1 hour)
  Key: admin:{adminId}
  Value: AdminUser with role info
  
- Cache driver approval status (TTL: 5 min)
  Key: approval:driver:{driverId}
  Value: ApprovalStatus + timestamp
```

## Monitoring & Observability

### Structured Logs

```
[APPROVAL_SUBMITTED] driverId=UUID approvalId=UUID documents=3
[APPROVAL_LISTED] adminId=UUID page=0 size=50 total=120
[APPROVAL_APPROVED] approvalId=UUID adminId=UUID status=ACTIVE
[APPROVAL_REJECTED] approvalId=UUID adminId=UUID reason="docs_invalid" ban=false
[APPROVAL_ERROR] operation=approve approvalId=UUID error="already_approved"
```

### Metrics to Track
- Approval submission rate (per day, per tenant)
- Approval rate (% approved vs rejected)
- Average time from submission to decision
- Admin activity (approvals per day per admin)
- Error rate by operation

## Migration Path (Database)

```sql
-- V1.0__Create_approval_tables.sql
CREATE TABLE driver_approval_requests (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED')),
    submitted_documents JSONB NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    reviewed_by_admin_id UUID,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id) ON DELETE SET NULL
);

CREATE INDEX idx_approval_status_tenant 
    ON driver_approval_requests(status, tenant_id);
CREATE INDEX idx_approval_driver_id 
    ON driver_approval_requests(driver_id);
CREATE INDEX idx_approval_tenant_created 
    ON driver_approval_requests(tenant_id, created_at DESC);

CREATE TABLE admin_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL CHECK(role IN ('SUPER_ADMIN', 'COMPLIANCE_OFFICER', 'SUPPORT_ADMIN')),
    full_name VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_admin_email ON admin_users(email);
CREATE INDEX idx_admin_role_tenant ON admin_users(role, tenant_id);
```

---

This architecture ensures:
✅ **Separation of Concerns**: Each layer has clear responsibility  
✅ **Testability**: Dependency injection + ports allow easy mocking  
✅ **Scalability**: Indexed queries, batch processing, caching-ready  
✅ **Security**: RBAC + multi-tenancy + transaction isolation  
✅ **Reliability**: Events, audit trail, transactional consistency  
