package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.shared.exception.ValidationException;

@DisplayName("MedicalRecord - Domain Entity Tests")
class MedicalRecordTest {

    private final Instant now = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    @DisplayName("Ký bệnh án thành công: chuyển sang SIGNED, lưu chữ ký và khóa nội dung")
    void signsAndStoresSignatureDataAndLocksContent() {
        MedicalRecord record = recordWithRequiredContent();
        UUID doctorId = UUID.randomUUID();
        String signature = "DR_SIGNATURE_BASE64_SIMULATED";

        record.sign(signature, doctorId, now);

        assertTrue(record.isSigned());
        assertTrue(record.isContentLocked());
        assertEquals(MedicalRecordStatus.SIGNED, record.getStatus());
        assertEquals(signature, record.getSignatureData());
        assertEquals(doctorId, record.getSignedBy());
        assertEquals(now, record.getSignedAt());

        // Chặn sửa trực tiếp nội dung khi đã ký
        assertThrows(MedicalRecordAlreadyLockedException.class, () -> record.updateContent(
                "Updated complaint", null, null, null, null, null, null, "Updated conclusion",
                UUID.randomUUID(), now.plusSeconds(2)
        ));

        // Chặn ký lại khi đã ký
        assertThrows(MedicalRecordAlreadyLockedException.class, () -> record.sign(
                "ANOTHER_SIGNATURE", doctorId, now.plusSeconds(1)
        ));
    }

    @Test
    @DisplayName("Khóa bệnh án: Bắt buộc bệnh án phải được ký trước đó (QTN-17)")
    void rejectsLockingRecordWhenNotSigned() {
        MedicalRecord record = recordWithRequiredContent();
        record.open(UUID.randomUUID(), now);

        // Chưa ký mà khóa -> ném MedicalRecordNotSignedException
        assertThrows(MedicalRecordNotSignedException.class, () -> record.lock(UUID.randomUUID(), now));
    }

    @Test
    @DisplayName("Khóa bệnh án đã ký: chuyển sang LOCKED, lưu thông tin lockedBy và lockedAt")
    void locksSignedRecordSuccessfully() {
        MedicalRecord record = recordWithRequiredContent();
        UUID doctorId = UUID.randomUUID();
        UUID lockActorId = UUID.randomUUID();

        record.sign("SIGNATURE_OK", doctorId, now);
        record.lock(lockActorId, now.plusSeconds(10));

        assertTrue(record.isLocked());
        assertTrue(record.isContentLocked());
        assertEquals(MedicalRecordStatus.LOCKED, record.getStatus());
        assertEquals(lockActorId, record.getLockedBy());
        assertEquals(now.plusSeconds(10), record.getLockedAt());
    }

    @Test
    @DisplayName("Từ chối ký khi thiếu thông tin bắt buộc (chief complaint / conclusion)")
    void rejectsSigningRecordWithoutRequiredContent() {
        MedicalRecord record = MedicalRecord.create(
                UUID.randomUUID(), null, null, null, null, null, null, null, null,
                UUID.randomUUID(), now
        );

        assertThrows(ValidationException.class, () -> record.sign("SIG", UUID.randomUUID(), now));
    }

    @Test
    @DisplayName("Required template sections are validated only when the record is signed")
    void rejectsSigningWhenAppliedTemplateRequiredSectionIsBlank() {
        MedicalRecord record = recordWithRequiredContent();
        UUID doctorId = UUID.randomUUID();
        MedicalRecordTemplateVersion version = MedicalRecordTemplateVersion.create(
                UUID.randomUUID(), 1, UUID.randomUUID(), "Initial", null, doctorId, now,
                List.of(new MedicalRecordTemplateVersion.SectionDefinition(
                        MedicalRecordFieldCode.PHYSICAL_EXAMINATION, "Physical examination", true, 1)));

        record.applyTemplateVersion(version.getId(), doctorId, now);

        assertThrows(ValidationException.class, () -> record.ensureRequiredTemplateSections(version));
        assertEquals(MedicalRecordStatus.DRAFT, record.getStatus());
    }

    private MedicalRecord recordWithRequiredContent() {
        return MedicalRecord.create(
                UUID.randomUUID(), "Headache", null, null, null, null, null, null, "Stable",
                UUID.randomUUID(), now
        );
    }
}
