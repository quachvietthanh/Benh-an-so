package com.benhsoan.infrastructure.authSecurity;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;

@Component
public class SecureRefreshTokenGeneratorAdapter implements RefreshTokenGeneratorPort {

    private static final int TOKEN_SIZE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] tokenBytes = new byte[TOKEN_SIZE_BYTES];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }
}
