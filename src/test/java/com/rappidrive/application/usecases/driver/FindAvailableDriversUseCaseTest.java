package com.rappidrive.application.usecases.driver;

import com.rappidrive.application.metrics.DriverAssignmentAttemptStatus;
import com.rappidrive.application.metrics.DriverAssignmentStage;
import com.rappidrive.application.ports.input.driver.FindAvailableDriversCommand;
import com.rappidrive.application.ports.output.DriverAssignmentMetricsPort;
import com.rappidrive.application.ports.output.DriverGeoQueryPort;
import com.rappidrive.application.ports.output.TelemetryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.valueobjects.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FindAvailableDriversUseCase.
 */
@ExtendWith(MockitoExtension.class)
class FindAvailableDriversUseCaseTest {
    
    @Mock
    private DriverGeoQueryPort driverGeoQueryPort;
    
    private ExecutorService executor;
    private FindAvailableDriversUseCase useCase;
    private TelemetryPort telemetryPort;
    private DriverAssignmentMetricsPort metricsPort;
    
    private TenantId tenantId;
    private Location pickupLocation;
    private Driver activeDriver;
    private Driver inactiveDriver;
    private Driver driverWithoutLocation;
    private Driver driverWithExpiredCNH;
    
    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        telemetryPort = new TelemetryPort() {
            @Override
            public <T> T traceUseCase(String useCaseName, Map<String, String> attributes, java.util.function.Supplier<T> supplier) {
                return supplier.get();
            }
        };

        metricsPort = new DriverAssignmentMetricsPort() {
            @Override
            public void recordStageDuration(DriverAssignmentStage stage, long durationMillis) {}

            @Override
            public void incrementAttempts(DriverAssignmentStage stage, DriverAssignmentAttemptStatus status) {}

            @Override
            public void incrementQueue(String tenantId) {}

            @Override
            public void decrementQueue(String tenantId) {}
        };

        useCase = new FindAvailableDriversUseCase(driverGeoQueryPort, executor, telemetryPort, metricsPort);
        
        tenantId = TenantId.generate();
        pickupLocation = new Location(-23.550520, -46.633308);
        
