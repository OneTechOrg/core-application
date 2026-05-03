package com.rappidrive.presentation.controllers.admin;

import com.rappidrive.application.ports.input.payment.GetFareConfigurationInputPort;
import com.rappidrive.application.ports.input.payment.UpdateFareConfigurationInputPort;
import com.rappidrive.domain.entities.FareConfiguration;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.presentation.dto.request.UpdateFareConfigurationRequest;
import com.rappidrive.presentation.dto.response.FareConfigurationResponse;
import com.rappidrive.presentation.mappers.FareConfigurationDtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for fare configuration management (Admin area).
 * Should be protected with admin-level authorization.
 */
@RestController
@RequestMapping("/api/v1/admin/fare-configurations")
@PreAuthorize("hasRole('ADMIN')")
public class FareConfigurationController {
    
    private final GetFareConfigurationInputPort getFareConfigurationUseCase;
    private final UpdateFareConfigurationInputPort updateFareConfigurationUseCase;
    private final FareConfigurationDtoMapper mapper;
    
    public FareConfigurationController(
            GetFareConfigurationInputPort getFareConfigurationUseCase,
            UpdateFareConfigurationInputPort updateFareConfigurationUseCase,
            FareConfigurationDtoMapper mapper) {
        this.getFareConfigurationUseCase = getFareConfigurationUseCase;
        this.updateFareConfigurationUseCase = updateFareConfigurationUseCase;
        this.mapper = mapper;
    }
    
    /**
     * GET /api/v1/admin/fare-configurations/tenant/{tenantId} - Gets fare configuration by tenant
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<FareConfigurationResponse> getFareConfiguration(@PathVariable UUID tenantId) {
        FareConfiguration fareConfig = getFareConfigurationUseCase.execute(new TenantId(tenantId));
        return ResponseEntity.ok(mapper.toResponse(fareConfig));
    }
    
    /**
     * PUT /api/v1/admin/fare-configurations - Updates (or creates) fare configuration
     */
    @PutMapping
    public ResponseEntity<FareConfigurationResponse> updateFareConfiguration(
            @Valid @RequestBody UpdateFareConfigurationRequest request) {
        FareConfiguration fareConfig = updateFareConfigurationUseCase.execute(mapper.toCommand(request));
        return ResponseEntity.ok(mapper.toResponse(fareConfig));
    }
}
