package com.rappidrive.infrastructure.adapters.twilio;

import com.rappidrive.application.ports.output.SmsOtpPort;
import com.rappidrive.infrastructure.config.TwilioConfig;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TwilioVerifyAdapter implements SmsOtpPort {

    private final TwilioConfig twilioConfig;

    @Override
    public void sendOtp(String phone) {
        log.info("Sending OTP to phone: {}", phone);
        Verification.creator(
                twilioConfig.getServiceSid(),
                phone,
                "sms"
        ).create();
    }

    @Override
    public OtpVerificationResult verifyOtp(String phone, String code) {
        try {
            VerificationCheck check = VerificationCheck.creator(twilioConfig.getServiceSid())
                    .setTo(phone)
                    .setCode(code)
                    .create();

            if ("approved".equalsIgnoreCase(check.getStatus())) {
                return OtpVerificationResult.APPROVED;
            }
            return OtpVerificationResult.INVALID;

        } catch (ApiException e) {
            if (e.getCode() == 60202) {
                return OtpVerificationResult.MAX_ATTEMPTS_REACHED;
            }
            log.error("Twilio API error during OTP verification: {}", e.getMessage());
            return OtpVerificationResult.INVALID;
        }
    }
}
