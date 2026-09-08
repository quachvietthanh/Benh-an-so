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
     * e.g. {@code 0912345678} becomes {@code 09******78}. Non-digit formatting
     * (spaces, dashes, {@code +84} country code) is stripped first so the mask
     * never leaks formatting artifacts.
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return PHONE_PLACEHOLDER;
        }
        return digits.substring(0, 2) + PHONE_PLACEHOLDER + digits.substring(digits.length() - 2);
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

    /**
     * Always-on masking used by the public (unauthenticated) patient portal
     * lookup. This keeps a distinct, historically stable contract
     * (first 3 + "***" + last 3 digits) and must not be gated by the
     * demonstration anonymization mode, since the portal is public.
     */
    public static String maskPhonePublicPortal(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return "***";
        }
        return digits.substring(0, 3) + "***" + digits.substring(digits.length() - 3);
    }

    /**
     * True when the value is one of the synthetic masked-name forms produced by
     * {@link #maskFullName(String)}. Used to prevent masked values from being
     * persisted back as real patient data.
     */
    public static boolean isMaskedFullName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.equals(GENERIC_MASKED_NAME) || trimmed.startsWith(MASKED_NAME_PREFIX);
    }

    /**
     * True when the value contains the phone mask placeholder produced by
     * {@link #maskPhone(String)}.
     */
    public static boolean isMaskedPhone(String value) {
        return value != null && value.contains(PHONE_PLACEHOLDER);
    }

    /**
     * True when the value equals the fixed masked-address placeholder produced
     * by {@link #maskAddress(String)}.
     */
    public static boolean isMaskedAddress(String value) {
        return value != null && value.equals(MASKED_ADDRESS);
    }
}
