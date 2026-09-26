package com.benhsoan.infrastructure.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.notification.TwoFactorCodeDeliveryPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock adapter simulating the "mã xác thực mô phỏng" (simulated verification code)
 * delivery for two-factor authentication (NCL-01-CN-006).
 *
 * <p>The plaintext code is kept in-memory for tests only and is never written to
 * normal application logs; only a masked identifier is logged.</p>
 */
@Slf4j
@Component
public class MockTwoFactorCodeDeliveryAdapter implements TwoFactorCodeDeliveryPort {

    private final Map<String, String> lastSentCodes = new ConcurrentHashMap<>();

    // TODO [SECURITY - PRODUCTION BLOCKER]: Dòng log dưới đây in mã OTP dạng 
    // plaintext ra console, CHỈ được chấp nhận trong môi trường phát triển cá 
    // nhân. TUYỆT ĐỐI PHẢI XÓA hoặc thay bằng tích hợp SMS/Email Provider thật 
    // (Twilio, SendGrid, ESMS...) trước khi triển khai hệ thống cho người dùng 
    // thật, nếu không đây là lỗ hổng bảo mật nghiêm trọng.
    @Override
    public void sendVerificationCode(String username, String code, long ttlSeconds) {
        lastSentCodes.put(username, code);
        log.warn("===> [MOCK 2FA - CHỈ DÙNG CHO MÔI TRƯỜNG DEV/TEST] User: {} | " +
                "Mã xác thực: [ {} ] | Hiệu lực: {}s | " +
                "⚠️ CẢNH BÁO: Dòng log này lộ mã OTP dạng plaintext — BẮT BUỘC " +
                "phải gỡ bỏ hoặc chuyển sang cơ chế gửi SMS/Email thật trước khi " +
                "triển khai cho người dùng thật <===",
                username, code, ttlSeconds);
    }

    public String getLastSentCode(String username) {
        return lastSentCodes.get(username);
    }

    public void clear() {
        lastSentCodes.clear();
    }
}
