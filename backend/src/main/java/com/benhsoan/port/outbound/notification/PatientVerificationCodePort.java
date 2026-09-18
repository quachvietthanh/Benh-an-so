package com.benhsoan.port.outbound.notification;

public interface PatientVerificationCodePort {

    void sendVerificationCode(String phone, String code, long ttlSeconds);
}
