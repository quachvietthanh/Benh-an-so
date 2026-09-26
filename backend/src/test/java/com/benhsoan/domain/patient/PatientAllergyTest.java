package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.shared.exception.ValidationException;

class PatientAllergyTest {

    @Test
    @DisplayName("Tạo mới dị ứng thành công với tên hoạt chất được chuẩn hóa")
    void shouldCreatePatientAllergySuccessfully() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        PatientAllergy allergy = PatientAllergy.create(
                patientId,
                "MEDICATION",
                "  Amoxicillin  ",
                AllergySeverity.MODERATE,
                "Mẩn ngứa da",
                "Phát hiện sau đợt viêm họng",
                doctorId,
                Instant.now()
        );

        assertNotNull(allergy.getId());
        assertEquals(patientId, allergy.getPatientId());
        assertEquals("Amoxicillin", allergy.getAllergenName());
        assertEquals("amoxicillin", allergy.getNormalizedAllergenName());
        assertEquals(AllergySeverity.MODERATE, allergy.getSeverity());
        assertEquals("Mẩn ngứa da", allergy.getReaction());
        assertEquals("Phát hiện sau đợt viêm họng", allergy.getNotes());
        assertTrue(allergy.isActive());
        assertEquals(doctorId, allergy.getCreatedBy());
    }

    @Test
    @DisplayName("Cập nhật thông tin dị ứng thành công")
    void shouldUpdatePatientAllergySuccessfully() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID updateDoctorId = UUID.randomUUID();

        PatientAllergy allergy = PatientAllergy.create(
                patientId,
                "MEDICATION",
                "Penicillin",
                AllergySeverity.MILD,
                "Ngứa nhẹ",
                "Ghi nhận ban đầu",
                doctorId,
                Instant.now()
        );

        Instant updateTime = Instant.now().plusSeconds(3600);
        allergy.update("Penicillin G", AllergySeverity.SEVERE, "Khó thở, mề đay", "Cập nhật sau phản ứng nặng", updateDoctorId, updateTime);

        assertEquals("Penicillin G", allergy.getAllergenName());
        assertEquals("penicillin g", allergy.getNormalizedAllergenName());
        assertEquals(AllergySeverity.SEVERE, allergy.getSeverity());
        assertEquals("Khó thở, mề đay", allergy.getReaction());
        assertEquals(updateDoctorId, allergy.getUpdatedBy());
        assertEquals(updateTime, allergy.getUpdatedAt());
    }

    @Test
    @DisplayName("Vô hiệu hóa (xóa mềm) dị ứng thành công")
    void shouldDeactivatePatientAllergy() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        PatientAllergy allergy = PatientAllergy.create(
                patientId,
                "MEDICATION",
                "Aspirin",
                AllergySeverity.MILD,
                null,
                null,
                doctorId,
                Instant.now()
        );

        assertTrue(allergy.isActive());

        allergy.deactivate(doctorId, Instant.now());
        assertFalse(allergy.isActive());
    }

    @Test
    @DisplayName("Báo lỗi khi thiếu tên hoạt chất hoặc bệnh nhân")
    void shouldThrowWhenRequiredFieldsMissing() {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        assertThrows(ValidationException.class, () ->
                PatientAllergy.create(null, "MEDICATION", "Aspirin", AllergySeverity.MILD, null, null, doctorId, Instant.now())
        );

        assertThrows(ValidationException.class, () ->
                PatientAllergy.create(patientId, "MEDICATION", "   ", AllergySeverity.MILD, null, null, doctorId, Instant.now())
        );

        assertThrows(NullPointerException.class, () ->
                PatientAllergy.create(patientId, "MEDICATION", "Aspirin", null, null, null, doctorId, Instant.now())
        );
    }

    @Test
    @DisplayName("TC-DOM-01: normalizeAllergenName loại bỏ khoảng trắng thừa ở giữa và cố định chữ thường")
    void tcDom01_normalizeAllergenName_shouldCollapseSpacesAndLowercase() {
        assertEquals("penicillin v potassium", PatientAllergy.normalizeAllergenName("   Penicillin     V   Potassium   "));
        assertEquals("paracetamol", PatientAllergy.normalizeAllergenName("PARACETAMOL"));
        assertEquals("", PatientAllergy.normalizeAllergenName(null));
        assertEquals("", PatientAllergy.normalizeAllergenName("   "));
    }
}
