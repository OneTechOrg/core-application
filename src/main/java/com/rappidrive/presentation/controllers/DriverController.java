package com.rappidrive.presentation.controllers;

import com.rappidrive.application.ports.input.driver.*;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.presentation.dto.request.CreateDriverRequest;
import com.rappidrive.presentation.dto.request.UpdateDriverLocationRequest;
import com.rappidrive.presentation.dto.response.DriverResponse;
import com.rappidrive.presentation.mappers.DriverDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Drivers", description = "Driver management endpoints")
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private static final Logger log = LoggerFactory.getLogger(DriverController.class);
    
    private final CreateDriverInputPort createDriverUseCase;
    private final GetDriverInputPort getDriverUseCase;
    private final ActivateDriverInputPort activateDriverUseCase;
    private final UpdateDriverLocationInputPort updateDriverLocationUseCase;
    private final FindAvailableDriversInputPort findAvailableDriversUseCase;
    private final DriverDtoMapper mapper;
    
    @Operation(summary = "Create a new driver")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Driver created successfully",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Driver already exists with this email or CPF")
    })
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        String maskedEmail = request.email().replaceAll("(^.{3})[^@]*", "$1***");
        String maskedCpf = "***.***.***-" + request.cpf().substring(request.cpf().length() - 2);
        log.info("Creating driver: email={}, cpf={}", maskedEmail, maskedCpf);
        
        CreateDriverInputPort.CreateDriverCommand command = new CreateDriverInputPort.CreateDriverCommand(
            mapper.toTenantId(request.tenantId()),
            request.fullName(),
            mapper.toEmail(request.email()),
            mapper.toCPF(request.cpf()),
            mapper.toPhone(request.phone()),
            mapper.toDriverLicense(request.driverLicense())
        );
        
        Driver driver = createDriverUseCase.execute(command);
        DriverResponse response = mapper.toResponse(driver);
        
        log.info("Driver created successfully: id={}", driver.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Get driver by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Driver found",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
        @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable UUID id) {
        log.info("Fetching driver: id={}", id);
        
        Driver driver = getDriverUseCase.execute(id);
        DriverResponse response = mapper.toResponse(driver);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Activate driver")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Driver activated successfully",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
        @ApiResponse(responseCode = "404", description = "Driver not found"),
        @ApiResponse(responseCode = "409", description = "Invalid driver state for activation")
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<DriverResponse> activateDriver(@PathVariable UUID id) {
        log.info("Activating driver: id={}", id);
        
        Driver driver = activateDriverUseCase.execute(id);
        DriverResponse response = mapper.toResponse(driver);
        
        log.info("Driver activated successfully: id={}", id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update driver location")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Location updated successfully",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid location data"),
        @ApiResponse(responseCode = "404", description = "Driver not found"),
        @ApiResponse(responseCode = "409", description = "Driver is not active")
    })
    @PutMapping("/{id}/location")
    public ResponseEntity<DriverResponse> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDriverLocationRequest request) {
        log.info("Updating driver location: id={}, location=({}, {})",
            id, request.location().latitude(), request.location().longitude());
        
        UpdateDriverLocationInputPort.UpdateLocationCommand command = 
            new UpdateDriverLocationInputPort.UpdateLocationCommand(
                id,
                mapper.toLocation(request.location())
            );
        
        Driver driver = updateDriverLocationUseCase.execute(command);
        DriverResponse response = mapper.toResponse(driver);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Find available drivers near a location")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Drivers found successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid location or radius parameters")
    })
    @GetMapping("/nearby")
    public ResponseEntity<?> findNearbyDrivers(
            @RequestParam UUID tenantId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        log.info("Finding drivers near location: lat={}, lon={}, radius={}km, tenant={}",
            latitude, longitude, radiusKm, tenantId);
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            mapper.toTenantId(tenantId),
            mapper.toLocation(latitude, longitude),
            radiusKm
        );
        
        var drivers = findAvailableDriversUseCase.execute(command);
        var responses = drivers.stream()
            .map(mapper::toResponse)
            .toList();
        
        log.info("Found {} available drivers", responses.size());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableDrivers() {
        log.info("Getting list of available drivers");
        
        return ResponseEntity.ok(List.of());
    }
}
