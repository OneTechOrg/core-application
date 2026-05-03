package com.rappidrive.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneTest {

    @Test
    void shouldCreateValidBrazilianMobilePhone() {
        Phone phone = new Phone("+5511987654321");
        assertEquals("+5511987654321", phone.getValue());
    }

    @Test
    void shouldCreateValidBrazilianLandline() {
        Phone phone = new Phone("+551134567890");
        assertEquals("+551134567890", phone.getValue());
    }

    @Test
    void shouldCreateValidUSPhone() {
        Phone phone = new Phone("+12025551234");
        assertEquals("+12025551234", phone.getValue());
    }

    @Test
    void shouldRemoveWhitespaceAndFormatting() {
        Phone phone = new Phone("+55 11 98765-4321");
        assertEquals("+5511987654321", phone.getValue());
    }

    @Test
    void shouldRemoveParentheses() {
        Phone phone = new Phone("+55 (11) 98765-4321");
        assertEquals("+5511987654321", phone.getValue());
    }

    @Test
    void shouldExtractBrazilianCountryCode() {
        Phone phone = new Phone("+5511987654321");
        assertEquals("55", phone.getCountryCode());
    }

    @Test
    void shouldExtractUSCountryCode() {
        Phone phone = new Phone("+12025551234");
        assertEquals("1", phone.getCountryCode());
    }

    @Test
    void shouldFormatBrazilianMobile() {
        Phone phone = new Phone("+5511987654321");
        assertEquals("+55 11 98765-4321", phone.getFormatted());
    }

    @Test
    void shouldFormatBrazilianLandline() {
        Phone phone = new Phone("+551134567890");
        assertEquals("+55 11 3456-7890", phone.getFormatted());
    }

    @Test
    void shouldReturnUnformattedForNonBrazilianPhones() {
        Phone phone = new Phone("+12025551234");
        assertEquals("+12025551234", phone.getFormatted());
    }

    @Test
    void shouldRejectNullPhone() {
        assertThrows(IllegalArgumentException.class, () -> new Phone(null));
    }

    @Test
    void shouldRejectEmptyPhone() {
        assertThrows(IllegalArgumentException.class, () -> new Phone(""));
    }

    @Test
    void shouldRejectBlankPhone() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("   "));
    }

    @Test
    void shouldRejectPhoneWithoutCountryCode() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("11987654321"));
    }

    @Test
    void shouldRejectPhoneTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("+551234"));
    }

    @Test
    void shouldRejectPhoneTooLong() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("+55119876543211234567"));
    }

    @Test
    void shouldRejectPhoneWithInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("+55abc87654321"));
    }

    @Test
    void shouldAcceptMinimumValidLength() {
        // Minimum: +X followed by 8 digits
        Phone phone = new Phone("+112345678");
        assertEquals("+112345678", phone.getValue());
    }

    @Test
    void shouldAcceptMaximumValidLength() {
        // Maximum: +X followed by 14 digits
        Phone phone = new Phone("+112345678901234");
        assertEquals("+112345678901234", phone.getValue());
    }

    @Test
    void shouldHaveEqualityBasedOnValue() {
        Phone phone1 = new Phone("+5511987654321");
        Phone phone2 = new Phone("+55 11 98765-4321");
        Phone phone3 = new Phone("+5511999999999");

        assertEquals(phone1, phone2);
        assertNotEquals(phone1, phone3);
    }

    @Test
    void shouldHaveSameHashCodeForEqualPhones() {
        Phone phone1 = new Phone("+5511987654321");
        Phone phone2 = new Phone("+55 11 98765-4321");

        assertEquals(phone1.hashCode(), phone2.hashCode());
    }

    @Test
    void shouldReturnUnformattedPhoneInToString() {
        Phone phone = new Phone("+5511987654321");
        assertEquals("**********4321", phone.toString());
    }
}
