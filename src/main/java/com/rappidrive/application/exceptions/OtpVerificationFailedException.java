package com.rappidrive.application.exceptions;

public class OtpVerificationFailedException extends ApplicationException {
    private final boolean maxAttemptsReached;

    public OtpVerificationFailedException(String message, boolean maxAttemptsReached) {
        super(message);
        this.maxAttemptsReached = maxAttemptsReached;
    }

    public boolean isMaxAttemptsReached() {
        return maxAttemptsReached;
    }
}
