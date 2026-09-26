package com.benhsoan.infrastructure.authSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;

@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(
            String rawPassword,
            String encodedPassword
    ) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}