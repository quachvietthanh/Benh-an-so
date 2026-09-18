package com.benhsoan.infrastructure.authSecurity;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.authSecurity.PatientRecoveryCooldownPort;

/**
 * In-memory thread-safe adapter for enforcing patient recovery cooldowns
 * across all phone numbers independent of user entity existence.
 */
@Component
public class InMemoryPatientRecoveryCooldownAdapter implements PatientRecoveryCooldownPort {

    private final Map<String, Instant> cooldownMap = new ConcurrentHashMap<>();

    @Override
    public boolean isInCooldown(String phone, Instant now) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        Instant expiresAt = cooldownMap.get(phone);
        if (expiresAt == null) {
            return false;
        }
        if (now.isAfter(expiresAt)) {
            cooldownMap.remove(phone);
            return false;
        }
        return true;
    }

    @Override
    public void recordRequest(String phone, Instant now, long cooldownSeconds) {
        if (phone != null && !phone.isBlank()) {
            cooldownMap.put(phone, now.plusSeconds(cooldownSeconds));
            // Opportunistic cleanup of expired entries if map grows
            if (cooldownMap.size() > 500) {
                cooldownMap.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
            }
        }
    }

    @Override
    public long getRemainingCooldownSeconds(String phone, Instant now) {
        if (phone == null || phone.isBlank()) {
            return 0;
        }
        Instant expiresAt = cooldownMap.get(phone);
        if (expiresAt == null || now.isAfter(expiresAt)) {
            return 0;
        }
        return Math.max(1, Duration.between(now, expiresAt).getSeconds());
    }

    @Override
    public void clearCooldown(String phone) {
        if (phone != null) {
            cooldownMap.remove(phone);
        }
    }
}
