package com.rappidrive.application.usecases.auth;

import com.rappidrive.application.ports.input.auth.SendOtpInputPort;
import com.rappidrive.application.ports.output.OtpRateLimiterPort;
import com.rappidrive.application.ports.output.SmsOtpPort;
import com.rappidrive.domain.valueobjects.Phone;

public class SendOtpUseCase implements SendOtpInputPort {

    private final OtpRateLimiterPort rateLimiter;
    private final SmsOtpPort smsOtpPort;

    public SendOtpUseCase(OtpRateLimiterPort rateLimiter, SmsOtpPort smsOtpPort) {
        this.rateLimiter = rateLimiter;
        this.smsOtpPort = smsOtpPort;
    }

    @Override
    public SendOtpResult execute(Command command) {
        Phone phone = new Phone(command.phone());
        rateLimiter.checkAndRecordOtpSend(phone.getValue());
        smsOtpPort.sendOtp(phone.getValue());
        return new SendOtpResult(600);
    }
}
