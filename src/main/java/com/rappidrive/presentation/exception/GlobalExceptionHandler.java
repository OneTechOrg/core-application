package com.rappidrive.presentation.exception;

import com.rappidrive.application.exceptions.*;
import com.rappidrive.domain.exceptions.*;
import com.rappidrive.presentation.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDriverNotFound(DriverNotFoundException ex) {
        log.error("Driver not found: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Driver not found",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(PassengerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePassengerNotFound(PassengerNotFoundException ex) {
        log.error("Passenger not found: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Passenger not found",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTripNotFound(TripNotFoundException ex) {
        log.error("Trip not found: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Trip not found",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFound(VehicleNotFoundException ex) {
        log.error("Vehicle not found: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Vehicle not found",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlreadyExists(EntityAlreadyExistsException ex) {
        log.error("Entity already exists: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            "Entity already exists",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(InvalidPhoneTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPhoneToken(InvalidPhoneTokenException ex) {
        log.error("Invalid phone token: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid phone token",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(OtpRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleOtpRateLimitExceeded(OtpRateLimitExceededException ex) {
        log.error("OTP rate limit exceeded: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Rate limit exceeded",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(response);
    }
    
    @ExceptionHandler(OtpVerificationFailedException.class)
    public ResponseEntity<ErrorResponse> handleOtpVerificationFailed(OtpVerificationFailedException ex) {
        log.error("OTP verification failed: {}", ex.getMessage());
        
        HttpStatus status = ex.isMaxAttemptsReached() ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_REQUEST;
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            ex.isMaxAttemptsReached() ? "Rate limit exceeded" : "Verification failed",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(status).body(response);
    }
    
    @ExceptionHandler(InvalidDriverStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDriverState(InvalidDriverStateException ex) {
        log.error("Invalid driver state: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid driver state",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(InvalidPassengerStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassengerState(InvalidPassengerStateException ex) {
        log.error("Invalid passenger state: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid passenger state",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(InvalidTripStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTripState(InvalidTripStateException ex) {
        log.error("Invalid trip state: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid trip state",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(InvalidVehicleStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVehicleState(InvalidVehicleStateException ex) {
        log.error("Invalid vehicle state: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid vehicle state",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.ValidationError(
                error.getField(),
                error.getDefaultMessage()
            ))
            .collect(Collectors.toList());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            "Request validation failed. Please check the errors.",
            null,
            validationErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid argument",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException ex) {
        log.error("Application error: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Application error",
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            "An unexpected error occurred. Please try again later.",
            null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
