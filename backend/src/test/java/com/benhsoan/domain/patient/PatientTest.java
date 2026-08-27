package com.benhsoan.domain.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.exception.PatientConsentRequiredException;

@DisplayName("Patient Domain Entity - Unit Tests (NCL-15-CN-001 / QTN-24)")
class PatientTest {

    private final UUID createdBy = UUID.randomUUID();

    @Test
    @DisplayName("TC-01: Tạo hồ sơ thành công khi có sự đồng ý xử lý dữ liệu (consentAgreed = true)")
    void createPatientWithConsentSucceeds() {
        Patient patient = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "0901234567",
                "a@example.com",
                "123 Street",
                "079090001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                createdBy
        );

        assertNotNull(patient.getId());
        assertEquals("BN000001", patient.getPatientCode());
        assertEquals("Nguyen Van A", patient.getFullName());
        assertTrue(patient.isConsentAgreed());
        assertNotNull(patient.getConsentAgreedAt());
        assertEquals("v1.0", patient.getConsentVersion());
        assertFalse(patient.isConsentWithdrawn());
        assertNull(patient.getConsentWithdrawnAt());
        assertNull(patient.getConsentWithdrawnReason());
        assertFalse(patient.isNonMedicalUseRestricted());
        assertTrue(patient.isActive());
    }

    @Test
    @DisplayName("TC-02 / QTN-24: Chặn tạo hồ sơ khi không có sự đồng ý (consentAgreed = false)")
    void createPatientWithoutConsentThrowsException() {
        assertThrows(PatientConsentRequiredException.class, () -> Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "0901234567",
                "a@example.com",
                "123 Street",
                "079090001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                false,
                "v1.0",
                createdBy
        ));
    }

    @Test
    @DisplayName("TC-03: Rút lại sự đồng ý đánh dấu hồ sơ ngừng dùng ngoài KCB nhưng vẫn active")
    void withdrawConsentRestrictsNonMedicalUse() {
        Patient patient = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "0901234567",
                "a@example.com",
                "123 Street",
                "079090001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                createdBy
        );

        Instant withdrawTime = Instant.now();
        patient.withdrawConsent("Không muốn nhận tin tiếp thị", withdrawTime);

        assertTrue(patient.isConsentWithdrawn());
        assertEquals(withdrawTime, patient.getConsentWithdrawnAt());
        assertEquals("Không muốn nhận tin tiếp thị", patient.getConsentWithdrawnReason());
        assertTrue(patient.isNonMedicalUseRestricted());
        assertTrue(patient.isActive(), "Hồ sơ vẫn active phục vụ khám chữa bệnh");
    }

    @Test
    @DisplayName("Phục hồi sự đồng ý khi người bệnh đồng ý lại")
    void renewConsentRestoresConsentState() {
        Patient patient = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "0901234567",
                "a@example.com",
                "123 Street",
                "079090001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                createdBy
        );

        patient.withdrawConsent("Lý do cá nhân", Instant.now());
        assertTrue(patient.isConsentWithdrawn());
        assertTrue(patient.isNonMedicalUseRestricted());

        Instant renewTime = Instant.now();
        patient.renewConsent("v2.0", renewTime);

        assertTrue(patient.isConsentAgreed());
        assertEquals(renewTime, patient.getConsentAgreedAt());
        assertEquals("v2.0", patient.getConsentVersion());
        assertFalse(patient.isConsentWithdrawn());
        assertNull(patient.getConsentWithdrawnAt());
        assertNull(patient.getConsentWithdrawnReason());
        assertFalse(patient.isNonMedicalUseRestricted());
    }

    @Test
    @DisplayName("P1 Fix: Khôi phục hồ sơ bệnh nhân lịch sử mặc định trạng thái unconsented")
    void restoreHistoricalPatientDefaultsToUnconsentedState() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Patient restored = Patient.restore(
                id,
                "BN-OLD-01",
                "Tran Thi C",
                LocalDate.of(1985, 3, 15),
                Gender.FEMALE,
                "0908887766",
                "c@example.com",
                "456 Street",
                "079085009999",
                "DN4790850099999",
                BloodType.A_POSITIVE,
                "Tran Van D",
                "0907776655",
                true,
                now,
                now,
                null,
                createdBy
        );

        assertNotNull(restored);
        assertEquals(id, restored.getId());
        assertEquals("BN-OLD-01", restored.getPatientCode());
        assertFalse(restored.isConsentAgreed(), "Dữ liệu lịch sử chưa có consent phải là false");
        assertNull(restored.getConsentAgreedAt(), "Dữ liệu lịch sử chưa có consentAgreedAt phải là null");
        assertNull(restored.getConsentVersion());
        assertFalse(restored.isConsentWithdrawn());
        assertFalse(restored.isNonMedicalUseRestricted());
    }
}
