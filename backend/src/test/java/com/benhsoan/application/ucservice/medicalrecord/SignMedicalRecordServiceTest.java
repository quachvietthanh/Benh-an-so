package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordMissingDiagnosisException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedSignerException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignMedicalRecordService - Unit Tests (NCL-11-CN-001)")
class SignMedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private MedicalRecordTemplateRepository medicalRecordTemplateRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private MedicalRecordTemplateApplicationMapper templateMapper;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();

    @InjectMocks private SignMedicalRecordService service;

    private final UUID doctorId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-21T08:00:00Z");

    private Visit activeVisit(UUID doctor) {
        return Visit.restore(
                visitId, "VIS-001", patientId, doctor, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null,
                "Consultation", null, doctor, now, now
        );
    }

    private MedicalRecord openRecord() {
        return MedicalRecord.restore(
                UUID.randomUUID(), visitId, "Headache", "Fever", "None",
                "Normal", "Stable", "Rest", "Drink water", "Acute flu",
                MedicalRecordStatus.OPEN, null, null, null, null, null,
                doctorId, now, doctorId, now
        );
    }

    @Test
    @DisplayName("TC-01: Bác sĩ phụ trách ký bệnh án thành công khi đã có chẩn đoán -> chuyển SIGNED, lưu chữ ký và khóa nội dung")
    void tc01_successSigningChangesStatusToSignedAndLocksContent() {
        MedicalRecord record = openRecord();
        Visit visit = activeVisit(doctorId);

        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.existsByMedicalRecordId(record.getId())).thenReturn(true);
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArgument(0));

        SignMedicalRecordCommand command = new SignMedicalRecordCommand("DR_SIG_DATA_123");
        var result = service.sign(record.getId(), command);

        assertEquals(MedicalRecordStatus.SIGNED, result.status());
        assertEquals("DR_SIG_DATA_123", result.signatureData());
        assertEquals(doctorId, result.signedBy());
        assertEquals(now, result.signedAt());

        verify(accessAuditService).recordRecordAccess(
                patientId, visitId, record.getId(), doctorId,
                MedicalRecordAccessAction.SIGN, "Medical record signed", now
        );
    }

    @Test
    @DisplayName("TC-02: Ký bệnh án khi chưa có chẩn đoán -> bị từ chối với MedicalRecordMissingDiagnosisException")
    void tc02_missingDiagnosisRejectsSigning() {
        MedicalRecord record = openRecord();
        Visit visit = activeVisit(doctorId);

        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.existsByMedicalRecordId(record.getId())).thenReturn(false);
        when(clockPort.now()).thenReturn(now);

        assertThrows(MedicalRecordMissingDiagnosisException.class,
                () -> service.sign(record.getId(), new SignMedicalRecordCommand(null)));

        verifyNoInteractions(accessAuditService);
    }

    @Test
    @DisplayName("TC-03: Người dùng không phải bác sĩ phụ trách ký -> bị từ chối và ghi nhật ký truy cập SIGN")
    void tc03_unauthorizedDoctorRejectsAndWritesAuditLog() {
        MedicalRecord record = openRecord();
        UUID otherDoctorId = UUID.randomUUID();
        Visit visit = activeVisit(doctorId);

        when(authorizationService.requireWriteAccess()).thenReturn(otherDoctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);

        assertThrows(MedicalRecordUnauthorizedSignerException.class,
                () -> service.sign(record.getId(), new SignMedicalRecordCommand(null)));

        verify(accessAuditService).recordRecordAccess(
                patientId, visitId, record.getId(), otherDoctorId,
                MedicalRecordAccessAction.SIGN, "Signature rejected: User is not doctor in charge", now
        );
    }

    @Test
    @DisplayName("TC-04: Ký bệnh án khi đã ở trạng thái SIGNED hoặc LOCKED -> bị từ chối với MedicalRecordAlreadyLockedException")
    void tc04_alreadySignedOrLockedRecordRejectsReSigning() {
        MedicalRecord record = openRecord();
        record.sign("EXISTING_SIG", doctorId, now);

        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));

        assertThrows(MedicalRecordAlreadyLockedException.class,
                () -> service.sign(record.getId(), new SignMedicalRecordCommand("NEW_SIG")));

        verifyNoInteractions(accessAuditService, visitRepository);
    }

    @Test
    @DisplayName("Ký bệnh án khi không tìm thấy record -> ném MedicalRecordNotFoundException")
    void recordNotFoundThrowsException() {
        UUID recordId = UUID.randomUUID();
        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(recordId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class,
                () -> service.sign(recordId, new SignMedicalRecordCommand(null)));
    }

    @Test
    @DisplayName("Ký bệnh án khi lượt khám đã bị hủy -> ném MedicalRecordInvalidVisitException")
    void cancelledVisitThrowsException() {
        MedicalRecord record = openRecord();
        Visit visit = Visit.restore(
                visitId, "VIS-001", patientId, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.CANCELLED, now, now, null,
                "Consultation", null, doctorId, now, now
        );

        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordInvalidVisitException.class,
                () -> service.sign(record.getId(), new SignMedicalRecordCommand(null)));
    }

    @Test
    @DisplayName("Tự sinh simulated signature nếu command signatureData để trống")
    void simulatedSignatureGeneratedWhenBlank() {
        MedicalRecord record = openRecord();
        Visit visit = activeVisit(doctorId);

        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.existsByMedicalRecordId(record.getId())).thenReturn(true);
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.sign(record.getId(), new SignMedicalRecordCommand("   "));

        assertEquals(MedicalRecordStatus.SIGNED, result.status());
        assertNotNull(result.signatureData());
        assertEquals("SIMULATED_SIGNATURE:" + doctorId + ":" + now.toEpochMilli(), result.signatureData());
    }

    @Test
    @DisplayName("Required section of applied template is checked at signing, not while drafting")
    void requiredTemplateSectionRejectsSigningWhenBlank() {
        MedicalRecord record = openRecord();
        Visit visit = activeVisit(doctorId);
        record.updateContent("Headache", "Fever", "None", null, "Stable", "Rest", "Drink water", "Acute flu",
                doctorId, now);
        MedicalRecordTemplateVersion version = MedicalRecordTemplateVersion.create(
                UUID.randomUUID(), 1, UUID.randomUUID(), "Initial", null, doctorId, now,
                List.of(new MedicalRecordTemplateVersion.SectionDefinition(
                        MedicalRecordFieldCode.PHYSICAL_EXAMINATION, "Physical examination", true, 1)));
        record.applyTemplateVersion(version.getId(), doctorId, now);

        when(authorizationService.requireWriteAccess()).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.existsByMedicalRecordId(record.getId())).thenReturn(true);
        when(medicalRecordTemplateRepository.findVersionById(version.getId())).thenReturn(Optional.of(version));
        when(clockPort.now()).thenReturn(now);

        assertThrows(com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> service.sign(record.getId(), new SignMedicalRecordCommand("signature")));

        verifyNoInteractions(accessAuditService);
    }
}
