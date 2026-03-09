package com.rappidrive.infrastructure.integration;

import com.rappidrive.application.ports.input.tenant.OnboardNewTenantInputPort;
import com.rappidrive.application.ports.output.FareConfigurationRepositoryPort;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.FareConfiguration;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test for Tenant Onboarding.
 * Uses Testcontainers for PostgreSQL.
 * 
 * Tests the persistence layer:
 * 1. Save fare configuration in PostgreSQL
 * 2. Save service area in PostgreSQL
 * 3. Retrieve saved configurations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TenantOnboardingIntegrationTest {
    
    // PostgreSQL container
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("rappidrive_test")
            .withUsername("postgres")
            .withPassword("postgres");
    
    @Autowired
    private OnboardNewTenantInputPort onboardTenantUseCase;
    
    @Autowired
    private ServiceAreaRepositoryPort serviceAreaRepository;
    
    @Autowired
    private FareConfigurationRepositoryPort fareConfigRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.rappidrive.application.ports.output.IdentityProvisioningPort identityProvisioningPort;

    @org.junit.jupiter.api.BeforeEach
    void setupMocks() {
        org.mockito.Mockito.when(identityProvisioningPort.createTenantAdmin(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> java.util.UUID.randomUUID().toString());
        org.mockito.Mockito.when(identityProvisioningPort.createTenantGroup(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> java.util.UUID.randomUUID().toString());
        org.mockito.Mockito.when(identityProvisioningPort.tenantGroupExists(org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
    }
    
    @Test
    void shouldOnboardNewTenantSuccessfully() {
        // Arrange
        TenantId tenantId = TenantId.generate();
        Email adminEmail = new Email("admin@goiania.rappidrive.com");
        String displayName = "RappiDrive Goiânia";
        String currency = "BRL";
        BigDecimal baseFare = new BigDecimal("5.00");
        BigDecimal pricePerKm = new BigDecimal("2.50");
        BigDecimal pricePerMin = new BigDecimal("0.30");
        String serviceAreaName = "Centro de Goiânia";
        String geoJsonPolygon = """
                {
                  "type": "Polygon",
                  "coordinates": [
                    [
                      [-49.0, -15.8],
                      [-49.0, -15.9],
                      [-49.1, -15.9],
                      [-49.1, -15.8],
                      [-49.0, -15.8]
                    ]
                  ]
                }
                """;
        
        OnboardNewTenantInputPort.OnboardingCommand command = 
                new OnboardNewTenantInputPort.OnboardingCommand(
                        tenantId,
                        displayName,
                        adminEmail,
                        currency,
                        baseFare,
                        pricePerKm,
                        pricePerMin,
                        serviceAreaName,
                        geoJsonPolygon
                );
        
        // Act
        OnboardNewTenantInputPort.OnboardingResult result = onboardTenantUseCase.execute(command);
        
        // Assert - Result
        assertThat(result).isNotNull();
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.displayName()).isEqualTo(displayName);
        assertThat(result.adminEmail()).isEqualTo(adminEmail.getValue());
        assertThat(result.keycloakUserId()).isNotBlank();
        assertThat(result.keycloakGroupId()).isNotBlank();
        assertThat(result.serviceAreaId()).isNotBlank();
        assertThat(result.fareConfigurationId()).isNotBlank();
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(result.message()).contains("onboarded successfully");
        
        // Assert - ServiceArea saved in PostgreSQL
        var serviceAreas = serviceAreaRepository.findByTenantId(tenantId);
        assertThat(serviceAreas).hasSize(1);
        
        ServiceArea savedServiceArea = serviceAreas.get(0);
        assertThat(savedServiceArea.getName()).isEqualTo(serviceAreaName);
        assertThat(savedServiceArea.getTenantId()).isEqualTo(tenantId);
        assertThat(savedServiceArea.isActive()).isTrue();
        assertThat(savedServiceArea.getGeoJsonPolygon()).contains("Polygon");
        
        // Assert - FareConfiguration saved in PostgreSQL
        Optional<FareConfiguration> fareConfig = fareConfigRepository.findByTenantId(tenantId);
        assertThat(fareConfig).isPresent();
        
        FareConfiguration config = fareConfig.get();
        assertThat(config.getTenantId()).isEqualTo(tenantId);
        assertThat(config.getBaseFare().getAmount()).isEqualByComparingTo(baseFare);
        assertThat(config.getPricePerKm().getAmount()).isEqualByComparingTo(pricePerKm);
        assertThat(config.getPricePerMinute().getAmount()).isEqualByComparingTo(pricePerMin);
    }
    
    @Test
    void shouldFailWhenTenantAlreadyExists() {
        // Arrange - Onboard first tenant
        TenantId tenantId = TenantId.generate();
        Email adminEmail = new Email("admin@saopaulo.rappidrive.com");
        
        OnboardNewTenantInputPort.OnboardingCommand command = 
                new OnboardNewTenantInputPort.OnboardingCommand(
                        tenantId,
                        "RappiDrive São Paulo",
                        adminEmail,
                        "BRL",
                        new BigDecimal("5.00"),
                        new BigDecimal("2.50"),
                        new BigDecimal("0.30"),
                        "Centro SP",
                        createValidGeoJson()
                );
        
        // Act - First onboarding
        onboardTenantUseCase.execute(command);
        
        // Act - Try to onboard same tenant again
        // Assert
        assertThatThrownBy(() -> onboardTenantUseCase.execute(command))
                .hasMessageContaining("already exists");
    }
    
    @Test
    void shouldValidateGeoJsonPolygon() {
        // Arrange
        TenantId tenantId = TenantId.generate();
        Email adminEmail = new Email("admin@invalid.rappidrive.com");
        String invalidGeoJson = "{\"invalid\": \"json\"}";
        
        OnboardNewTenantInputPort.OnboardingCommand command = 
                new OnboardNewTenantInputPort.OnboardingCommand(
                        tenantId,
                        "Invalid Tenant",
                        adminEmail,
                        "BRL",
                        new BigDecimal("5.00"),
                        new BigDecimal("2.50"),
                        new BigDecimal("0.30"),
                        "Invalid Area",
                        invalidGeoJson
                );
        
        // Act & Assert
        assertThatThrownBy(() -> onboardTenantUseCase.execute(command))
                .hasMessageContaining("GeoJSON");
    }
    
    @Test
    void shouldGenerateSecureTemporaryPassword() {
        // Arrange
        TenantId tenantId = TenantId.generate();
        Email adminEmail = new Email("admin@password.rappidrive.com");
        
        OnboardNewTenantInputPort.OnboardingCommand command = 
                new OnboardNewTenantInputPort.OnboardingCommand(
                        tenantId,
                        "Password Test",
                        adminEmail,
                        "BRL",
                        new BigDecimal("5.00"),
                        new BigDecimal("2.50"),
                        new BigDecimal("0.30"),
                        "Test Area",
                        createValidGeoJson()
                );
        
        // Act
        OnboardNewTenantInputPort.OnboardingResult result1 = onboardTenantUseCase.execute(command);
        
        // Onboard another tenant to compare passwords
        TenantId tenantId2 = TenantId.generate();
        Email adminEmail2 = new Email("admin@password2.rappidrive.com");
        
        OnboardNewTenantInputPort.OnboardingCommand command2 = 
                new OnboardNewTenantInputPort.OnboardingCommand(
                        tenantId2,
                        "Password Test 2",
                        adminEmail2,
                        "BRL",
                        new BigDecimal("5.00"),
                        new BigDecimal("2.50"),
                        new BigDecimal("0.30"),
                        "Test Area 2",
                        createValidGeoJson()
                );
        
        OnboardNewTenantInputPort.OnboardingResult result2 = onboardTenantUseCase.execute(command2);
        
        // Assert
        assertThat(result1.temporaryPassword())
                .isNotBlank()
                .hasSize(16)
                .isNotEqualTo(result2.temporaryPassword());
    }
    
    /**
     * Helper method to create a valid GeoJSON polygon.
     */
    private String createValidGeoJson() {
        return """
                {
                  "type": "Polygon",
                  "coordinates": [
                    [
                      [-48.0, -15.8],
                      [-48.0, -15.9],
                      [-48.1, -15.9],
                      [-48.1, -15.8],
                      [-48.0, -15.8]
                    ]
                  ]
                }
                """;
    }
}
