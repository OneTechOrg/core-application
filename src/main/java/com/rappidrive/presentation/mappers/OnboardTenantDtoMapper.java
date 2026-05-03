package com.rappidrive.presentation.mappers;

import com.rappidrive.application.ports.input.tenant.OnboardNewTenantInputPort;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.presentation.dto.request.OnboardTenantRequest;
import com.rappidrive.presentation.dto.response.OnboardTenantResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper between OnboardTenant DTOs and domain/application objects.
 */
@Component
public class OnboardTenantDtoMapper {
    
    /**
     * Convert request DTO to use case command.
     */
    public OnboardNewTenantInputPort.OnboardingCommand toCommand(OnboardTenantRequest request) {
        return new OnboardNewTenantInputPort.OnboardingCommand(
                TenantId.fromString(request.tenantId().toString()),
                request.displayName(),
                new Email(request.adminEmail()),
                request.currency(),
                request.baseFare(),
                request.pricePerKm(),
                request.pricePerMin(),
                request.serviceAreaName(),
                request.geoJsonPolygon()
        );
    }
    
    /**
     * Convert use case result to response DTO.
     */
    public OnboardTenantResponse toResponse(OnboardNewTenantInputPort.OnboardingResult result) {
        return new OnboardTenantResponse(
                result.tenantId().getValue().toString(),
                result.displayName(),
                result.adminEmail(),
                result.keycloakUserId(),
                result.keycloakGroupId(),
                result.serviceAreaId(),
                result.fareConfigurationId(),
                result.message()
        );
    }
}
