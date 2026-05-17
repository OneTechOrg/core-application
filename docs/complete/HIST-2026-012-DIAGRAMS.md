# HIST-2026-012: Visual Diagrams & Data Flows

## Sequence Diagram: Happy Path (Aprovação)

```
┌─────────────┐       ┌──────────┐       ┌─────────────────────────────────────────────────────────────┐
│   Motorista │       │  Sistema │       │              Admin Dashboard + Backend                     │
└──────┬──────┘       └─────┬────┘       └──────────────────────────────┬──────────────────────────────┘
       │                    │                                           │
       │ 1. Registra        │                                           │
       ├───────────────────>│                                           │
       │                    │                                           │
       │                    │ 2. Cria DriverApprovalRequest             │
       │                    ├──────────────────────────────────────────>│
       │                    │    status: PENDING                        │
       │                    │    submittedDocuments: [url1, url2]       │
       │                    │                                           │
       │                    │ 3. Salva em DB                            │
       │                    │<──────────────────────────────────────────┤
       │                    │    Publica DriverApprovalSubmittedEvent   │
       │                    │                                           │
       │  Email: "Aguardando│                                           │
       │  análise de admin" │                                           │
       │<───────────────────┤                                           │
       │                    │                                           │
       │                    │                                           ┌─────────────────┐
       │                    │                                           │   Admin Acessa  │
       │                    │                                           │   Dashboard     │
       │                    │                                           └────────┬────────┘
       │                    │                                           │
       │                    │ 4. GET /api/approvals/pending             │
       │                    │<──────────────────────────────────────────┤
       │                    │                                           │
       │                    │    Query: WHERE status = PENDING          │
       │                    │           AND tenant_id = ?               │
       │                    │                                           │
       │                    │    Response: [approval1, approval2, ...]  │
       │                    │──────────────────────────────────────────>│
       │                    │                                           │
       │                    │                                           ┌─────────────────┐
       │                    │                                           │   Admin Vê      │
       │                    │                                           │   Documentação  │
       │                    │                                           └────────┬────────┘
       │                    │                                           │
       │                    │ 5. POST /api/approvals/{id}/approve       │
       │                    │<──────────────────────────────────────────┤
       │                    │    { notes: "Documentação ok" }           │
       │                    │                                           │
       │                    │ 6. ApproveDriverUseCase.execute()         │
       │                    ├──────────────────────────────────────────>│
       │                    │    - Carrega DriverApprovalRequest        │
       │                    │    - approvalRequest.approve(adminId)     │
       │                    │    - Carrega Driver                       │
       │                    │    - driver.activate()                    │
       │                    │    - @Transactional: salva ambos          │
       │                    │    - Publica DriverApprovedEvent          │
       │                    │                                           │
       │                    │ 7. Atualiza DB                            │
       │                    │<──────────────────────────────────────────┤
       │                    │    driver_approval_requests: APPROVED     │
       │                    │    drivers: ACTIVE                        │
       │                    │    outbox_events: +1 evento               │
       │                    │                                           │
       │  Email: "Parabéns! │                                           │
       │  Você foi aprovado"│                                           │
       │<───────────────────┤                                           │
       │                    │                                           │
       │ Pode aceitar       │                                           │
       │ viagens! ✅        │                                           │
       │                    │                                           │
```

## Sequence Diagram: Error Path (Rejeição)

```
┌─────────────┐       ┌──────────┐       ┌─────────────────────────────────────┐
│   Motorista │       │  Sistema │       │      Admin Dashboard + Backend      │
└──────┬──────┘       └─────┬────┘       └───────────────┬─────────────────────┘
       │                    │                            │
       │                    │ 1-4. Mesmos passos da      │
       │                    │      sequência anterior    │
       │                    │                            │
       │                    │ 5. POST /api/approvals/    │
       │                    │     {id}/reject            │
       │                    │<───────────────────────────┤
       │                    │    {                       │
       │                    │      rejectionReason:      │
       │                    │      "CNH inválida",       │
       │                    │      permanentBan: false   │
       │                    │    }                       │
       │                    │                            │
       │                    │ 6. RejectDriverUseCase     │
       │                    ├───────────────────────────>│
       │                    │    - approvalRequest.      │
       │                    │      reject(adminId, ...)  │
       │                    │    - if permanentBan:      │
       │                    │        driver.block()      │
       │                    │      else:                 │
       │                    │        driver.deactivate() │
       │                    │    - Salva ambos (TX)      │
       │                    │    - Publica evento        │
       │                    │                            │
       │  Email: "Sua       │                            │
       │  aprovação foi     │<───────────────────────────┤
       │  rejeitada.        │                            │
       │  Motivo: CNH       │                            │
       │  inválida.         │                            │
       │  Você pode         │                            │
       │  resubmeter."      │                            │
       │<───────────────────┤                            │
       │                    │                            │
       │ Status: INACTIVE   │                            │
       │ Pode resubmeter    │                            │
       │                    │                            │
```

