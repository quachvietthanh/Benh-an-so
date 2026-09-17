package com.benhsoan.infrastructure.security.generator;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.authSecurity.VerificationCodeGeneratorPort;

@Component
public class RandomNumericVerificationCodeGenerator implements VerificationCodeGeneratorPort {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
