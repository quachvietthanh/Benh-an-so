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

    @Override
    public void sendVerificationCode(String username, String code, long ttlSeconds) {
        lastSentCodes.put(username, code);
        log.info("[MOCK 2FA] Simulated verification code issued for user {} (valid for {}s)",
                maskUsername(username), ttlSeconds);
    }

    public String getLastSentCode(String username) {
        return lastSentCodes.get(username);
    }

    public void clear() {
        lastSentCodes.clear();
    }

    private String maskUsername(String username) {
        if (username == null || username.length() < 4) {
            return "[REDACTED]";
        }
        return username.charAt(0) + "****" + username.charAt(username.length() - 1);
    }
}
