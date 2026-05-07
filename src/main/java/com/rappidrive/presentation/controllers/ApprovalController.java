package com.rappidrive.presentation.controllers;

import com.rappidrive.application.ports.input.ApproveDriverInputPort;
import com.rappidrive.application.ports.input.ListPendingApprovalsInputPort;
import com.rappidrive.application.ports.input.RejectDriverInputPort;
import com.rappidrive.application.ports.input.SubmitDriverApprovalInputPort;
import com.rappidrive.presentation.dto.approval.*;
import com.rappidrive.presentation.mappers.approval.ApprovalDtoMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/approvals")
public class ApprovalController {

    private final SubmitDriverApprovalInputPort submitUseCase;
    private final ListPendingApprovalsInputPort listUseCase;
    private final ApproveDriverInputPort approveUseCase;
    private final RejectDriverInputPort rejectUseCase;
    private final ApprovalDtoMapper mapper;

    public ApprovalController(SubmitDriverApprovalInputPort submitUseCase,
                              ListPendingApprovalsInputPort listUseCase,
                              ApproveDriverInputPort approveUseCase,
                              RejectDriverInputPort rejectUseCase,
                              ApprovalDtoMapper mapper) {
        this.submitUseCase = submitUseCase;
        this.listUseCase = listUseCase;
        this.approveUseCase = approveUseCase;
        this.rejectUseCase = rejectUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/pending")
    public ResponseEntity<PendingApprovalsPageResponse> list(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            // Authenticated request - get adminId from JWT
            adminId = UUID.fromString(authentication.getName());
        }
        // For unauthenticated requests, adminId should be provided in param
        
        if (adminId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            var response = listUseCase.execute(new ListPendingApprovalsInputPort.ListPendingApprovalsCommand(adminId, page, size));
            return ResponseEntity.ok(mapper.toPage(response));
        } catch (Exception e) {
            var emptyResponse = new ListPendingApprovalsInputPort.ListPendingApprovalsResponse(List.of(), 0, page, size);
            return ResponseEntity.ok(mapper.toPage(emptyResponse));
        }
    }

    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<ApproveResponse> approve(@PathVariable UUID approvalId, @RequestBody ApproveRequest request) {
        UUID adminId = request.adminId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            adminId = UUID.fromString(authentication.getName());
        }
        
        var result = approveUseCase.execute(new ApproveDriverInputPort.ApproveDriverCommand(adminId, approvalId));
        return ResponseEntity.ok(new ApproveResponse(result.approvalId(), result.driverId(), result.status()));
    }

    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<RejectResponse> reject(@PathVariable UUID approvalId, @RequestBody RejectRequest request) {
        UUID adminId = request.adminId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            adminId = UUID.fromString(authentication.getName());
        }
        
        var result = rejectUseCase.execute(new RejectDriverInputPort.RejectDriverCommand(
            adminId,
            approvalId,
            request.rejectionReason(),
            request.permanentBan()
        ));
        return ResponseEntity.ok(new RejectResponse(result.approvalId(), result.driverId(), result.status(), result.rejectionReason()));
    }
}
