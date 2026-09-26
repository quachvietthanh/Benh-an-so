package com.benhsoan.port.outbound.authSecurity;

public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}