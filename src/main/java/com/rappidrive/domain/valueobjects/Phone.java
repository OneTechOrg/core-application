package com.rappidrive.domain.valueobjects;

import java.util.regex.Pattern;

public record Phone(String value) {
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    public Phone {
        if (value == null || !E164_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone format. Must be E.164 (e.g., +5511999999999)");
        }
    }

    public String getValue() {
        return value;
    }
}
