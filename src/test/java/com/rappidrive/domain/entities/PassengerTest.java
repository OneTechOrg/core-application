package com.rappidrive.domain.entities;

import com.rappidrive.domain.enums.PassengerStatus;
import com.rappidrive.domain.exceptions.InvalidPassengerStateException;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Phone;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PassengerTest {
    
    private UUID id;
    private String keycloakId;
    private TenantId tenantId;
    private String fullName;
    private Email email;
    private Phone phone;
    
    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        keycloakId = UUID.randomUUID().toString();
        tenantId = TenantId.generate();
        fullName = "Maria Silva";
        email = new Email("maria.silva@example.com");
        phone = new Phone("+5511987654321");
    }
    
    @Test
    void shouldCreatePassengerWithValidFields() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        
        assertNotNull(passenger);
        assertEquals(id, passenger.getId());
        assertEquals(keycloakId, passenger.getKeycloakId());
        assertEquals(tenantId, passenger.getTenantId());
        assertEquals(fullName, passenger.getFullName());
        assertEquals(email, passenger.getEmail());
        assertEquals(phone, passenger.getPhone());
        assertEquals(PassengerStatus.ACTIVE, passenger.getStatus());
        assertNotNull(passenger.getCreatedAt());
        assertNotNull(passenger.getUpdatedAt());
    }
    
    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(null, keycloakId, tenantId, fullName, email, phone)
        );
    }

    @Test
    void shouldThrowExceptionWhenKeycloakIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(id, null, tenantId, fullName, email, phone)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenTenantIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(id, keycloakId, null, fullName, email, phone)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenFullNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(id, keycloakId, tenantId, null, email, phone)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenFullNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(id, keycloakId, tenantId, "   ", email, phone)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(id, keycloakId, tenantId, fullName, null, phone)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenPhoneIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new Passenger(id, keycloakId, tenantId, fullName, email, null)
        );
    }
    
    @Test
    void shouldTrimFullName() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, "  Maria Silva  ", email, phone);
        
        assertEquals("Maria Silva", passenger.getFullName());
    }
    
    @Test
    void shouldActivatePassengerFromInactive() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.deactivate();
        
        passenger.activate();
        
        assertEquals(PassengerStatus.ACTIVE, passenger.getStatus());
    }
    
    @Test
    void shouldThrowExceptionWhenActivatingActivePassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        
        assertThrows(InvalidPassengerStateException.class, passenger::activate);
    }
    
    @Test
    void shouldThrowExceptionWhenActivatingBlockedPassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.block();
        
        assertThrows(InvalidPassengerStateException.class, passenger::activate);
    }
    
    @Test
    void shouldDeactivateActivePassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        
        passenger.deactivate();
        
        assertEquals(PassengerStatus.INACTIVE, passenger.getStatus());
    }
    
    @Test
    void shouldThrowExceptionWhenDeactivatingInactivePassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.deactivate();
        
        assertThrows(InvalidPassengerStateException.class, passenger::deactivate);
    }
    
    @Test
    void shouldThrowExceptionWhenDeactivatingBlockedPassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.block();
        
        assertThrows(InvalidPassengerStateException.class, passenger::deactivate);
    }
    
    @Test
    void shouldBlockPassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        
        passenger.block();
        
        assertEquals(PassengerStatus.BLOCKED, passenger.getStatus());
    }
    
    @Test
    void shouldThrowExceptionWhenBlockingAlreadyBlockedPassenger() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.block();
        
        assertThrows(InvalidPassengerStateException.class, passenger::block);
    }
    
    @Test
    void shouldBeAbleToRequestRideWhenActive() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        
        assertTrue(passenger.canRequestRide());
    }
    
    @Test
    void shouldNotBeAbleToRequestRideWhenInactive() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.deactivate();
        
        assertFalse(passenger.canRequestRide());
    }
    
    @Test
    void shouldNotBeAbleToRequestRideWhenBlocked() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        passenger.block();
        
        assertFalse(passenger.canRequestRide());
    }
    
    @Test
    void shouldBeEqualWhenSameId() {
        Passenger passenger1 = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        Passenger passenger2 = new Passenger(id, keycloakId, TenantId.generate(), "Other Name",
            new Email("other@example.com"), new Phone("+5511999999999"));
        
        assertEquals(passenger1, passenger2);
        assertEquals(passenger1.hashCode(), passenger2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenDifferentId() {
        Passenger passenger1 = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        Passenger passenger2 = new Passenger(UUID.randomUUID(), keycloakId, tenantId, 
            fullName, email, phone);
        
        assertNotEquals(passenger1, passenger2);
    }
    
    @Test
    void shouldHaveMeaningfulToString() {
        Passenger passenger = new Passenger(id, keycloakId, tenantId, fullName, email, phone);
        
        String str = passenger.toString();
        
        assertTrue(str.contains(id.toString()));
        assertTrue(str.contains(fullName));
        assertTrue(str.contains("ACTIVE"));
    }
}
