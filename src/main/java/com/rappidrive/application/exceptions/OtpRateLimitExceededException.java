package com.rappidrive.application.exceptions;

public class OtpRateLimitExceededException extends ApplicationException {
    private final int retryAfterSeconds;

    public OtpRateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
