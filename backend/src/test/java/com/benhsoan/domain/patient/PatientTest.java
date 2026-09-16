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
import com.benhsoan.domain.patient.enums.PatientStatus;
import com.benhsoan.domain.patient.exception.PatientAlreadyMergedException;
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
        patient.renewConsent("v1.0", renewTime);

        assertTrue(patient.isConsentAgreed());
        assertEquals(renewTime, patient.getConsentAgreedAt());
        assertEquals("v1.0", patient.getConsentVersion());
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

    @Test
    @DisplayName("NCL-02-CN-007 TC-01: Tạo và cập nhật hồ sơ với thông tin người liên hệ khẩn cấp đầy đủ (Họ tên, Mối quan hệ, SĐT)")
    void createAndUpdatePatientWithEmergencyContactAndRelationship() {
        Patient patient = Patient.create(
                "BN000002",
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "0901234567",
                "a@example.com",
                "123 Street",
                "079090001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Le Thi B",
                "Vợ",
                "0909998877",
                true,
                "v1.0",
                createdBy
        );

        assertEquals("Le Thi B", patient.getEmergencyContact());
        assertEquals("Vợ", patient.getEmergencyRelationship());
        assertEquals("0909998877", patient.getEmergencyPhone());

        patient.updateProfile(
                "Nguyen Van A",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "0901234567",
                "a@example.com",
                "123 Street",
                "079090001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van C",
                "Bố",
                "0908887766"
        );

        assertEquals("Nguyen Van C", patient.getEmergencyContact());
        assertEquals("Bố", patient.getEmergencyRelationship());
        assertEquals("0908887766", patient.getEmergencyPhone());
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-01 & TC-03: Tạo hồ sơ trẻ em thành công kèm người giám hộ và phiếu đồng ý đứng tên người giám hộ")
    void createMinorPatientWithGuardianSucceedsAndSetsConsentSigner() {
        Patient child = Patient.create(
                "BN-CHILD-01",
                "Bé Nguyễn Văn Con",
                LocalDate.of(2020, 5, 1),
                Gender.MALE,
                null,
                null,
                "123 Street",
                null,
                null,
                BloodType.A_POSITIVE,
                null,
                null,
                null,
                "Nguyễn Văn Bố",
                "Bố",
                "0912345678",
                "079090001234",
                null,
                null,
                true,
                "v1.0",
                createdBy
        );

        assertTrue(child.isMinor());
        assertEquals("Nguyễn Văn Bố", child.getGuardianName());
        assertEquals("Bố", child.getGuardianRelationship());
        assertEquals("0912345678", child.getGuardianPhone());
        assertEquals("079090001234", child.getGuardianIdentityNumber());
        assertEquals("Nguyễn Văn Bố", child.getConsentSignerName(), "Phiếu đồng ý phải đứng tên người giám hộ (TC-03)");
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-02 / QTN-44: Chặn tạo hồ sơ trẻ em khi thiếu người giám hộ")
    void createMinorPatientWithoutGuardianThrowsValidationException() {
        com.benhsoan.domain.shared.exception.ValidationException ex = assertThrows(
                com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> Patient.create(
                        "BN-CHILD-02",
                        "Bé Nguyễn Văn Con",
                        LocalDate.of(2020, 5, 1),
                        Gender.MALE,
                        null,
                        null,
                        "123 Street",
                        null,
                        null,
                        BloodType.A_POSITIVE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        "v1.0",
                        createdBy
                )
        );

        assertEquals("guardianName", ex.getField());
    }

    @Test
    @DisplayName("NCL-02-CN-008: Bệnh nhân đủ tuổi thành niên không bắt buộc người giám hộ và phiếu đồng ý đứng tên chính mình")
    void adultPatientRequiresNoGuardianAndDefaultsConsentSignerToSelf() {
        Patient adult = Patient.create(
                "BN-ADULT-01",
                "Trần Thị Trưởng Thành",
                LocalDate.of(1995, 1, 1),
                Gender.FEMALE,
                "0901234567",
                null,
                "123 Street",
                "079095001234",
                null,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                "v1.0",
                createdBy
        );

        assertFalse(adult.isMinor());
        assertNull(adult.getGuardianName());
        assertEquals("Trần Thị Trưởng Thành", adult.getConsentSignerName());
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-04: Nhắc chuyển sang tự chịu trách nhiệm khi đủ tuổi và hoàn tất chuyển đổi")
    void handlesAdultTransitionSuccessfully() {
        Patient patient = Patient.create(
                "BN-GROWING-01",
                "Lê Văn Trẻ",
                LocalDate.of(2005, 1, 1),
                Gender.MALE,
                "0901234567",
                null,
                "123 Street",
                null,
                null,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                "Lê Văn Cha",
                "Cha",
                "0912345678",
                null,
                null,
                null,
                true,
                "v1.0",
                createdBy
        );

        // Năm 2026: bệnh nhân 21 tuổi -> đủ tuổi thành niên nhưng còn người giám hộ
        assertTrue(patient.requiresAdultTransition(LocalDate.of(2026, 1, 1)));

        // Thực hiện chuyển đổi
        patient.transitionToAdult();

        assertNull(patient.getGuardianName());
        assertNull(patient.getGuardianRelationship());
        assertNull(patient.getGuardianPhone());
        assertEquals("Lê Văn Trẻ", patient.getConsentSignerName(), "Sau khi chuyển đổi, phiếu đồng ý đứng tên người bệnh");
        assertFalse(patient.requiresAdultTransition(LocalDate.of(2026, 1, 1)));
    }

    @Test
    @DisplayName("P1 / TC-04: transitionToAdult bảo toàn trạng thái rút consent (consentWithdrawn) và hạn chế sử dụng dữ liệu")
    void transitionToAdultPreservesWithdrawnConsentState() {
        Patient patient = Patient.create(
                "BN-ADULT-WITHDRAWN",
                "Trần Văn Lớn",
                LocalDate.of(2005, 1, 1),
                Gender.MALE,
                "0901112233",
                "lon@example.com",
                "123 Street",
                "079095001234",
                null,
                BloodType.A_POSITIVE,
                null,
                null,
                null,
                "Trần Văn Bố",
                "Bố",
                "0912345678",
                "001200000002",
                null,
                "Trần Văn Bố",
                true,
                "v1.0",
                createdBy
        );

        // Giám hộ từng rút consent
        patient.withdrawConsent("Không muốn chia sẻ dữ liệu nghiên cứu", Instant.now());
        assertTrue(patient.isConsentWithdrawn());
        assertTrue(patient.isNonMedicalUseRestricted());
        assertEquals("Không muốn chia sẻ dữ liệu nghiên cứu", patient.getConsentWithdrawnReason());

        // Chuyển sang thành niên
        patient.transitionToAdult();

        assertNull(patient.getGuardianName());
        assertNull(patient.getGuardianRelationship());
        assertNull(patient.getGuardianPhone());
        assertEquals("Trần Văn Lớn", patient.getConsentSignerName(), "Phiếu đồng ý đổi sang đứng tên chính bệnh nhân");
        // Quan trọng: trạng thái rút consent và hạn chế dữ liệu PHẢI được bảo toàn, không tự động re-grant
        assertTrue(patient.isConsentWithdrawn(), "consentWithdrawn không được tự động reset thành false");
        assertTrue(patient.isNonMedicalUseRestricted(), "nonMedicalUseRestricted không được tự động reset");
        assertEquals("Không muốn chia sẻ dữ liệu nghiên cứu", patient.getConsentWithdrawnReason());
    }

    @Test
    @DisplayName("TC-04 ngoại lệ: Từ chối chuyển đổi sang tự chịu trách nhiệm nếu bệnh nhân vẫn chưa đủ 18 tuổi")
    void rejectsAdultTransitionIfStillMinor() {
        Patient child = Patient.create(
                "BN-CHILD-03",
                "Bé Nhỏ",
                LocalDate.now().minusYears(10),
                Gender.MALE,
                null,
                null,
                "123 Street",
                null,
                null,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                "Nguyễn Văn Mẹ",
                "Mẹ",
                "0912345678",
                null,
                null,
                null,
                true,
                "v1.0",
                createdBy
        );

        assertThrows(
                com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> child.transitionToAdult()
        );
    }

    @Test
    @DisplayName("P1-2 / QTN-44: Chặn tạo hồ sơ trẻ em khi consentSignerName khác với guardianName")
    void createMinorPatientWithMismatchedConsentSignerThrowsValidationException() {
        com.benhsoan.domain.shared.exception.ValidationException ex = assertThrows(
                com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> Patient.create(
                        "BN-CHILD-04",
                        "Bé Nguyễn Văn Con",
                        LocalDate.of(2020, 5, 1),
                        Gender.MALE,
                        null,
                        null,
                        "123 Street",
                        null,
                        null,
                        BloodType.A_POSITIVE,
                        null,
                        null,
                        null,
                        "Nguyễn Văn Bố",
                        "Bố",
                        "0912345678",
                        null,
                        null,
                        "Người Ký Khác",
                        true,
                        "v1.0",
                        createdBy
                )
        );

        assertEquals("consentSignerName", ex.getField());
    }

    @Test
    @DisplayName("P1-1 / QTN-44: Chặn updateProfile hồ sơ trẻ em khi thiếu người giám hộ")
    void updateProfileMinorPatientWithoutGuardianThrowsValidationException() {
        Patient child = Patient.create(
                "BN-CHILD-05",
                "Bé Nhỏ",
                LocalDate.of(2020, 1, 1),
                Gender.MALE,
                null,
                null,
                "123 Street",
                null,
                null,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                "Nguyễn Văn Bố",
                "Bố",
                "0912345678",
                null,
                null,
                "Nguyễn Văn Bố",
                true,
                "v1.0",
                createdBy
        );

        com.benhsoan.domain.shared.exception.ValidationException ex = assertThrows(
                com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> child.updateProfile(
                        "Bé Nhỏ Đổi Tên",
                        LocalDate.of(2020, 1, 1),
                        Gender.MALE,
                        null,
                        null,
                        "123 Street",
                        null,
                        null,
                        BloodType.O_POSITIVE,
                        null,
                        null,
                        null,
                        null, // missing guardianName
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals("guardianName", ex.getField());
    }

    @Test
    @DisplayName("P1-2 / QTN-44: Chặn updateProfile hồ sơ trẻ em khi consentSignerName khác với guardianName")
    void updateProfileMinorPatientWithMismatchedConsentSignerThrowsValidationException() {
        Patient child = Patient.create(
                "BN-CHILD-06",
                "Bé Nhỏ",
                LocalDate.of(2020, 1, 1),
                Gender.MALE,
                null,
                null,
                "123 Street",
                null,
                null,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                "Nguyễn Văn Bố",
                "Bố",
                "0912345678",
                null,
                null,
                "Nguyễn Văn Bố",
                true,
                "v1.0",
                createdBy
        );

        com.benhsoan.domain.shared.exception.ValidationException ex = assertThrows(
                com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> child.updateProfile(
                        "Bé Nhỏ",
                        LocalDate.of(2020, 1, 1),
                        Gender.MALE,
                        null,
                        null,
                        "123 Street",
                        null,
                        null,
                        BloodType.O_POSITIVE,
                        null,
                        null,
                        null,
                        "Nguyễn Văn Bố",
                        "Bố",
                        "0912345678",
                        null,
                        null,
                        "Người Ký Giả Mạo"
                )
        );

        assertEquals("consentSignerName", ex.getField());
    }

    @Test
    @DisplayName("NCL-02-CN-006: Đánh dấu gộp hồ sơ thành công")
    void markAsMergedSucceeds() {
        Patient source = Patient.create(
                "BN-SRC", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", createdBy
        );

        UUID targetId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        assertEquals(PatientStatus.ACTIVE, source.getStatus());
        assertFalse(source.isMerged());

        source.markAsMerged(targetId, operatorId, "Hồ sơ trùng");

        assertEquals(PatientStatus.MERGED, source.getStatus());
        assertTrue(source.isMerged());
        assertFalse(source.isActive());
        assertEquals(targetId, source.getMergedIntoPatientId());
        assertEquals(operatorId, source.getMergedBy());
        assertEquals("Hồ sơ trùng", source.getMergeReason());
        assertNotNull(source.getMergedAt());

        assertThrows(PatientAlreadyMergedException.class,
                () -> source.markAsMerged(targetId, operatorId, "Thử gộp lại"));

        assertThrows(PatientAlreadyMergedException.class, source::validateCanBeUpdated);
    }

    @Test
    @DisplayName("UT-01 / F-02: patient.unlinkUser() đặt userId về null và cập nhật updatedAt")
    void unlinkUserSetsUserIdToNull() {
        Patient patient = Patient.create(
                "BN-SRC", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", createdBy
        );
        UUID userId = UUID.randomUUID();
        patient.linkUser(userId);
        assertEquals(userId, patient.getUserId());

        patient.unlinkUser();

        assertNull(patient.getUserId());
        assertNotNull(patient.getUpdatedAt());
    }
}
