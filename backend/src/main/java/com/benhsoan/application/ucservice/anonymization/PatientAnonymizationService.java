package com.benhsoan.application.ucservice.anonymization;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.benhsoan.domain.patient.PatientAnonymizer;

/**
 * Application-level entry point for the anonymization mode (NCL-15-CN-003).
 *
 * <p>Holds the global ON/OFF state in a thread-safe in-memory flag and applies
 * the masking policy only when enabled. When disabled (the default), every
 * method returns the original value unchanged, so existing behavior is
 * preserved.</p>
 *
 * <p>The future administrator toggle (CV-03) will call {@link #enable()} /
 * {@link #disable()}; this service intentionally exposes no persistence and no
 * UI.</p>
 */
@Service
public class PatientAnonymizationService {

    private final AtomicBoolean enabled;

    public PatientAnonymizationService(
            @Value("${app.anonymization.enabled:false}") boolean enabledByDefault) {
        this.enabled = new AtomicBoolean(enabledByDefault);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void enable() {
        enabled.set(true);
    }

    public void disable() {
        enabled.set(false);
    }

    public String anonymizeFullName(String patientCode, String fullName) {
        return enabled.get() ? PatientAnonymizer.maskFullName(patientCode) : fullName;
    }

    public String anonymizePhone(String phone) {
        return enabled.get() ? PatientAnonymizer.maskPhone(phone) : phone;
    }

    public String anonymizeAddress(String address) {
        return enabled.get() ? PatientAnonymizer.maskAddress(address) : address;
    }
}
