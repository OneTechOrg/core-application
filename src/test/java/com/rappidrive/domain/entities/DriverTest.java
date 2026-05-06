package com.rappidrive.domain.entities;

import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.exceptions.InvalidDriverStateException;
import com.rappidrive.domain.valueobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverTest {
    
    private UUID id;
    private String keycloakId;
    private TenantId tenantId;
    private String fullName;
    private Email email;
    private CPF cpf;
    private Phone phone;
    private DriverLicense driverLicense;
    
    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        keycloakId = UUID.randomUUID().toString();
        tenantId = TenantId.generate();
        fullName = "João da Silva";
        email = new Email("joao.silva@example.com");
        cpf = new CPF("12345678909");
        phone = new Phone("+5511987654321");
        driverLicense = new DriverLicense("12345678901", "B",
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
    }
    
    @Test
    void shouldCreateDriverWithValidFields() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        
        assertNotNull(driver);
        assertEquals(id, driver.getId());
        assertEquals(keycloakId, driver.getKeycloakId());
        assertEquals(tenantId, driver.getTenantId());
        assertEquals(fullName, driver.getFullName());
        assertEquals(email, driver.getEmail());
        assertEquals(cpf, driver.getCpf());
        assertEquals(phone, driver.getPhone());
        assertEquals(driverLicense, driver.getDriverLicense());
        assertEquals(DriverStatus.PENDING_APPROVAL, driver.getStatus());
        assertTrue(driver.getCurrentLocation().isEmpty());
        assertNotNull(driver.getCreatedAt());
        assertNotNull(driver.getUpdatedAt());
    }
    
    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(null, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense)
        );
    }

    @Test
    void shouldThrowExceptionWhenKeycloakIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, null, tenantId, fullName, email, cpf, phone, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenTenantIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, null, fullName, email, cpf, phone, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenFullNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, tenantId, null, email, cpf, phone, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenFullNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, tenantId, "   ", email, cpf, phone, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, tenantId, fullName, null, cpf, phone, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenCpfIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, tenantId, fullName, email, null, phone, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenPhoneIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, tenantId, fullName, email, cpf, null, driverLicense)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenDriverLicenseIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, null)
        );
    }
    
    @Test
    void shouldTrimFullName() {
        Driver driver = new Driver(id, keycloakId, tenantId, "  João da Silva  ", email, cpf, phone, driverLicense);
        
        assertEquals("João da Silva", driver.getFullName());
    }
    
    @Test
    void shouldActivateDriverFromPendingApproval() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        
        driver.activate();
        
        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
    }
    
    @Test
    void shouldActivateDriverFromInactive() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.deactivate();
        
        driver.activate();
        
        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
    }
    
    @Test
    void shouldThrowExceptionWhenActivatingActiveDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        
        assertThrows(InvalidDriverStateException.class, driver::activate);
    }
    
    @Test
    void shouldThrowExceptionWhenActivatingBlockedDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.block();
        
        assertThrows(InvalidDriverStateException.class, driver::activate);
    }
    
    @Test
    void shouldDeactivateActiveDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        
        driver.deactivate();
        
        assertEquals(DriverStatus.INACTIVE, driver.getStatus());
    }
    
    @Test
    void shouldClearLocationWhenDeactivating() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.updateLocation(new Location(-23.550520, -46.633308));
        
        driver.deactivate();
        
        assertTrue(driver.getCurrentLocation().isEmpty());
    }
    
    @Test
    void shouldThrowExceptionWhenDeactivatingInactiveDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.deactivate();
        
        assertThrows(InvalidDriverStateException.class, driver::deactivate);
    }
    
    @Test
    void shouldThrowExceptionWhenDeactivatingBlockedDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.block();
        
        assertThrows(InvalidDriverStateException.class, driver::deactivate);
    }
    
    @Test
    void shouldBlockDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        
        driver.block();
        
        assertEquals(DriverStatus.BLOCKED, driver.getStatus());
    }
    
    @Test
    void shouldClearLocationWhenBlocking() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.updateLocation(new Location(-23.550520, -46.633308));
        
        driver.block();
        
        assertTrue(driver.getCurrentLocation().isEmpty());
    }
    
    @Test
    void shouldThrowExceptionWhenBlockingAlreadyBlockedDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.block();
        
        assertThrows(InvalidDriverStateException.class, driver::block);
    }
    
    @Test
    void shouldUpdateLocationForActiveDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        Location location = new Location(-23.550520, -46.633308);
        
        driver.updateLocation(location);
        
        assertTrue(driver.getCurrentLocation().isPresent());
        assertEquals(location, driver.getCurrentLocation().get());
    }
    
    @Test
    void shouldThrowExceptionWhenUpdatingLocationWithNull() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        
        assertThrows(IllegalArgumentException.class, () -> 
            driver.updateLocation(null)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenUpdatingLocationForInactiveDriver() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        Location location = new Location(-23.550520, -46.633308);
        
        assertThrows(InvalidDriverStateException.class, () ->
            driver.updateLocation(location)
        );
    }
    
    @Test
    void shouldBeAvailableWhenActiveAndHasLocation() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.updateLocation(new Location(-23.550520, -46.633308));
        
        assertTrue(driver.isAvailableForRide());
    }
    
    @Test
    void shouldNotBeAvailableWhenActiveButNoLocation() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        
        assertFalse(driver.isAvailableForRide());
    }
    
    @Test
    void shouldNotBeAvailableWhenInactive() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.updateLocation(new Location(-23.550520, -46.633308));
        driver.deactivate();
        
        assertFalse(driver.isAvailableForRide());
    }
    
    @Test
    void shouldNotBeAvailableWhenBlocked() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        driver.activate();
        driver.updateLocation(new Location(-23.550520, -46.633308));
        driver.block();
        
        assertFalse(driver.isAvailableForRide());
    }
    
    @Test
    void shouldNotBeAvailableWhenLicenseExpired() {
        DriverLicense expiredLicense = new DriverLicense("12345678901", "B",
            LocalDate.of(2015, 1, 1), LocalDate.of(2020, 1, 1), true);
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, expiredLicense);
        driver.activate();
        driver.updateLocation(new Location(-23.550520, -46.633308));
        
        assertFalse(driver.isAvailableForRide());
    }
    
    @Test
    void shouldBeEqualWhenSameId() {
        Driver driver1 = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        DriverLicense otherLicense = new DriverLicense("98765432109", "C",
            LocalDate.of(2019, 1, 1), LocalDate.of(2029, 1, 1), true);
        Driver driver2 = new Driver(id, keycloakId, TenantId.generate(), "Other Name", 
            new Email("other@example.com"), new CPF("98765432100"), 
            new Phone("+5511999999999"), otherLicense);
        
        assertEquals(driver1, driver2);
        assertEquals(driver1.hashCode(), driver2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenDifferentId() {
        Driver driver1 = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        Driver driver2 = new Driver(UUID.randomUUID(), keycloakId, tenantId, fullName, 
            email, cpf, phone, driverLicense);
        
        assertNotEquals(driver1, driver2);
    }
    
    @Test
    void shouldHaveMeaningfulToString() {
        Driver driver = new Driver(id, keycloakId, tenantId, fullName, email, cpf, phone, driverLicense);
        
        String str = driver.toString();
        
        assertTrue(str.contains(id.toString()));
        assertTrue(str.contains(fullName));
        assertTrue(str.contains("PENDING_APPROVAL"));
    }
}
