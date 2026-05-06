package com.rappidrive.presentation.controllers;

import com.rappidrive.application.ports.input.passenger.*;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.presentation.dto.request.CreatePassengerRequest;
import com.rappidrive.presentation.dto.response.PassengerResponse;
import com.rappidrive.presentation.mappers.PassengerDtoMapper;
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

import java.util.UUID;

/**
 * REST controller for passenger management.
 */
@Tag(name = "Passengers", description = "Passenger management endpoints")
@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private static final Logger log = LoggerFactory.getLogger(PassengerController.class);
    
    private final CreatePassengerInputPort createPassengerUseCase;
    private final GetPassengerInputPort getPassengerUseCase;
    private final GetCurrentPassengerInputPort getCurrentPassengerUseCase;
    private final com.rappidrive.application.ports.output.CurrentUserPort currentUserPort;
    private final PassengerDtoMapper mapper;
    
    @Operation(summary = "Create a new passenger")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Passenger created successfully",
            content = @Content(schema = @Schema(implementation = PassengerResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing Keycloak identity"),
        @ApiResponse(responseCode = "409", description = "Passenger already exists with this email")
    })
    @PostMapping
    public ResponseEntity<PassengerResponse> createPassenger(@Valid @RequestBody CreatePassengerRequest request) {
        log.info("Creating passenger: email={}", request.email());
        
        String keycloakId = currentUserPort.getCurrentUser()
            .map(u -> u.userId().toString())
            .orElseThrow(() -> new IllegalStateException("Authenticated user required to create profile"));
            
        CreatePassengerInputPort.CreatePassengerCommand command = 
            new CreatePassengerInputPort.CreatePassengerCommand(
                mapper.toTenantId(request.tenantId()),
                keycloakId,
                request.fullName(),
                mapper.toEmail(request.email()),
                mapper.toPhone(request.phone())
            );
        
        Passenger passenger = createPassengerUseCase.execute(command);
        PassengerResponse response = mapper.toResponse(passenger);
        
        log.info("Passenger created successfully: id={}", passenger.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get current authenticated passenger profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Passenger profile found",
            content = @Content(schema = @Schema(implementation = PassengerResponse.class))),
        @ApiResponse(responseCode = "404", description = "Passenger profile not found")
    })
    @GetMapping("/me")
    public ResponseEntity<PassengerResponse> getMe(@RequestParam UUID tenantId) {
        log.info("Fetching current passenger profile for tenant: {}", tenantId);
        
        Passenger passenger = getCurrentPassengerUseCase.execute(mapper.toTenantId(tenantId));
        return ResponseEntity.ok(mapper.toResponse(passenger));
    }
    
    @Operation(summary = "Get passenger by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Passenger found",
            content = @Content(schema = @Schema(implementation = PassengerResponse.class))),
        @ApiResponse(responseCode = "404", description = "Passenger not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponse> getPassenger(@PathVariable UUID id) {
        log.info("Fetching passenger: id={}", id);
        
        Passenger passenger = getPassengerUseCase.execute(id);
        PassengerResponse response = mapper.toResponse(passenger);
        
        return ResponseEntity.ok(response);
    }
}
