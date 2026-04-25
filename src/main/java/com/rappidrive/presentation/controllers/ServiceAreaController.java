package com.rappidrive.presentation.controllers;

import com.rappidrive.application.ports.input.servicearea.CreateServiceAreaInputPort;
import com.rappidrive.application.ports.input.servicearea.GetServiceAreaInputPort;
import com.rappidrive.application.ports.input.servicearea.ListServiceAreasInputPort;
import com.rappidrive.application.ports.input.servicearea.ListServiceAreasInputPort.ListServiceAreasQuery;
import com.rappidrive.application.ports.input.servicearea.ToggleServiceAreaStatusInputPort;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.presentation.dto.request.CreateServiceAreaRequest;
import com.rappidrive.presentation.dto.response.ServiceAreaResponse;
import com.rappidrive.presentation.mappers.ServiceAreaDtoMapper;
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

@Tag(name = "Service Areas", description = "Tenant operating zone management")
@SecurityRequirement(name = "bearer-jwt")
@RestController
public class ServiceAreaController {

    private final GetServiceAreaInputPort getServiceAreaUseCase;
    private final ListServiceAreasInputPort listServiceAreasUseCase;
    private final CreateServiceAreaInputPort createServiceAreaUseCase;
    private final ToggleServiceAreaStatusInputPort toggleStatusUseCase;
    private final ServiceAreaDtoMapper mapper;

    public ServiceAreaController(GetServiceAreaInputPort getServiceAreaUseCase,
                                 ListServiceAreasInputPort listServiceAreasUseCase,
                                 CreateServiceAreaInputPort createServiceAreaUseCase,
                                 ToggleServiceAreaStatusInputPort toggleStatusUseCase,
                                 ServiceAreaDtoMapper mapper) {
        this.getServiceAreaUseCase = getServiceAreaUseCase;
        this.listServiceAreasUseCase = listServiceAreasUseCase;
        this.createServiceAreaUseCase = createServiceAreaUseCase;
        this.toggleStatusUseCase = toggleStatusUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "List all service areas for the current tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service areas listed")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/service-areas")
    public ResponseEntity<List<ServiceAreaResponse>> listAll(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        List<ServiceArea> areas = listServiceAreasUseCase.execute(
            new ListServiceAreasQuery(new TenantId(tenantId), false));
        return ResponseEntity.ok(mapper.toResponseList(areas));
    }

    @Operation(summary = "List active service areas for the current tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active service areas listed")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/service-areas/active")
    public ResponseEntity<List<ServiceAreaResponse>> listActive(
            @RequestHeader("X-Tenant-ID") UUID tenantId) {
        List<ServiceArea> areas = listServiceAreasUseCase.execute(
            new ListServiceAreasQuery(new TenantId(tenantId), true));
        return ResponseEntity.ok(mapper.toResponseList(areas));
    }

    @Operation(summary = "Get a service area by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service area found",
            content = @Content(schema = @Schema(implementation = ServiceAreaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Service area not found")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/service-areas/{id}")
    public ResponseEntity<ServiceAreaResponse> getById(@PathVariable UUID id) {
        ServiceArea area = getServiceAreaUseCase.execute(id);
        return ResponseEntity.ok(mapper.toResponse(area));
    }

    @Operation(summary = "Create a new service area (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Service area created",
            content = @Content(schema = @Schema(implementation = ServiceAreaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/service-areas")
    public ResponseEntity<ServiceAreaResponse> create(
            @Valid @RequestBody CreateServiceAreaRequest request) {
        ServiceArea area = createServiceAreaUseCase.execute(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(area));
    }

    @Operation(summary = "Deactivate a service area (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service area deactivated",
            content = @Content(schema = @Schema(implementation = ServiceAreaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Service area not found"),
        @ApiResponse(responseCode = "409", description = "Service area already inactive")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/admin/service-areas/{id}/deactivate")
    public ResponseEntity<ServiceAreaResponse> deactivate(@PathVariable UUID id) {
        ServiceArea area = toggleStatusUseCase.deactivate(id);
        return ResponseEntity.ok(mapper.toResponse(area));
    }

    @Operation(summary = "Activate a service area (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service area activated",
            content = @Content(schema = @Schema(implementation = ServiceAreaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Service area not found"),
        @ApiResponse(responseCode = "409", description = "Service area already active")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/admin/service-areas/{id}/activate")
    public ResponseEntity<ServiceAreaResponse> activate(@PathVariable UUID id) {
        ServiceArea area = toggleStatusUseCase.activate(id);
        return ResponseEntity.ok(mapper.toResponse(area));
    }
}
