package com.benhsoan.domain.patient;

/**
 * Pure domain policy that masks patient-identifying fields for demonstration
 * (NCL-15-CN-003). It is deliberately stateless and framework-free: the
 * ON/OFF decision lives in the application layer, while this class only knows
 * how to turn raw values into readable, deterministic, non-identifying values.
 *
 * <p>The original patient values must never be persisted or logged; masking is
 * applied only at presentation/export boundaries.</p>
 */
public final class PatientAnonymizer {

    public static final String MASKED_ADDRESS = "[ĐỊA CHỈ ĐÃ ẨN DANH]";

    private static final String MASKED_NAME_PREFIX = "BỆNH NHÂN #";
    private static final String GENERIC_MASKED_NAME = "BỆNH NHÂN";
    private static final String PHONE_PLACEHOLDER = "******";

    private PatientAnonymizer() {
    }

    /**
     * Replaces a patient full name with a stable, non-identifying label derived
     * from the patient code so the same patient stays recognizable during a demo.
     */
    public static String maskFullName(String patientCode) {
        if (patientCode == null || patientCode.isBlank()) {
            return GENERIC_MASKED_NAME;
        }
        return MASKED_NAME_PREFIX + patientCode.trim();
    }

    /**
     * Masks a phone number by keeping only its first two and last two digits,
     * e.g. {@code 0912345678} becomes {@code 09******78}.
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return PHONE_PLACEHOLDER;
        }
        return trimmed.substring(0, 2) + PHONE_PLACEHOLDER + trimmed.substring(trimmed.length() - 2);
    }

    /**
     * Replaces a patient address with a fixed, non-identifying placeholder.
     */
    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        return MASKED_ADDRESS;
    }
}
