package com.rappidrive.domain.valueobjects;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a phone number with international format.
 * Immutable and validates phone number format.
 */
public final class Phone {
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+[1-9]\\d{7,14}$");
    
    private final String value;
    
    /**
     * Creates a new Phone instance.
     * Format: +[country code][number]
     * Example: +5511987654321 (Brazilian mobile)
     * 
     * @param value the phone number in international format
     * @throws IllegalArgumentException if phone is null, empty, or invalid format
     */
    public Phone(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be null or empty");
        }
        
        String cleaned = value.trim().replaceAll("[\\s()-]", "");
        
        if (!cleaned.startsWith("+")) {
            throw new IllegalArgumentException(
                "Phone must start with country code (e.g., +55 for Brazil)"
            );
        }
        
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                "Invalid phone format. Expected: +[country code][number] with 8-15 digits total"
            );
        }
        
        this.value = cleaned;
    }
    
    /**
     * Returns the phone number value.
     * 
     * @return the phone number in international format
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Returns the country code.
     * 
     * @return the country code (e.g., "55" for Brazil)
     */
    public String getCountryCode() {
        // Brazil uses country code 55 (2 digits)
        if (value.startsWith("+55")) {
            return "55";
        }
        // USA/Canada uses country code 1 (1 digit)
        if (value.startsWith("+1")) {
            return "1";
        }
        // Generic: try to extract up to 3 digits
        int endIndex = 2; // Minimum: +X
        if (value.length() > 3 && Character.isDigit(value.charAt(2))) {
            endIndex = 3; // +XX
            if (value.length() > 4 && Character.isDigit(value.charAt(3))) {
                endIndex = 4; // +XXX
            }
        }
        return value.substring(1, endIndex);
    }
    
    /**
     * Returns formatted phone for display.
     * Example: +55 11 98765-4321
     * 
     * @return formatted phone number
     */
    public String getFormatted() {
        if (value.startsWith("+55")) {
            if (value.length() == 14) {
                // Brazilian mobile: +55 11 98765-4321 (9 digits in number part)
                return String.format("%s %s %s-%s",
                    value.substring(0, 3),   // +55
                    value.substring(3, 5),   // 11
                    value.substring(5, 10),  // 98765
                    value.substring(10)      // 4321
                );
            } else if (value.length() == 13) {
                // Brazilian landline: +55 11 3456-7890 (8 digits in number part)
                return String.format("%s %s %s-%s",
                    value.substring(0, 3),   // +55
                    value.substring(3, 5),   // 11
                    value.substring(5, 9),   // 3456
                    value.substring(9)       // 7890
                );
            }
        }
        // Generic format for other countries
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return Objects.equals(value, phone.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value.substring(0, value.length() - 4).replaceAll(".", "*") + value.substring(value.length() - 4);
    }
}
