package com.rappidrive.domain.valueobjects;

import java.util.Objects;
import java.util.Set;

/**
 * Value object representing a Brazilian CPF (Cadastro de Pessoas Físicas).
 * Immutable and validates CPF format and check digits.
 */
public final class CPF {
    
    private static final Set<String> INVALID_CPFS = Set.of(
        "00000000000", "11111111111", "22222222222", "33333333333",
        "44444444444", "55555555555", "66666666666", "77777777777",
        "88888888888", "99999999999"
    );
    
    private final String value;
    
    /**
     * Creates a new CPF instance.
     * 
     * @param value the CPF number (can be formatted or not)
     * @throws IllegalArgumentException if CPF is null, invalid format, or invalid check digits
     */
    public CPF(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CPF cannot be null or empty");
        }
        
        String cleanedCpf = cleanCpf(value);
        validateCpf(cleanedCpf);
        
        this.value = cleanedCpf;
    }
    
    private String cleanCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }
    
    private void validateCpf(String cpf) {
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("CPF must have 11 digits");
        }
        
        if (INVALID_CPFS.contains(cpf)) {
            throw new IllegalArgumentException("Invalid CPF: sequential digits");
        }
        
        if (!isValidCheckDigits(cpf)) {
            throw new IllegalArgumentException("Invalid CPF: check digits are incorrect");
        }
    }
    
    private boolean isValidCheckDigits(String cpf) {
        // Calculate first check digit
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int firstCheckDigit = 11 - (sum % 11);
        if (firstCheckDigit >= 10) {
            firstCheckDigit = 0;
        }
        
        if (firstCheckDigit != Character.getNumericValue(cpf.charAt(9))) {
            return false;
        }
        
        // Calculate second check digit
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int secondCheckDigit = 11 - (sum % 11);
        if (secondCheckDigit >= 10) {
            secondCheckDigit = 0;
        }
        
        return secondCheckDigit == Character.getNumericValue(cpf.charAt(10));
    }
    
    /**
     * Returns the CPF value without formatting.
     * 
     * @return the CPF with 11 digits only
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Returns the CPF in formatted style: XXX.XXX.XXX-XX
     * 
     * @return formatted CPF
     */
    public String getFormatted() {
        return String.format("%s.%s.%s-%s",
            value.substring(0, 3),
            value.substring(3, 6),
            value.substring(6, 9),
            value.substring(9, 11)
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CPF cpf = (CPF) o;
        return Objects.equals(value, cpf.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return "***.***.***-" + value.substring(9);
    }
}
