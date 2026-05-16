package com.rappidrive.domain.valueobjects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhoneTest {
    @Test
    void shouldAcceptValidE164() {
        assertDoesNotThrow(() -> new Phone("+5511999999999"));
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("11999999999"));
    }
}
