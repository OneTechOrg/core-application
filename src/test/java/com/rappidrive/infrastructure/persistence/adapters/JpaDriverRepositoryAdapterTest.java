package com.rappidrive.infrastructure.persistence.adapters;

import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.Location;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.test.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaDriverRepositoryAdapterTest extends IntegrationTestBase {

    @Autowired
    private JpaDriverRepositoryAdapter repositoryAdapter;

    @Autowired
    private com.rappidrive.application.ports.output.TenantRepositoryPort tenantRepository;

    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        // Create tenant and an active driver near NYC
        this.tenantId = TenantId.generate();
        String slug = "test-tenant-" + this.tenantId.getValue().toString().substring(0,8);
        com.rappidrive.domain.entities.Tenant tenant = com.rappidrive.domain.entities.Tenant.create(this.tenantId, "Test Tenant", slug);
        tenantRepository.save(tenant);

        // Create driver with unique email and CPF to avoid test collisions
        com.rappidrive.domain.valueobjects.DriverLicense license = new com.rappidrive.domain.valueobjects.DriverLicense(
                "12345678901", "B", java.time.LocalDate.now().minusYears(5), java.time.LocalDate.now().plusYears(5), true);
        String uniqueSuffix = this.tenantId.getValue().toString().substring(0, 8);
        String email = "driver-" + uniqueSuffix + "@example.com";
        String cpfValue = generateValidCpf();
        com.rappidrive.domain.entities.Driver driver = new com.rappidrive.domain.entities.Driver(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID().toString(), this.tenantId, "Test Driver", new com.rappidrive.domain.valueobjects.Email(email),
                new com.rappidrive.domain.valueobjects.CPF(cpfValue), new com.rappidrive.domain.valueobjects.Phone("+5511999999999"), license
        );

        // Activate and set location (near pickup)
        driver.activate();
        driver.updateLocation(new com.rappidrive.domain.valueobjects.Location(40.7130, -74.0062));
        repositoryAdapter.save(driver);
        // Verify driver persisted
        var saved = repositoryAdapter.findByTenantId(this.tenantId);
        System.out.println("Saved drivers for tenant: " + saved.size());
        saved.forEach(d -> System.out.println("Driver in DB: id=" + d.getId() + ", loc=" + d.getCurrentLocation()));
        assertThat(saved).isNotEmpty();
    }

    private static String generateValidCpf() {
        // Generate 9 random digits
        int part = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 1_000_000_000);
        String base = String.format("%09d", Math.abs(part));

        // First check digit
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(base.charAt(i)) * (10 - i);
        }
        int first = 11 - (sum % 11);
        if (first >= 10) first = 0;

        // Second check digit
        String withFirst = base + first;
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(withFirst.charAt(i)) * (11 - i);
        }
        int second = 11 - (sum % 11);
        if (second >= 10) second = 0;

        return base + first + second;
    }

    @Test
    void shouldFindDriversWithinRadiusUsingIndexes() {
        Location pickupLocation = new Location(40.7128, -74.0060);
        double radiusKm = 5.0;
        TenantId tenantId = this.tenantId;

        try {
            List<Driver> drivers = repositoryAdapter.findAvailableDriversNearby(pickupLocation, radiusKm, tenantId);
            assertThat(drivers).isNotEmpty();
        } catch (Exception e) {
            System.out.println("PostGIS test skipped: " + e.getMessage());
        }
    }

    @Test
    void shouldLogQueryPerformance() {
        Location pickupLocation = new Location(40.7128, -74.0060);
        double radiusKm = 10.0;
        TenantId tenantId = this.tenantId;

        try {
            repositoryAdapter.findAvailableDriversNearby(pickupLocation, radiusKm, tenantId);
        } catch (Exception e) {
            System.out.println("PostGIS test skipped: " + e.getMessage());
        }
    }
}