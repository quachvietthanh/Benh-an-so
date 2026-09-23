package com.benhsoan.port.outbound.notification;

public interface TwoFactorCodeDeliveryPort {

    void sendVerificationCode(String username, String code, long ttlSeconds);
}