## State Transition Diagram: DriverApprovalRequest

```
                            ┌─────────────────┐
                            │     PENDING     │◄──────────┐
                            │  (Inicial state)│           │
                            └────────┬────────┘           │
                                     │                    │
                    ┌────────────────┼────────────────┐   │
                    │                │                │   │
                    │                │                │   │
                    ▼                ▼                ▼   │
          ┌──────────────────┐ ┌──────────────────┐    Motorista
          │    APPROVED      │ │    REJECTED      │    resubmete
          │ (Admin aprova)   │ │ (Admin rejeita)  │    docs
          │ driver: ACTIVE   │ │ driver: INACTIVE │    │
          │                  │ │      ou BLOCKED  │    │
          └──────────────────┘ └──────────────────┘    │
                    │                                   │
                    └───────────────────────────────────┘

              Transitions Allowed:
              • PENDING → APPROVED (via approve())
              • PENDING → REJECTED (via reject())
              
              Rejected:
              • APPROVED → anything (immutable)
              • REJECTED → anything (immutable)
              • REJECTED → PENDING (only if permanentBan=false)

              Note: permanentBan=true means driver.block(), 
                    no resubmission allowed
```

## State Machine: Driver Status (Integration)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DRIVER STATUS LIFECYCLE                            │
└─────────────────────────────────────────────────────────────────────────────┘

During Registration (HIST-2025-001):
┌──────────────────┐
│ PENDING_APPROVAL │ ◄─── Driver.new() starts here
└────────┬─────────┘
         │
         │ Approval workflow begins (HIST-2026-012)
         │

Admin Approves (ApproveDriverUseCase):
         │
         ▼
┌──────────────────┐
│     ACTIVE       │ ◄─── driver.activate()
└─────┬────────────┘      Ready to accept trips
      │
      │ Trip Assignment (HIST-2026-001):
      │
      ├──────────────────►┌──────────────────┐
      │                   │      BUSY        │
      │                   │ (On a trip)      │
      │                   └─────┬────────────┘
      │                         │
      │                    (Trip complete)
      │                         │
      │◄────────────────────────┘
      │ Back to ACTIVE
      │
      ▼
      ├──────────────────►┌──────────────────┐
      │                   │    INACTIVE      │
      │                   │ (Deactivated by  │
      │                   │  driver or system)
      │                   └──────────────────┘
      │
      └──────────────────►┌──────────────────┐
                          │     BLOCKED      │
                          │ (System ban or   │
                          │  permanent reject)
                          └──────────────────┘

Admin Rejects (RejectDriverUseCase):

From PENDING_APPROVAL:
         │
         ▼
    If permanentBan=true:
    ┌──────────────────┐
    │     BLOCKED      │ ◄─── driver.block()
    │ (Permanent ban)  │      No resubmission
    └──────────────────┘

    If permanentBan=false:
    ┌──────────────────┐
    │    INACTIVE      │ ◄─── driver.deactivate()
    │ (Can resubmit)   │      Driver can try again
    └────────┬─────────┘
             │
             │ Motorista resubmete documentação
             │ (SubmitDriverApprovalUseCase)
             │
             ▼
        PENDING_APPROVAL ◄─── Volta ao ciclo
