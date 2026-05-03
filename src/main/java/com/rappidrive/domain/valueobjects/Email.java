package com.rappidrive.domain.valueobjects;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing an email address.
 * Immutable and validates email format.
 */
public final class Email {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private final String value;
    
    /**
     * Creates a new Email instance.
     * 
     * @param value the email address
     * @throws IllegalArgumentException if email is null, empty, or invalid format
     */
    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        String normalized = value.trim().toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        
        this.value = normalized;
    }
    
    /**
     * Returns the email address value.
     * 
     * @return the email address in lowercase
     */
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value.replaceAll("(^.{3})[^@]*", "$1***");
    }
}
