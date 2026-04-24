package com.rappidrive.presentation.controllers;

import com.rappidrive.application.ports.input.notification.GetUnreadCountInputPort;
import com.rappidrive.application.ports.input.notification.GetUnreadCountInputPort.GetUnreadCountQuery;
import com.rappidrive.application.ports.input.notification.GetUserNotificationsInputPort;
import com.rappidrive.application.ports.input.notification.GetUserNotificationsInputPort.GetUserNotificationsQuery;
import com.rappidrive.application.ports.input.notification.MarkNotificationAsReadInputPort;
import com.rappidrive.application.ports.input.notification.MarkNotificationAsReadInputPort.MarkAsReadCommand;
import com.rappidrive.application.ports.input.notification.SendNotificationInputPort;
import com.rappidrive.domain.entities.Notification;
import com.rappidrive.domain.enums.NotificationStatus;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.domain.valueobjects.UserId;
import com.rappidrive.presentation.dto.request.SendNotificationRequest;
import com.rappidrive.presentation.dto.response.NotificationResponse;
import com.rappidrive.presentation.dto.response.UnreadCountResponse;
import com.rappidrive.presentation.mappers.NotificationDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications", description = "User notification endpoints")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    private final SendNotificationInputPort sendNotificationUseCase;
    private final GetUserNotificationsInputPort getUserNotificationsUseCase;
    private final MarkNotificationAsReadInputPort markAsReadUseCase;
    private final GetUnreadCountInputPort getUnreadCountUseCase;
    private final NotificationDtoMapper mapper;
    
    public NotificationController(
            SendNotificationInputPort sendNotificationUseCase,
            GetUserNotificationsInputPort getUserNotificationsUseCase,
            MarkNotificationAsReadInputPort markAsReadUseCase,
            GetUnreadCountInputPort getUnreadCountUseCase,
            NotificationDtoMapper mapper) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.getUserNotificationsUseCase = getUserNotificationsUseCase;
        this.markAsReadUseCase = markAsReadUseCase;
        this.getUnreadCountUseCase = getUnreadCountUseCase;
        this.mapper = mapper;
    }
    
    /**
     * Envia notificação (admin/sistema)
     * POST /api/v1/notifications
     */
    @Operation(summary = "Send a notification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Notification sent",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        TenantId tenant = new TenantId(tenantId);
        Notification notification = sendNotificationUseCase.execute(mapper.toCommand(request, tenant));
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapper.toNotificationResponse(notification));
    }
    
    /**
     * Lista notificações do usuário logado
     * GET /api/v1/notifications?status=UNREAD&page=0&size=20
     */
    @Operation(summary = "List user notifications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications listed")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        NotificationStatus notificationStatus = status != null 
            ? NotificationStatus.valueOf(status) 
            : null;
        
        GetUserNotificationsQuery query = new GetUserNotificationsQuery(
            new UserId(userId),
            notificationStatus,
            new TenantId(tenantId),
            page,
            size
        );
        
        List<Notification> notifications = getUserNotificationsUseCase.execute(query);
        
        return ResponseEntity.ok(mapper.toNotificationResponseList(notifications));
    }
    
    /**
     * Contador de notificações não lidas
     * GET /api/v1/notifications/unread-count
     */
    @Operation(summary = "Get unread notification count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Unread count returned",
            content = @Content(schema = @Schema(implementation = UnreadCountResponse.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        GetUnreadCountQuery query = new GetUnreadCountQuery(
            new UserId(userId),
            new TenantId(tenantId)
        );
        
        Long count = getUnreadCountUseCase.execute(query);
        
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }
    
    /**
     * Marca notificação como lida
     * PATCH /api/v1/notifications/{id}/read
     */
    @Operation(summary = "Mark notification as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Notification marked as read"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        MarkAsReadCommand command = new MarkAsReadCommand(
            id,
            new UserId(userId),
            new TenantId(tenantId)
        );
        
        markAsReadUseCase.execute(command);
        
        return ResponseEntity.noContent().build();
    }
}
