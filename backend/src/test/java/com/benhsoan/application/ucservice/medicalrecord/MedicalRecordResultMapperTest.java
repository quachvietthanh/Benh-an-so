package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;

@DisplayName("MedicalRecordResultMapper - Emergency Contact Tests (NCL-02-CN-007)")
class MedicalRecordResultMapperTest {

    private final MedicalRecordResultMapper mapper = new MedicalRecordResultMapper();

    @Test
    @DisplayName("TC-01: Chuyển tiếp đầy đủ thông tin người liên hệ khẩn cấp từ Patient sang MedicalRecordDetailResult.PatientInfo")
    void mapsEmergencyContactFromPatientToDetailResult() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.now();

        Patient patient = Patient.restore(
                patientId, "BN-0001", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                Gender.MALE, "0900000000", "a@test.com", "Hanoi", "079090001234", "DN4790123456789",
                BloodType.O_POSITIVE, "Le Thi B", "Vợ", "0909998877",
                true, now, now, doctorId, doctorId,
                true, now, "v1.0", false, null, null, false
        );

        Visit visit = Visit.restore(
                visitId, "VS-0001", patientId, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED, now, now, now,
                "Exam", null, doctorId, now, now
        );

        User doctor = User.restore(
                doctorId, "doctor", "hash", "Dr. Tran B", "doctor@hospital.com", "0988888888",
                UUID.randomUUID(), true, now, now
        );

        MedicalRecord record = MedicalRecord.restore(
                recordId, visitId, "Headache", "Pain", "None", "Normal", "Stable", "Rest",
                "Follow-up", "Migraine", MedicalRecordStatus.OPEN,
                null, null, doctorId, now, null, null
        );

        MedicalRecordDetailResult result = mapper.toDetailResult(record, visit, patient, doctor, List.of());

        assertNotNull(result);
        assertNotNull(result.patient());
        assertEquals("Le Thi B", result.patient().emergencyContact());
        assertEquals("Vợ", result.patient().emergencyRelationship());
        assertEquals("0909998877", result.patient().emergencyPhone());
    }
}
