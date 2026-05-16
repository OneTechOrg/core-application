package com.rappidrive.presentation.controllers;

import com.rappidrive.application.ports.input.auth.RegisterUserInputPort;
import com.rappidrive.application.ports.input.auth.SendOtpInputPort;
import com.rappidrive.application.ports.input.auth.VerifyOtpInputPort;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.presentation.dto.request.OtpSendRequest;
import com.rappidrive.presentation.dto.request.OtpVerifyRequest;
import com.rappidrive.presentation.dto.request.RegisterUserRequest;
import com.rappidrive.presentation.dto.response.OtpSendResponse;
import com.rappidrive.presentation.dto.response.OtpVerifyResponse;
import com.rappidrive.presentation.dto.response.RegisterUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and user registration.
 */
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final SendOtpInputPort sendOtpUseCase;
    private final VerifyOtpInputPort verifyOtpUseCase;
    private final RegisterUserInputPort registerUserUseCase;

    @Operation(summary = "Send OTP to phone number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP sent successfully",
            content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or phone number format"),
        @ApiResponse(responseCode = "429", description = "Too many requests / Rate limit exceeded")
    })
    @PostMapping("/otp/send")
    public ResponseEntity<OtpSendResponse> sendOtp(
            @Valid @RequestBody OtpSendRequest request,
            HttpServletRequest servletRequest) {
        
        log.info("Request to send OTP to: {}", request.phone());
        
        SendOtpInputPort.Command command = new SendOtpInputPort.Command(
                request.phone(),
                servletRequest.getRemoteAddr()
        );
        
        SendOtpInputPort.SendOtpResult result = sendOtpUseCase.execute(command);
        
        log.info("OTP sent successfully to: {}", request.phone());
        return ResponseEntity.ok(new OtpSendResponse(result.expiresIn()));
    }

    @Operation(summary = "Verify OTP")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP verified successfully",
            content = @Content(schema = @Schema(implementation = OtpVerifyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid OTP or phone number"),
        @ApiResponse(responseCode = "401", description = "OTP verification failed")
    })
    @PostMapping("/otp/verify")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        
        log.info("Request to verify OTP for: {}", request.phone());
        
        VerifyOtpInputPort.Command command = new VerifyOtpInputPort.Command(
                request.phone(),
                request.code()
        );
        
        VerifyOtpInputPort.VerifyOtpResult result = verifyOtpUseCase.execute(command);
        
        log.info("OTP verified successfully for: {}", request.phone());
        return ResponseEntity.ok(new OtpVerifyResponse(result.phoneToken(), result.expiresIn()));
    }

    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = RegisterUserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid registration data or token"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(
            @Valid @RequestBody RegisterUserRequest request,
            @RequestHeader("X-Tenant-ID") String tenantIdHeader,
            HttpServletRequest servletRequest) {
        
        log.info("Request to register user: email={}, role={}", request.email(), request.role());
        
        RegisterUserInputPort.Command command = new RegisterUserInputPort.Command(
                request.fullName(),
                request.email(),
                request.password(),
                request.phone(),
                request.phoneToken(),
                request.role(),
                servletRequest.getRemoteAddr(),
                TenantId.fromString(tenantIdHeader)
        );
        
        RegisterUserInputPort.RegisterUserResult result = registerUserUseCase.execute(command);
        
        log.info("User registered successfully: id={}, email={}", result.userId(), result.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterUserResponse(result.userId(), result.email()));
    }
}
