package com.rappidrive.domain.valueobjects;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value object representing a Brazilian driver's license (CNH).
 * Immutable and validates license information.
 */
public final class DriverLicense {
    
    private final String number;
    private final String category;
    private final LocalDate issueDate;
    private final LocalDate expirationDate;
    private final boolean isDefinitive;
    
    /**
     * Creates a new DriverLicense instance.
     * 
     * @param number CNH number (11 digits)
     * @param category CNH category (A, B, AB, C, D, E, etc.)
     * @param issueDate date when CNH was issued
     * @param expirationDate date when CNH expires
     * @param isDefinitive true if CNH is definitive, false if it's PPD (temporary permission)
     * @throws IllegalArgumentException if validation fails
     */
    public DriverLicense(String number, String category, LocalDate issueDate, 
                        LocalDate expirationDate, boolean isDefinitive) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("CNH number cannot be null or empty");
        }
        
        String cleaned = number.trim().replaceAll("[^0-9]", "");
        
        if (cleaned.length() != 11) {
            throw new IllegalArgumentException(
                "CNH number must have exactly 11 digits"
            );
        }
        
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("CNH category cannot be null or empty");
        }
        
        String normalizedCategory = category.trim().toUpperCase();
        if (!isValidCategory(normalizedCategory)) {
            throw new IllegalArgumentException(
                "Invalid CNH category. Valid categories: A, B, AB, C, D, E, AC, AD, AE"
            );
        }
        
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date cannot be null");
        }
        
        if (expirationDate == null) {
            throw new IllegalArgumentException("Expiration date cannot be null");
        }
        
        if (expirationDate.isBefore(issueDate)) {
            throw new IllegalArgumentException(
                "Expiration date cannot be before issue date"
            );
        }
        
        if (issueDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Issue date cannot be in the future");
        }
        
        this.number = cleaned;
        this.category = normalizedCategory;
        this.issueDate = issueDate;
        this.expirationDate = expirationDate;
        this.isDefinitive = isDefinitive;
    }
    
    /**
     * Checks if the driver's license is expired.
     * 
     * @return true if current date is after expiration date
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }
    
    /**
     * Checks if the driver's license is valid (not expired).
     * 
     * @return true if current date is on or before expiration date
     */
    public boolean isValid() {
        return !isExpired();
    }
    
    /**
     * Returns formatted CNH number (XXX.XXX.XXX-XX).
     * 
     * @return formatted CNH number
     */
    public String getFormatted() {
        return String.format("%s.%s.%s-%s",
            number.substring(0, 3),
            number.substring(3, 6),
            number.substring(6, 9),
            number.substring(9)
        );
    }
    
    private boolean isValidCategory(String category) {
        return category.matches("^(A|B|AB|C|D|E|AC|AD|AE)$");
    }
    
    // Getters
    
    public String getNumber() {
        return number;
    }
    
    public String getCategory() {
        return category;
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public boolean isDefinitive() {
        return isDefinitive;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DriverLicense)) return false;
        DriverLicense that = (DriverLicense) o;
        return number.equals(that.number);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
    
    @Override
    public String toString() {
        return String.format("DriverLicense(number=***.***.***-%s, category=%s, definitive=%s, valid=%s)",
            number.substring(9), category, isDefinitive, isValid());
    }
}
