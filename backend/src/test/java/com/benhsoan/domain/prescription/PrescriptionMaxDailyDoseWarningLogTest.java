package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class PrescriptionMaxDailyDoseWarningLogTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID PRESCRIPTION_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID HANDLED_BY = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-05T02:00:00Z");

    private static PrescriptionMaxDailyDoseWarningLog restore(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return PrescriptionMaxDailyDoseWarningLog.restore(
                id,
                prescriptionId,
                patientId,
                "Paracetamol",
                new BigDecimal("3000"),
                new BigDecimal("2000"),
                "override reason",
                handledBy,
                handledAt,
                createdAt
        );
    }

    @Test
    void nullId_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                restore(null, PRESCRIPTION_ID, PATIENT_ID, HANDLED_BY, NOW, NOW));
    }

    @Test
    void nullPrescriptionId_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                restore(ID, null, PATIENT_ID, HANDLED_BY, NOW, NOW));
    }

    @Test
    void nullPatientId_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                restore(ID, PRESCRIPTION_ID, null, HANDLED_BY, NOW, NOW));
    }

    @Test
    void nullHandledBy_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                restore(ID, PRESCRIPTION_ID, PATIENT_ID, null, NOW, NOW));
    }

    @Test
    void nullHandledAt_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                restore(ID, PRESCRIPTION_ID, PATIENT_ID, HANDLED_BY, null, NOW));
    }

    @Test
    void nullCreatedAt_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                restore(ID, PRESCRIPTION_ID, PATIENT_ID, HANDLED_BY, NOW, null));
    }
}
