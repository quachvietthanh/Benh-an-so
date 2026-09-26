package com.benhsoan.infrastructure.authSecurity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.authSecurity.TokenHashPort;


@Component
public class Sha256TokenHashAdapter implements TokenHashPort {

    @Override
    public String hash(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();

            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "Cannot hash token.",
                    ex
            );
        }
    }
}