package com.rappidrive.infrastructure.adapters.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rappidrive.application.exceptions.InvalidPhoneTokenException;
import com.rappidrive.application.ports.output.PhoneTokenPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;

@Component
@Slf4j
public class NimbusPhoneTokenAdapter implements PhoneTokenPort {

    private final String secret;
    private final long ttlSeconds;

    public NimbusPhoneTokenAdapter(
            @Value("${rappidrive.auth.phone-token.secret}") String secret,
            @Value("${rappidrive.auth.phone-token.ttl-seconds:300}") long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String issue(String phone) {
        try {
            JWSSigner signer = new MACSigner(secret);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(phone)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + ttlSeconds * 1000))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error issuing phone token for {}: {}", phone, e.getMessage());
            throw new RuntimeException("Could not issue phone token", e);
        }
    }

    @Override
    public String validateAndExtractPhone(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secret);

            if (!signedJWT.verify(verifier)) {
                log.warn("Invalid phone token signature");
                throw new InvalidPhoneTokenException("Invalid phone token signature");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Date expirationTime = claimsSet.getExpirationTime();

            if (expirationTime == null || new Date().after(expirationTime)) {
                log.warn("Phone token expired");
                throw new InvalidPhoneTokenException("Phone token expired");
            }

            return claimsSet.getSubject();

        } catch (ParseException | JOSEException e) {
            log.warn("Failed to parse or verify phone token: {}", e.getMessage());
            throw new InvalidPhoneTokenException("Invalid phone token: " + e.getMessage());
        }
    }
}
