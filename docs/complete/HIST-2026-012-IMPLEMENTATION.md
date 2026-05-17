# HIST-2026-012: Implementation Checklist & Step-by-Step Guide

## Phase 1: Domain Layer Implementation (1.5 days)

### Step 1.1: Create Enums

#### ApprovalStatus.java
```java
package com.rappidrive.domain.enums;

/**
 * Status of a driver approval request.
 */
public enum ApprovalStatus {
    /**
     * Approval submitted, awaiting admin review.
     */
    PENDING,
    
    /**
     * Approval granted, driver activated.
     */
    APPROVED,
    
    /**
     * Approval rejected by admin.
     */
    REJECTED
}
```

**Location**: `src/main/java/com/rappidrive/domain/enums/ApprovalStatus.java`  
**Checklist**: 
- [ ] File created
- [ ] Javadoc added
- [ ] No Spring annotations

#### AdminRole.java
```java
package com.rappidrive.domain.enums;

/**
 * Role of an admin user in the system.
 */
public enum AdminRole {
    /**
     * Full system access, can approve/reject drivers.
     */
    SUPER_ADMIN,
    
    /**
     * Can analyze documentation and approve/reject drivers.
     */
    COMPLIANCE_OFFICER,
    
    /**
     * Read-only access, support only.
     */
    SUPPORT_ADMIN
}
```

**Location**: `src/main/java/com/rappidrive/domain/enums/AdminRole.java`  
**Checklist**:
- [ ] File created
- [ ] Javadoc added

### Step 1.2: Create Value Objects

#### AdminUser.java
```java
package com.rappidrive.domain.valueobjects;

import com.rappidrive.domain.enums.AdminRole;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

/**
 * Immutable value object representing an admin user.
 */
public final class AdminUser {
    private final UUID id;
    private final Email email;
    private final AdminRole role;
    private final String fullName;
    private final LocalDateTime createdAt;
    
    public AdminUser(UUID id, Email email, AdminRole role, String fullName, 
                     LocalDateTime createdAt) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        if (email == null) throw new IllegalArgumentException("Email cannot be null");
        if (role == null) throw new IllegalArgumentException("Role cannot be null");
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        if (createdAt == null) throw new IllegalArgumentException("CreatedAt cannot be null");
        
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName.trim();
        this.createdAt = createdAt;
    }
    
    // Getters (no setters - immutable)
    public UUID id() { return id; }
    public Email email() { return email; }
    public AdminRole role() { return role; }
    public String fullName() { return fullName; }
    public LocalDateTime createdAt() { return createdAt; }
    
    // Permission checks
    public boolean canApproveDrivers() {
        return role == AdminRole.SUPER_ADMIN || 
               role == AdminRole.COMPLIANCE_OFFICER;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminUser)) return false;
        AdminUser that = (AdminUser) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

**Location**: `src/main/java/com/rappidrive/domain/valueobjects/AdminUser.java`  
**Checklist**:
- [ ] File created
- [ ] Immutable (final class, final fields)
- [ ] Constructor validation
- [ ] equals/hashCode based on id
- [ ] Permission check methods added
- [ ] No Spring annotations

### Step 1.3: Create Aggregate Root

#### DriverApprovalRequest.java
```java
package com.rappidrive.domain.entities;

import com.rappidrive.domain.enums.ApprovalStatus;
import com.rappidrive.domain.exceptions.DomainException;
import com.rappidrive.domain.valueobjects.TenantId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for driver approval workflow.
 * Represents a request for admin approval of a driver's documentation.
 */
public class DriverApprovalRequest {
    
    private final UUID id;
    private final UUID driverId;
    private final TenantId tenantId;
    private ApprovalStatus status;
    private final String submittedDocuments;  // JSON string with doc URLs
    private final LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedByAdminId;
    private String rejectionReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Create new approval request (initial state: PENDING)
     */
    public DriverApprovalRequest(UUID id, UUID driverId, TenantId tenantId,
                                 String submittedDocuments) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        if (driverId == null) throw new IllegalArgumentException("Driver ID cannot be null");
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (submittedDocuments == null || submittedDocuments.isBlank()) {
            throw new IllegalArgumentException("Documents cannot be null or empty");
        }
        
