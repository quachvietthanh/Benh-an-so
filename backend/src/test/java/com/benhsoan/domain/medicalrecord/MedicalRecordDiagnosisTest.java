package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;

class MedicalRecordDiagnosisTest {

    @Test
    void updatesDiagnosisTypeAndNote() {
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecordDiagnosis diagnosis = MedicalRecordDiagnosis.create(
                UUID.randomUUID(), null, "R73.9", "Hyperglycemia", DiagnosisType.SECONDARY,
                "Initial assessment", UUID.randomUUID(), now
        );

        diagnosis.changeType(DiagnosisType.PRIMARY, now.plusSeconds(1));
        diagnosis.updateNote("Confirmed after testing", now.plusSeconds(2));

        assertEquals(DiagnosisType.PRIMARY, diagnosis.getDiagnosisType());
        assertEquals("Confirmed after testing", diagnosis.getNote());
    }
}
