package com.rappidrive.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = new Email("user@example.com");
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    void shouldNormalizeEmailToLowercase() {
        Email email = new Email("User@Example.COM");
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    void shouldTrimWhitespace() {
        Email email = new Email("  user@example.com  ");
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    void shouldAcceptEmailWithPlus() {
        Email email = new Email("user+tag@example.com");
        assertEquals("user+tag@example.com", email.getValue());
    }

    @Test
    void shouldAcceptEmailWithDots() {
        Email email = new Email("first.last@example.com");
        assertEquals("first.last@example.com", email.getValue());
    }

    @Test
    void shouldAcceptEmailWithHyphen() {
        Email email = new Email("user@my-domain.com");
        assertEquals("user@my-domain.com", email.getValue());
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    void shouldRejectEmptyEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email("   "));
    }

    @Test
    void shouldRejectEmailWithoutAtSign() {
        assertThrows(IllegalArgumentException.class, () -> new Email("userexample.com"));
    }

    @Test
    void shouldRejectEmailWithoutDomain() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@"));
    }

    @Test
    void shouldRejectEmailWithoutLocalPart() {
        assertThrows(IllegalArgumentException.class, () -> new Email("@example.com"));
    }

    @Test
    void shouldRejectEmailWithoutTLD() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@example"));
    }

    @Test
    void shouldRejectEmailWithSpaces() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user name@example.com"));
    }

    @Test
    void shouldHaveEqualityBasedOnValue() {
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("user@example.com");
        Email email3 = new Email("other@example.com");

        assertEquals(email1, email2);
        assertNotEquals(email1, email3);
    }

    @Test
    void shouldHaveSameHashCodeForEqualEmails() {
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("user@example.com");

        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    void shouldReturnEmailInToString() {
        Email email = new Email("user@example.com");
        assertEquals("use***@example.com", email.toString());
    }
}