        this.id = id;
        this.driverId = driverId;
        this.tenantId = tenantId;
        this.submittedDocuments = submittedDocuments;
        this.submittedAt = LocalDateTime.now();
        this.status = ApprovalStatus.PENDING;
        this.reviewedAt = null;
        this.reviewedByAdminId = null;
        this.rejectionReason = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Reconstruction constructor (for persistence layer)
     */
    public DriverApprovalRequest(UUID id, UUID driverId, TenantId tenantId,
                                 String submittedDocuments, ApprovalStatus status,
                                 LocalDateTime submittedAt, LocalDateTime reviewedAt,
                                 UUID reviewedByAdminId, String rejectionReason) {
        this.id = id;
        this.driverId = driverId;
        this.tenantId = tenantId;
        this.submittedDocuments = submittedDocuments;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedByAdminId = reviewedByAdminId;
        this.rejectionReason = rejectionReason;
        this.createdAt = LocalDateTime.now(); // Will be overridden by JPA
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Domain behavior: Approve this request.
     * Transitions from PENDING to APPROVED.
     * 
     * @param adminId UUID of approving admin
     * @throws DomainException if not in PENDING state
     */
    public void approve(UUID adminId) {
        if (status != ApprovalStatus.PENDING) {
            throw new DomainException(
                "Cannot approve request with status: " + status + 
                ". Only PENDING approvals can be approved.");
        }
        if (adminId == null) {
            throw new IllegalArgumentException("Admin ID cannot be null");
        }
        
        this.status = ApprovalStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;  // Clear any previous reason
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Domain behavior: Reject this request.
     * Transitions from PENDING to REJECTED.
     * 
     * @param adminId UUID of rejecting admin
     * @param reason Reason for rejection (required)
     * @throws DomainException if not in PENDING state
     * @throws IllegalArgumentException if reason is null/empty
     */
    public void reject(UUID adminId, String reason) {
        if (status != ApprovalStatus.PENDING) {
            throw new DomainException(
                "Cannot reject request with status: " + status + 
                ". Only PENDING approvals can be rejected.");
        }
        if (adminId == null) {
            throw new IllegalArgumentException("Admin ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be null or empty");
        }
        
        this.status = ApprovalStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason.trim();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Query methods
    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == ApprovalStatus.APPROVED;
    }
    
    public boolean isRejected() {
        return status == ApprovalStatus.REJECTED;
    }
    
    public boolean isFinalized() {
        return status != ApprovalStatus.PENDING;
    }
    
    // Getters
    public UUID id() { return id; }
    public UUID driverId() { return driverId; }
    public TenantId tenantId() { return tenantId; }
    public ApprovalStatus status() { return status; }
    public String submittedDocuments() { return submittedDocuments; }
    public LocalDateTime submittedAt() { return submittedAt; }
    public LocalDateTime reviewedAt() { return reviewedAt; }
    public UUID reviewedByAdminId() { return reviewedByAdminId; }
    public String rejectionReason() { return rejectionReason; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DriverApprovalRequest)) return false;
        DriverApprovalRequest that = (DriverApprovalRequest) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

**Location**: `src/main/java/com/rappidrive/domain/entities/DriverApprovalRequest.java`  
**Checklist**:
- [ ] File created
- [ ] Constructor validation (invariants)
- [ ] `approve()` method with state transition validation
- [ ] `reject()` method with reason requirement
- [ ] Query methods (`isPending()`, `isApproved()`, etc.)
- [ ] Reconstruction constructor for persistence
- [ ] equals/hashCode based on id
- [ ] No Spring annotations
- [ ] Comprehensive Javadoc

### Step 1.4: Create Domain Events

#### DriverApprovalSubmittedEvent.java
```java
package com.rappidrive.domain.events;

import com.rappidrive.domain.outbox.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DriverApprovalSubmittedEvent(
    UUID eventId,
    LocalDateTime occurredOn,
    UUID driverId,
    UUID approvalRequestId,
    int documentCount
) implements DomainEvent {}
```

#### DriverApprovedEvent.java
```java
package com.rappidrive.domain.events;

import com.rappidrive.domain.outbox.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DriverApprovedEvent(
    UUID eventId,
    LocalDateTime occurredOn,
    UUID driverId,
    UUID approvalRequestId,
    UUID approvedByAdminId,
    String approverName
) implements DomainEvent {}
```

#### DriverRejectedEvent.java
```java
package com.rappidrive.domain.events;

import com.rappidrive.domain.outbox.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DriverRejectedEvent(
    UUID eventId,
    LocalDateTime occurredOn,
    UUID driverId,
    UUID approvalRequestId,
    UUID rejectedByAdminId,
    String rejectionReason,
    boolean permanentBan
) implements DomainEvent {}
```

**Location**: `src/main/java/com/rappidrive/domain/events/Driver*Event.java`  
**Checklist**:
- [ ] All 3 events created
- [ ] Implement `DomainEvent` interface
- [ ] Include essential context (driverId, adminId, etc.)
- [ ] Records are immutable

---

## Phase 2: Application Layer (1 day)

### Step 2.1: Create Output Ports

#### DriverApprovalRepositoryPort.java
```java
package com.rappidrive.application.ports.output;

import com.rappidrive.domain.entities.DriverApprovalRequest;
import com.rappidrive.domain.enums.ApprovalStatus;
import com.rappidrive.domain.valueobjects.TenantId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for driver approval persistence operations.
 */
public interface DriverApprovalRepositoryPort {
    
    DriverApprovalRequest save(DriverApprovalRequest request);
    
    Optional<DriverApprovalRequest> findById(UUID id);
    
    Optional<DriverApprovalRequest> findByDriverId(UUID driverId);
    
    /**
     * Find pending approvals for a tenant with pagination.
     */
    PaginatedResult<DriverApprovalRequest> findPendingByTenant(
        TenantId tenantId, int pageNumber, int pageSize);
    
    /**
     * Count pending approvals for a tenant.
     */
    long countPendingByTenant(TenantId tenantId);
    
    /**
     * Find rejected approvals for a driver (for resubmission check).
     */
    List<DriverApprovalRequest> findRejectedByDriver(UUID driverId);
}

// Helper class
public record PaginatedResult<T>(
    List<T> items,
    long totalCount,
    int pageNumber,
    int pageSize
) {
    public long getTotalPages() {
        return (totalCount + pageSize - 1) / pageSize;
    }
}
```

#### AdminUserRepositoryPort.java
```java
package com.rappidrive.application.ports.output;

import com.rappidrive.domain.valueobjects.AdminUser;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for admin user operations.
 */
public interface AdminUserRepositoryPort {
    
    Optional<AdminUser> findById(UUID id);
    
    Optional<AdminUser> findByEmail(String email);
    
    /**
     * Check if admin has required role and is active.
     */
    boolean hasRole(UUID adminId, AdminRole requiredRole);
}
```

**Location**: `src/main/java/com/rappidrive/application/ports/output/`  
**Checklist**:
- [ ] `DriverApprovalRepositoryPort` created with all methods
- [ ] `AdminUserRepositoryPort` created
- [ ] `PaginatedResult` helper class
- [ ] Documented with Javadoc

### Step 2.2: Create Input Ports (Use Case Interfaces)

#### SubmitDriverApprovalInputPort.java
```java
package com.rappidrive.application.ports.input;

import java.util.List;
import java.util.UUID;

public interface SubmitDriverApprovalInputPort {
    
    SubmitDriverApprovalResponse execute(SubmitDriverApprovalCommand command);
    
    record SubmitDriverApprovalCommand(
        UUID driverId,
        List<String> documentUrls
    ) {}
    
    record SubmitDriverApprovalResponse(
        UUID approvalRequestId,
        String status
    ) {}
}
```

#### ListPendingApprovalsInputPort.java
```java
package com.rappidrive.application.ports.input;

import java.util.List;
import java.util.UUID;

public interface ListPendingApprovalsInputPort {
    
    ListPendingApprovalsResponse execute(ListPendingApprovalsCommand command);
    
    record ListPendingApprovalsCommand(
        UUID adminId,
        int pageNumber,
        int pageSize
    ) {}
    
    record ListPendingApprovalsResponse(
        List<PendingApprovalDto> approvals,
        long totalCount,
        int pageNumber,
        int pageSize
    ) {}
    
    record PendingApprovalDto(
        UUID approvalId,
        UUID driverId,
        String driverName,
        String driverEmail,
        LocalDateTime submittedAt,
        List<String> documents
    ) {}
}
```

#### ApproveDriverInputPort.java
```java
package com.rappidrive.application.ports.input;

import java.util.UUID;

public interface ApproveDriverInputPort {
    
    void execute(ApproveDriverCommand command);
    
    record ApproveDriverCommand(
        UUID approvalRequestId,
        UUID adminId,
        String notes
    ) {}
}
```

#### RejectDriverInputPort.java
```java
package com.rappidrive.application.ports.input;

import java.util.UUID;

public interface RejectDriverInputPort {
    
    void execute(RejectDriverCommand command);
    
    record RejectDriverCommand(
        UUID approvalRequestId,
        UUID adminId,
        String rejectionReason,
        boolean permanentBan
    ) {}
}
```

**Location**: `src/main/java/com/rappidrive/application/ports/input/`  
**Checklist**:
- [ ] All 4 input port interfaces created
- [ ] DTOs/records with clear field names
- [ ] Javadoc for each port

### Step 2.3: Implement Use Cases

#### SubmitDriverApprovalUseCase.java
(See `docs/HIST-2026-012-ARCHITECTURE.md` Section "Use Case: ApproveDriverUseCase" for pattern)

#### ApproveDriverUseCase.java
(See detailed implementation in Architecture document)

#### RejectDriverUseCase.java
#### ListPendingApprovalsUseCase.java

**Location**: `src/main/java/com/rappidrive/application/usecases/approval/`  
**Checklist**:
- [ ] All 4 use cases implemented
- [ ] Constructor injection of ports
- [ ] Transactional (@Transactional)
- [ ] Domain events published
- [ ] Comprehensive error handling
- [ ] Tests written (unit tests with mocks)

---

## Phase 3: Infrastructure Layer (1 day)

### Step 3.1: Create JPA Entities & Mappers

#### DriverApprovalRequestJpaEntity.java
```java
package com.rappidrive.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_approval_requests", indexes = {
    @Index(name = "idx_status_tenant", columnList = "status,tenant_id"),
    @Index(name = "idx_driver_id", columnList = "driver_id"),
    @Index(name = "idx_tenant_created", columnList = "tenant_id,created_at DESC")
})
public class DriverApprovalRequestJpaEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;
    
    @Column(name = "submitted_documents", nullable = false, columnDefinition = "jsonb")
    private String submittedDocuments;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "reviewed_by_admin_id")
    private UUID reviewedByAdminId;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Getters & setters (or use Lombok @Getter @Setter)
}
```

#### AdminUserJpaEntity.java
```java
package com.rappidrive.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_role_tenant", columnList = "role,tenant_id")
})
public class AdminUserJpaEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private AdminRole role;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

#### DriverApprovalRequestMapper.java
```java
package com.rappidrive.infrastructure.persistence.mappers;

import com.rappidrive.domain.entities.DriverApprovalRequest;
import com.rappidrive.infrastructure.persistence.entities.DriverApprovalRequestJpaEntity;
// ... etc
```

**Location**: `src/main/java/com/rappidrive/infrastructure/persistence/`  
**Checklist**:
- [ ] JPA entities created with `@Entity`, `@Table`, `@Column`
- [ ] Indexes configured for performance
- [ ] Mappers created (domain ↔ JPA)
- [ ] Converters for value objects (Email, TenantId)

### Step 3.2: Create Repository Adapters

#### JpaDriverApprovalRepositoryAdapter.java
```java
package com.rappidrive.infrastructure.persistence.adapters;

import com.rappidrive.application.ports.output.DriverApprovalRepositoryPort;
import com.rappidrive.domain.entities.DriverApprovalRequest;
// ... implementation
```

#### JpaAdminUserRepositoryAdapter.java
```java
package com.rappidrive.infrastructure.persistence.adapters;

import com.rappidrive.application.ports.output.AdminUserRepositoryPort;
import com.rappidrive.domain.valueobjects.AdminUser;
// ... implementation
```

**Location**: `src/main/java/com/rappidrive/infrastructure/persistence/adapters/`  
**Checklist**:
- [ ] Adapters implement ports
- [ ] Mappers used to convert JPA ↔ domain
- [ ] Queries optimized with indexes
- [ ] Tests written (@DataJpaTest)

### Step 3.3: Create Bean Configuration

#### ApprovalBeanConfiguration.java
```java
package com.rappidrive.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
// ... wire all use cases and adapters
```

**Checklist**:
- [ ] All use cases wired as beans
- [ ] All adapters wired as beans
- [ ] Ports injected correctly

### Step 3.4: Create Database Migration

#### V.X.X__Create_approval_tables.sql
(See Architecture document for full SQL)

**Location**: `src/main/resources/db/migration/`  
**Checklist**:
- [ ] Migration file created
- [ ] Tables with correct columns and types
- [ ] Foreign keys configured
- [ ] Indexes created
- [ ] Check constraints for enums

---

## Phase 4: Presentation Layer (1 day)

### Step 4.1: Create REST Controller

#### ApprovalController.java
```java
package com.rappidrive.presentation.controllers;

import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
    
    private final ListPendingApprovalsInputPort listPending;
    private final ApproveDriverInputPort approveDriver;
    private final RejectDriverInputPort rejectDriver;
    
    @GetMapping("/pending")
    public ResponseEntity<PendingApprovalsResponse> listPending(
        @RequestParam int page,
        @RequestParam int size,
        @CurrentUser UUID adminId) {
        // ... implementation
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
        @PathVariable UUID id,
        @RequestBody ApproveApprovalRequest request,
        @CurrentUser UUID adminId) {
        // ... implementation
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
        @PathVariable UUID id,
        @RequestBody RejectApprovalRequest request,
        @CurrentUser UUID adminId) {
        // ... implementation
    }
}
```

### Step 4.2: Create DTOs

#### ApproveApprovalRequest.java
#### RejectApprovalRequest.java
#### DriverApprovalResponse.java
#### AdminUserResponse.java

**Location**: `src/main/java/com/rappidrive/presentation/dto/`  
**Checklist**:
- [ ] All DTOs created
- [ ] Validation annotations (@NotNull, @NotBlank, etc.)
- [ ] Javadoc

### Step 4.3: Create Exception Handlers

#### ApprovalExceptionHandler.java
(Add to existing `GlobalExceptionHandler` or create new)

**Checklist**:
- [ ] Handler for `ApprovalRequestNotFoundException`
- [ ] Handler for `AdminUnauthorizedException`
- [ ] Handler for `InvalidApprovalStateException`
- [ ] Returns proper HTTP status codes

---

## Phase 5: Testing Strategy

### Unit Tests

#### DriverApprovalRequestTest.java
- Test state transitions (PENDING → APPROVED)
- Test state transitions (PENDING → REJECTED)
- Test invariants (cannot approve if not PENDING)
- Test rejection reason validation

#### ApproveDriverUseCaseTest.java
- Mock all ports
- Test happy path
- Test role validation (SUPPORT_ADMIN cannot approve)
- Test driver not found error
- Test transactional consistency

#### RejectDriverUseCaseTest.java
- Test rejection with reason
- Test permanent ban logic
- Test driver status updates

### Integration Tests

#### DriverApprovalRequestRepositoryTest.java (@DataJpaTest)
- Test save and retrieve
- Test queries with filters
- Test pagination

#### ApprovalControllerE2ETest.java (@SpringBootTest)
- Test full flow: submit → list → approve → driver ACTIVE
- Test rejection flow: submit → list → reject → driver INACTIVE
- Test permission checks (403 for SUPPORT_ADMIN)
- Test tenant isolation

---

## Summary Checklist

### Domain Layer ✅
- [ ] ApprovalStatus enum
- [ ] AdminRole enum
- [ ] AdminUser value object
- [ ] DriverApprovalRequest aggregate
- [ ] DriverApprovalSubmittedEvent
- [ ] DriverApprovedEvent
- [ ] DriverRejectedEvent

### Application Layer ✅
- [ ] DriverApprovalRepositoryPort
- [ ] AdminUserRepositoryPort
- [ ] SubmitDriverApprovalInputPort
- [ ] ListPendingApprovalsInputPort
- [ ] ApproveDriverInputPort
- [ ] RejectDriverInputPort
- [ ] 4 Use case implementations

### Infrastructure Layer ✅
- [ ] DriverApprovalRequestJpaEntity
- [ ] AdminUserJpaEntity
- [ ] DriverApprovalRequestMapper
- [ ] AdminUserMapper
- [ ] JpaDriverApprovalRepositoryAdapter
- [ ] JpaAdminUserRepositoryAdapter
- [ ] Database migration
- [ ] ApprovalBeanConfiguration

### Presentation Layer ✅
- [ ] ApprovalController
- [ ] ApproveApprovalRequest DTO
- [ ] RejectApprovalRequest DTO
- [ ] DriverApprovalResponse DTO
- [ ] AdminUserResponse DTO
- [ ] Exception handlers

### Testing ✅
- [ ] Unit tests for DriverApprovalRequest
- [ ] Unit tests for each use case (4)
- [ ] Integration tests for repositories (2)
- [ ] E2E tests for controller (full flows)
- [ ] Security tests (RBAC)
- [ ] Multi-tenancy tests

### Documentation ✅
- [ ] Javadoc in all public classes
- [ ] README update with API endpoints
- [ ] Architecture decisions documented
- [ ] Performance considerations documented

---

## Next Steps After Implementation

1. **Merge to main** after all tests pass
2. **Update HIST-2026-012.md** status from PROPOSTO → IMPLEMENTADO
3. **Deploy to staging** for manual testing
4. **Monitor metrics**: approval times, rejection rates, admin activity
5. **Plan HIST-2026-013** (future): Email templates, webhook notifications, background check integration

---

Good luck with the implementation! 🚀
