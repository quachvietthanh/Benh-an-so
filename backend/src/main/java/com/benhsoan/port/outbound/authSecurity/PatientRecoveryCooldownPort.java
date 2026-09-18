package com.benhsoan.port.outbound.authSecurity;

import java.time.Instant;

/**
 * Outbound port for tracking and enforcing password recovery request cooldown
 * independently of whether a user account exists, preventing phone enumeration (NCL-14-CN-006 TC-03).
 */
public interface PatientRecoveryCooldownPort {

    /**
     * Checks if the given phone is currently in cooldown.
     *
     * @param phone the normalized phone number
     * @param now   current timestamp
     * @return true if cooldown is active, false otherwise
     */
    boolean isInCooldown(String phone, Instant now);

    /**
     * Records a new recovery request timestamp for the given phone.
     *
     * @param phone           the normalized phone number
     * @param now             current timestamp
     * @param cooldownSeconds duration of cooldown in seconds
     */
    void recordRequest(String phone, Instant now, long cooldownSeconds);

    /**
     * Gets the remaining cooldown duration in seconds.
     *
     * @param phone the normalized phone number
     * @param now   current timestamp
     * @return remaining seconds, or 0 if not in cooldown
     */
    long getRemainingCooldownSeconds(String phone, Instant now);

    /**
     * Resets or clears the cooldown for the given phone (e.g. after successful password reset).
     *
     * @param phone the normalized phone number
     */
    void clearCooldown(String phone);
}
