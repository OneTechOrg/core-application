package com.rappidrive.domain.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DriverLicenseTest {
    
    @Test
    void shouldCreateDriverLicenseWithValidData() {
        String number = "12345678901";
        String category = "B";
        LocalDate issueDate = LocalDate.of(2020, 1, 1);
        LocalDate expirationDate = LocalDate.of(2030, 1, 1);
        
        DriverLicense license = new DriverLicense(number, category, issueDate, expirationDate, true);
        
        assertNotNull(license);
        assertEquals(number, license.getNumber());
        assertEquals("B", license.getCategory());
        assertEquals(issueDate, license.getIssueDate());
        assertEquals(expirationDate, license.getExpirationDate());
    }
    
    @Test
    void shouldThrowExceptionWhenNumberIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense(null, "B", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenNumberIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("", "B", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenNumberIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("   ", "B", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenNumberHasLessThan11Digits() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("1234567890", "B", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenNumberHasMoreThan11Digits() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("123456789012", "B", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldAcceptFormattedNumber() {
        DriverLicense license = new DriverLicense("123.456.789-01", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("12345678901", license.getNumber());
    }
    
    @Test
    void shouldThrowExceptionWhenCategoryIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", null, LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenCategoryIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", "", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenCategoryIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", "X", LocalDate.now(), LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldAcceptCategoryA() {
        DriverLicense license = new DriverLicense("12345678901", "A", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("A", license.getCategory());
    }
    
    @Test
    void shouldAcceptCategoryB() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("B", license.getCategory());
    }
    
    @Test
    void shouldAcceptCategoryAB() {
        DriverLicense license = new DriverLicense("12345678901", "AB", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("AB", license.getCategory());
    }
    
    @Test
    void shouldAcceptCategoryC() {
        DriverLicense license = new DriverLicense("12345678901", "C", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("C", license.getCategory());
    }
    
    @Test
    void shouldAcceptCategoryD() {
        DriverLicense license = new DriverLicense("12345678901", "D", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("D", license.getCategory());
    }
    
    @Test
    void shouldAcceptCategoryE() {
        DriverLicense license = new DriverLicense("12345678901", "E", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("E", license.getCategory());
    }
    
    @Test
    void shouldNormalizeCategoryToUpperCase() {
        DriverLicense license = new DriverLicense("12345678901", "ab", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("AB", license.getCategory());
    }
    
    @Test
    void shouldThrowExceptionWhenIssueDateIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", "B", null, LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenExpirationDateIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", "B", LocalDate.now(), null, true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenExpirationDateIsBeforeIssueDate() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", "B", 
                LocalDate.of(2030, 1, 1), 
                LocalDate.of(2020, 1, 1), true)
        );
    }
    
    @Test
    void shouldThrowExceptionWhenIssueDateIsInFuture() {
        assertThrows(IllegalArgumentException.class, () ->
            new DriverLicense("12345678901", "B", 
                LocalDate.now().plusDays(1), 
                LocalDate.now().plusYears(5), true)
        );
    }
    
    @Test
    void shouldAcceptIssueDateToday() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.now(), LocalDate.now().plusYears(5), true);
        
        assertEquals(LocalDate.now(), license.getIssueDate());
    }
    
    @Test
    void shouldReturnFalseForExpiredLicense() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.of(2015, 1, 1), 
            LocalDate.of(2020, 1, 1), true);
        
        assertTrue(license.isExpired());
        assertFalse(license.isValid());
    }
    
    @Test
    void shouldReturnTrueForValidLicense() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.of(2020, 1, 1), 
            LocalDate.of(2030, 1, 1), true);
        
        assertFalse(license.isExpired());
        assertTrue(license.isValid());
    }
    
    @Test
    void shouldReturnTrueForLicenseExpiringToday() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.now().minusYears(5), 
            LocalDate.now(), true);
        
        assertFalse(license.isExpired());
        assertTrue(license.isValid());
    }
    
    @Test
    void shouldFormatNumberCorrectly() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertEquals("123.456.789-01", license.getFormatted());
    }
    
    @Test
    void shouldBeEqualWhenSameNumber() {
        DriverLicense license1 = new DriverLicense("12345678901", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        DriverLicense license2 = new DriverLicense("12345678901", "C", 
            LocalDate.of(2019, 1, 1), LocalDate.of(2029, 1, 1), true);
        
        assertEquals(license1, license2);
        assertEquals(license1.hashCode(), license2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenDifferentNumber() {
        DriverLicense license1 = new DriverLicense("12345678901", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        DriverLicense license2 = new DriverLicense("98765432109", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        assertNotEquals(license1, license2);
    }
    
    @Test
    void shouldHaveMeaningfulToString() {
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), true);
        
        String str = license.toString();
        
        assertTrue(str.contains("***.***.***-01"));
        assertTrue(str.contains("B"));
        assertTrue(str.contains("true")); // isValid
    }
}
