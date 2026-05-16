package com.rappidrive.infrastructure.adapters.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rappidrive.application.exceptions.OtpRateLimitExceededException;
import com.rappidrive.application.ports.output.OtpRateLimiterPort;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CaffeineOtpRateLimiterAdapter implements OtpRateLimiterPort {

    private final Cache<String, AtomicInteger> otpSendCache;
    private final Cache<String, AtomicInteger> registrationCache;

    public CaffeineOtpRateLimiterAdapter() {
        this.otpSendCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        this.registrationCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    @Override
    public void checkAndRecordOtpSend(String phone) {
        AtomicInteger count = otpSendCache.get(phone, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > 3) {
            throw new OtpRateLimitExceededException("Too many OTP requests for this phone number. Please try again later.", 600);
        }
    }

    @Override
    public void checkRegistrationLimit(String clientIp) {
        AtomicInteger count = registrationCache.get(clientIp, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > 10) {
            throw new OtpRateLimitExceededException("Too many registration attempts from this IP. Please try again later.", 3600);
        }
    }
}