```

## Class Diagram: Domain Layer

```
┌─────────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER (Pure)                          │
└─────────────────────────────────────────────────────────────────┘

                          ┌──────────────────┐
                          │   AdminRole      │
                          │  (Enum)          │
                          ├──────────────────┤
                          │ SUPER_ADMIN      │
                          │ COMPLIANCE_OFFICER
                          │ SUPPORT_ADMIN    │
                          └──────────────────┘
                                   △
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
    ┌───────────────────────────────────┐  ┌─────────────────┐
    │         AdminUser                 │  │ ApprovalStatus  │
    │      (Value Object)               │  │    (Enum)       │
    ├───────────────────────────────────┤  ├─────────────────┤
    │ - id: UUID                        │  │ PENDING         │
    │ - email: Email                    │  │ APPROVED        │
    │ - role: AdminRole ◄───────────────┼─ │ REJECTED        │
    │ - fullName: String                │  └─────────────────┘
    │ - createdAt: LocalDateTime        │
    ├───────────────────────────────────┤
    │ + canApproveDrivers(): boolean    │
    └───────────────────────────────────┘
                    △
                    │ used by
                    │
    ┌───────────────────────────────────────────────────────────┐
    │    DriverApprovalRequest (Aggregate Root)                │
    ├───────────────────────────────────────────────────────────┤
    │ - id: UUID                                                │
    │ - driverId: UUID                                          │
    │ - tenantId: TenantId                                      │
    │ - status: ApprovalStatus ◄──────────────────────┐        │
    │ - submittedDocuments: String (JSON)            │        │
    │ - submittedAt: LocalDateTime                   │        │
    │ - reviewedAt: LocalDateTime                    │        │
    │ - reviewedByAdminId: UUID                      │        │
    │ - rejectionReason: String                      │        │
    ├───────────────────────────────────────────────────────────┤
    │ + approve(adminId: UUID): void                            │
    │ + reject(adminId: UUID, reason: String): void            │
    │ + isPending(): boolean                                    │
    │ + isApproved(): boolean                                   │
    │ + isRejected(): boolean                                   │
    │ + isFinalized(): boolean                                  │
    └───────────────────────────────────────────────────────────┘
                    △ publishes events
                    │
        ┌───────────┼───────────┐
        │           │           │
    ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
    │DriverApproval- │  │DriverApproved- │  │DriverRejected-  │
    │SubmittedEvent  │  │ Event           │  │ Event           │
    ├─────────────────┤  ├─────────────────┤  ├─────────────────┤
    │ eventId         │  │ eventId         │  │ eventId         │
    │ driverId        │  │ driverId        │  │ driverId        │
    │ approvalId      │  │ approvalId      │  │ approvalId      │
    │ documentCount   │  │ approvedByAdmin │  │ rejectedByAdmin │
    │                 │  │ approverName    │  │ rejectionReason │
    │                 │  │                 │  │ permanentBan    │
    └─────────────────┘  └─────────────────┘  └─────────────────┘
             △                    △                    △
             └────────┬───────────┴────────────────────┘
                      │
                 Published via
                 Outbox Pattern
                 (HIST-2026-011)
```

## Repository Pattern: Ports & Adapters

```
┌────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                           │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Use Cases (depend on ports via constructor injection) │ │
│  │                                                         │ │
│  │  ApproveDriverUseCase {                                │ │
│  │    constructor(DriverApprovalRepositoryPort,          │ │
│  │               AdminUserRepositoryPort,                │ │
│  │               DriverRepositoryPort,                   │ │
│  │               NotificationPort,                       │ │
│  │               EventDispatcherPort)                    │ │
│  │  }                                                     │ │
│  └──────────────┬──────────────────────────────────────────┘ │
│                 │ depends on                                  │
│                 │                                             │
│   ┌─────────────┴────────────────┬────────────────────────┐  │
│   │                              │                        │  │
│   ▼                              ▼                        ▼  │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│  │DriverApprovalRepo  │  │AdminUserRepository  │  │DriverRepository    │
│  │ PositoryPort        │  │Port                 │  │Port                 │
│  │ (interface)         │  │ (interface)         │  │ (interface - exists)│
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘
│         △                          △                       △
│         │ implemented by           │ implemented by        │
│         │                          │                       │
└─────────┼──────────────────────────┼───────────────────────┘
          │                          │
          │                          │
          ▼                          ▼