        // Active driver with valid location and CNH
        DriverLicense validLicense = new DriverLicense(
            "12345678901", "B",
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2030, 1, 1),
            true
        );
        activeDriver = new Driver(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            tenantId,
            "Active Driver",
            new Email("active@example.com"),
            new CPF("12345678909"),
            new Phone("+5511987654321"),
            validLicense,
            DriverStatus.ACTIVE,
            new Location(-23.551000, -46.634000)
        );
        
        // Inactive driver (should be filtered out)
        inactiveDriver = new Driver(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            tenantId,
            "Inactive Driver",
            new Email("inactive@example.com"),
            new CPF("98765432100"),
            new Phone("+5511999999999"),
            validLicense,
            DriverStatus.INACTIVE,
            new Location(-23.552000, -46.635000)
        );
        
        // Driver without location (should be filtered out)
        driverWithoutLocation = new Driver(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            tenantId,
            "No Location Driver",
            new Email("noloc@example.com"),
            new CPF("52998224725"),
            new Phone("+5511988888888"),
            validLicense,
            DriverStatus.ACTIVE,
            null
        );
        
        // Driver with expired CNH (should be filtered out)
        DriverLicense expiredLicense = new DriverLicense(
            "99999999999", "B",
            LocalDate.of(2010, 1, 1),
            LocalDate.of(2020, 1, 1),
            true
        );
        driverWithExpiredCNH = new Driver(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            tenantId,
            "Expired CNH Driver",
            new Email("expired@example.com"),
            new CPF("71428793860"),
            new Phone("+5511977777777"),
            expiredLicense,
            DriverStatus.ACTIVE,
            new Location(-23.553000, -46.636000)
        );
    }
    
    @Test
    void shouldFindAvailableDriversWithinRadius() {
        // Given
        List<Driver> driversNearby = List.of(activeDriver, inactiveDriver, driverWithoutLocation);
        when(driverGeoQueryPort.findAvailableDriversNearby(any(Location.class), anyDouble(), eq(tenantId)))
            .thenReturn(driversNearby);
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            tenantId, pickupLocation, 5.0
        );
        
        // When
        List<Driver> result = useCase.execute(command);
        
        // Then
        assertThat(result).contains(activeDriver);
        verify(driverGeoQueryPort, atLeastOnce()).findAvailableDriversNearby(any(Location.class), anyDouble(), eq(tenantId));
    }
    
    @Test
    void shouldFilterOutInactiveDrivers() {
        // Given
        when(driverGeoQueryPort.findAvailableDriversNearby(any(), anyDouble(), any()))
            .thenReturn(List.of(inactiveDriver));
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            tenantId, pickupLocation, 5.0
        );
        
        // When
        List<Driver> result = useCase.execute(command);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldFilterOutDriversWithoutLocation() {
        // Given
        when(driverGeoQueryPort.findAvailableDriversNearby(any(), anyDouble(), any()))
            .thenReturn(List.of(driverWithoutLocation));
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            tenantId, pickupLocation, 5.0
        );
        
        // When
        List<Driver> result = useCase.execute(command);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldFilterOutDriversWithExpiredCNH() {
        // Given
        when(driverGeoQueryPort.findAvailableDriversNearby(any(), anyDouble(), any()))
            .thenReturn(List.of(driverWithExpiredCNH));
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            tenantId, pickupLocation, 5.0
        );
        
        // When
        List<Driver> result = useCase.execute(command);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldReturnEmptyListWhenNoDriversNearby() {
        // Given
        when(driverGeoQueryPort.findAvailableDriversNearby(any(), anyDouble(), any()))
            .thenReturn(List.of());
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            tenantId, pickupLocation, 5.0
        );
        
        // When
        List<Driver> result = useCase.execute(command);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldUseCustomRadius() {
        // Given
        double customRadius = 10.0;
        when(driverGeoQueryPort.findAvailableDriversNearby(any(Location.class), anyDouble(), eq(tenantId)))
            .thenReturn(List.of(activeDriver));
        
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(
            tenantId, pickupLocation, customRadius
        );
        
        // When
        List<Driver> result = useCase.execute(command);
        
        // Then - parallel zones searched
        assertThat(result).contains(activeDriver);
        verify(driverGeoQueryPort, atLeastOnce()).findAvailableDriversNearby(any(Location.class), anyDouble(), eq(tenantId));
    }
    
    @Test
    void shouldValidateCommandParameters() {
        // Null tenant ID
        assertThatThrownBy(() -> new FindAvailableDriversCommand(null, pickupLocation, 5.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TenantId cannot be null");
        
        // Null pickup location
        assertThatThrownBy(() -> new FindAvailableDriversCommand(tenantId, null, 5.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pickup location cannot be null");
        
        // Invalid radius (zero)
        assertThatThrownBy(() -> new FindAvailableDriversCommand(tenantId, pickupLocation, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Radius must be between 0 and 50 km");
        
        // Invalid radius (negative)
        assertThatThrownBy(() -> new FindAvailableDriversCommand(tenantId, pickupLocation, -5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Radius must be between 0 and 50 km");
        
        // Invalid radius (too large)
        assertThatThrownBy(() -> new FindAvailableDriversCommand(tenantId, pickupLocation, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Radius must be between 0 and 50 km");
    }
    
    @Test
    void shouldUseDefaultRadiusWhenNotProvided() {
        // Given
        FindAvailableDriversCommand command = new FindAvailableDriversCommand(tenantId, pickupLocation);
        
        // Then
        assertThat(command.radiusKm()).isEqualTo(5.0);
    }
    
    @AfterEach
    void tearDown() {
        executor.close();
    }
}
