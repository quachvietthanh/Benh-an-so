package com.benhsoan.domain.patient;

import com.benhsoan.domain.shared.exception.ValidationException;

/**
 * Immutable server-managed list of consent document versions.
 */
public enum PatientConsentVersion {

    V1_0("v1.0");

    private final String value;

    PatientConsentVersion(String value) {
        this.value = value;
    }

    public static String current() {
        return V1_0.value;
    }

    public static String resolveForNewConsent(String requestedVersion) {
        if (requestedVersion == null || requestedVersion.isBlank()) {
            return current();
        }
        return requireSupported(requestedVersion);
    }

    public static String requireSupported(String requestedVersion) {
        for (PatientConsentVersion version : values()) {
            if (version.value.equals(requestedVersion)) {
                return version.value;
            }
        }

        throw new ValidationException("Phiên bản phiếu đồng ý không được hỗ trợ.");
    }
}