┌──────────────────────────────────────────────────────────────────┐
│               INFRASTRUCTURE LAYER                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────┐  ┌────────────────────────────┐ │
│  │ JpaDriverApprovalRepository │  │ JpaAdminUserRepository    │ │
│  │ Adapter implements Port     │  │ Adapter implements Port   │ │
│  │                            │  │                          │ │
│  │ + DriverApprovalRequestJpa  │  │ + AdminUserJpaEntity    │ │
│  │   Entity (JPA mapping)      │  │   (JPA mapping)         │ │
│  │ + DriverApprovalRequest     │  │ + AdminUserMapper       │ │
│  │   Mapper (domain ↔ JPA)     │  │   (domain ↔ JPA)        │ │
│  │ + save()                    │  │ + findById()            │ │
│  │ + findById()                │  │ + findByEmail()         │ │
│  │ + findPendingByTenant()     │  │ + hasRole()             │ │
│  └────┬─────────────────────────┘  └────────┬────────────────┘ │
│       │                                      │                  │
│       └──────────┬───────────────────────────┘                  │
│                  │ persist data to                              │
│                  ▼                                              │
│        ┌──────────────────────────┐                            │
│        │    PostgreSQL Database   │                            │
│        │                          │                            │
│        │ Tables:                  │                            │
│        │ - driver_approval_reqs   │                            │
│        │ - admin_users            │                            │
│        │ - drivers                │                            │
│        │ - outbox_events          │                            │
│        └──────────────────────────┘                            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

Key Pattern:
- Ports (interfaces) in application layer
- Adapters (implementations) in infrastructure layer
- Use cases depend on ports, not concrete implementations
- Allows testing with mocks
- Easy to swap implementations (e.g., different databases)
```

## Component Diagram: Full System Integration

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          RappiDrive Backend System                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                     PRESENTATION LAYER (REST)                      │  │
│  │                                                                     │  │
│  │  ApprovalController {                                              │  │
│  │    GET  /api/approvals/pending                                     │  │
│  │    GET  /api/approvals/{id}                                        │  │
│  │    POST /api/approvals/{id}/approve                                │  │
│  │    POST /api/approvals/{id}/reject                                 │  │
│  │    GET  /api/drivers/{id}/approval-status                          │  │
│  │  }                                                                  │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                   │                                        │
│                                   ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                   APPLICATION LAYER (Use Cases)                    │  │
│  │                                                                     │  │
│  │  ┌──────────────────────────────────────────────────────────────┐ │  │
│  │  │ Use Cases                                                    │ │  │
│  │  │                                                              │ │  │
│  │  │  • SubmitDriverApprovalUseCase                              │ │  │
│  │  │  • ListPendingApprovalsUseCase                              │ │  │
│  │  │  • ApproveDriverUseCase                                     │ │  │
│  │  │  • RejectDriverUseCase                                      │ │  │
│  │  │                                                              │ │  │
│  │  └──────────────────────────────────────────────────────────────┘ │  │
│  │                                   │                               │  │
│  │           ┌───────────────────────┼───────────────────────┐      │  │
│  │           │                       │                       │      │  │
│  │           ▼                       ▼                       ▼      │  │
│  │  ┌──────────────────────┐┌──────────────────────┐┌──────────────┐ │  │
│  │  │Output Ports (ifaces) ││Output Ports (ifaces) ││Output Ports  │ │  │
│  │  │                      ││                      ││(ifaces)      │ │  │
│  │  │DriverApprovalRepo   ││AdminUserRepository  ││Notification  │ │  │
│  │  │PortDriver Repository││Port                 ││EventDispatch │ │  │
│  │  └──────────────────────┘└──────────────────────┘└──────────────┘ │  │
│  │           △                      △                       △         │  │
│  └───────────┼──────────────────────┼───────────────────────┼────────┘  │
│              │ implemented by       │ implemented by        │            │
│              │                      │                       │            │
│  ┌───────────┼──────────────────────┼───────────────────────┼────────┐  │
│  │ INFRASTRUCTURE LAYER (Adapters)                                   │  │
│  │           │                      │                       │        │  │
│  │           ▼                      ▼                       ▼        │  │
│  │  ┌──────────────────────┐┌──────────────────────┐┌──────────────┐ │  │
│  │  │JpaDriverApprovalRepo ││JpaAdminUserRepository││Notification  │ │  │
│  │  │Adapter               ││Adapter               ││Adapter       │ │  │
│  │  └──────────────────────┘└──────────────────────┘└──────────────┘ │  │
│  │           │                      │                       │        │  │
│  │           └──────────┬───────────┴───────────────────────┘        │  │
│  │                      │                                             │  │
│  │                      ▼                                             │  │
│  │          ┌───────────────────────┐                                │  │
│  │          │  PostgreSQL Database  │                                │  │
│  │          │                       │                                │  │
│  │          │ Tables:               │                                │  │
│  │          │ • drivers             │  (from HIST-2025-001)          │  │
│  │          │ • approval_requests   │  (NEW - this sprint)           │  │
│  │          │ • admin_users         │  (NEW - this sprint)           │  │
│  │          │ • outbox_events       │  (from HIST-2026-011)          │  │
│  │          │ • tenants             │                                │  │
│  │          └───────────────────────┘                                │  │
│  │                                                                     │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                      DOMAIN LAYER (Pure Logic)                      │  │
│  │                                                                     │  │
│  │  • DriverApprovalRequest (Aggregate)                                │  │
│  │  • AdminUser (Value Object)                                         │  │
│  │  • ApprovalStatus, AdminRole (Enums)                                │  │
│  │  • DriverApprovedEvent, DriverRejectedEvent (Domain Events)        │  │
│  │                                                                     │  │
│  │  (0 framework dependencies - pure Java)                             │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Data Migration Path

```
Event: Driver Registration (HIST-2025-001)
├─ Trigger: POST /api/drivers/register
└─ Result: Driver created with status = PENDING_APPROVAL
           DriverApprovalRequest created with status = PENDING
           │
           ▼
Event: Driver Approval Submission (HIST-2026-012)
├─ Trigger: POST /api/drivers/{id}/submit-approval
├─ Input: documentUrls (CNH, comprovante, etc.)
└─ Result: DriverApprovalRequest.submittedAt = now
           DriverApprovalRequest.status = PENDING
           DriverApprovalSubmittedEvent published to Outbox
           │
           ▼
Event: Admin Reviews Pending Approvals (HIST-2026-012)
├─ Trigger: GET /api/approvals/pending
├─ Query: SELECT * FROM driver_approval_requests
          WHERE status = 'PENDING' AND tenant_id = ?
          ORDER BY submitted_at ASC
└─ Result: Paginated list with driver info + docs
           │
           ├─ APPROVE PATH ──────────────┐
           │                             │
           ▼                             ▼
Event: Admin Approves Driver        Event: Admin Rejects Driver
├─ Trigger: POST .../approve        ├─ Trigger: POST .../reject
├─ Action:                          ├─ Input: rejectionReason
│  ├─ approvalRequest.approve()     │         permanentBan
│  ├─ driver.activate()             ├─ Action:
│  ├─ Save (TX)                     │  ├─ approvalRequest.reject()
│  ├─ Publish DriverApprovedEvent   │  ├─ if permanentBan:
│  └─ Send email: "Approved"        │  │    driver.block() → BLOCKED
└─ Result:                          │  │  else:
   │                                │  │    driver.deactivate() → INACTIVE
   │ Driver.status = ACTIVE         │  ├─ Save (TX)
   │ Can accept trips               │  ├─ Publish DriverRejectedEvent
   │                                │  └─ Send email: "Rejected: reason"
   │                                │
   │                                └─ Result:
   │                                   │
   │                                   ├─ Driver.status = INACTIVE/BLOCKED
   │                                   │
   │                                   └─ If INACTIVE:
   │                                      Motorista pode resubmeter docs
   │                                      (volta ao início)
   │
   └─ Event: Outbox Event Processor (HIST-2026-011)
      ├─ Trigger: @Scheduled(delay=1000ms)
      ├─ Action: Processa DriverApprovedEvent
      │          Notifica passengers, analytics, etc
      └─ Result: Event marked as PROCESSED in Outbox

Timeline:
┌────────┬──────────────┬────────────┬──────────┬─────────────┬─────────────┐
│ 00:00  │ 00:10        │ 00:20      │ 00:30    │ 00:35       │ 00:36       │
├────────┼──────────────┼────────────┼──────────┼─────────────┼─────────────┤
│Registra│Submete docs  │Admin sees  │Admin     │Motorista    │Event relay  │
│        │              │pending list│approves  │notificado   │processa     │
│        │              │            │          │             │             │
└────────┴──────────────┴────────────┴──────────┴─────────────┴─────────────┘
```

---

These visual diagrams help understand:
✅ Complete system flow
✅ State transitions
✅ Data persistence
✅ Event propagation
✅ Integration points
