package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
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
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.UpdateInstructionsAndTreatmentPlanCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateInstructionsAndTreatmentPlanService - Unit Tests (NCL-04-CN-010)")
class UpdateInstructionsAndTreatmentPlanServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();

    @InjectMocks private UpdateInstructionsAndTreatmentPlanService service;

    private final Instant now = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    @DisplayName("TC-01: Bác sĩ cập nhật thành công lời dặn, kế hoạch điều trị và mốc tái khám khi hồ sơ đang mở")
    void updatesInstructionsAndTreatmentPlanSuccessfully() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate revisitDate = LocalDate.of(2026, 8, 27);

        MedicalRecord record = MedicalRecord.create(visitId, "Khám tổng quát", null, null, null, null, null, null,
                "Bình thường", doctorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null, "Consultation", null, doctorId, now, now);

        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new UpdateInstructionsAndTreatmentPlanCommand(
                "Kế hoạch điều trị nội khoa ngoại trú 1 tuần",
                "Nghỉ ngơi 3 ngày, uống nhiều nước",
                revisitDate
        );

        MedicalRecordResult result = service.updateInstructionsAndTreatmentPlan(record.getId(), command);

        assertEquals(record.getId(), result.id());
        assertEquals("Nghỉ ngơi 3 ngày, uống nhiều nước", result.doctorInstructions());
        assertEquals("Kế hoạch điều trị nội khoa ngoại trú 1 tuần", result.treatmentPlan());
        assertEquals(revisitDate, result.revisitDate());

        verify(authorizationService).requireContentVisitWriteAccess(doctorId, visit.getDoctorId(), record.getId());
        verify(medicalRecordRepository).findByIdForUpdate(record.getId());
        verify(medicalRecordRepository).save(record);
        verify(accessAuditService).recordRecordAccess(patientId, visitId, record.getId(), doctorId,
                MedicalRecordAccessAction.UPDATE, "Doctor instructions and treatment plan updated", now);
    }

    @Test
    @DisplayName("TC-03 / QTN-07, QTN-18: Từ chối cập nhật trực tiếp khi hồ sơ đã ký hoặc khóa (MedicalRecordAlreadyLockedException -> 409)")
    void rejectsUpdateWhenRecordIsAlreadySignedOrLocked() {
        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        MedicalRecord record = MedicalRecord.create(visitId, "Khám tổng quát", null, null, null, null, null, null,
                "Bình thường", doctorId, now);
        record.sign("VALID_SIGNATURE_DATA", doctorId, now);

        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));

        var command = new UpdateInstructionsAndTreatmentPlanCommand(
                "Lời dặn mới",
                "Kế hoạch mới",
                LocalDate.of(2026, 8, 27)
        );

        assertThrows(MedicalRecordAlreadyLockedException.class,
                () -> service.updateInstructionsAndTreatmentPlan(record.getId(), command));

        verifyNoInteractions(visitRepository, accessAuditService, clockPort);
    }

    @Test
    @DisplayName("Từ chối cập nhật khi mốc tái khám nhỏ hơn ngày khám (ValidationException)")
    void rejectsUpdateWhenRevisitDateIsBeforeVisitDate() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        // visitAt is 2026-08-20, revisitDate is 2026-08-15 (in the past compared to visit)
        LocalDate invalidRevisitDate = LocalDate.of(2026, 8, 15);

        MedicalRecord record = MedicalRecord.create(visitId, "Khám tổng quát", null, null, null, null, null, null,
                "Bình thường", doctorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null, "Consultation", null, doctorId, now, now);

        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);

        var command = new UpdateInstructionsAndTreatmentPlanCommand(
                "Lời dặn",
                "Kế hoạch",
                invalidRevisitDate
        );

        assertThrows(ValidationException.class,
                () -> service.updateInstructionsAndTreatmentPlan(record.getId(), command));

        verifyNoInteractions(accessAuditService);
    }

    @Test
    @DisplayName("Từ chối khi bác sĩ không phải bác sĩ phụ trách lượt khám")
    void rejectsUpdateWhenDoctorIsNotAttendingDoctor() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID attendingDoctorId = UUID.randomUUID();
        UUID otherDoctorId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.create(visitId, "Khám tổng quát", null, null, null, null, null, null,
                "Bình thường", attendingDoctorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, attendingDoctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, now, now, null, "Consultation", null, attendingDoctorId, now, now);

        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(otherDoctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        doThrow(new MedicalRecordAccessDeniedException()).when(authorizationService)
                .requireContentVisitWriteAccess(otherDoctorId, attendingDoctorId, record.getId());

        var command = new UpdateInstructionsAndTreatmentPlanCommand("Lời dặn", "Kế hoạch", null);

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> service.updateInstructionsAndTreatmentPlan(record.getId(), command));

        verifyNoInteractions(accessAuditService, clockPort);
    }

    @Test
    @DisplayName("Từ chối khi lượt khám đã kết thúc")
    void rejectsUpdateWhenVisitIsCompleted() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.create(visitId, "Khám tổng quát", null, null, null, null, null, null,
                "Bình thường", doctorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED, now, now, now, "Consultation", null, doctorId, now, now);

        when(authorizationService.requireContentWriteAccess(record.getId())).thenReturn(doctorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        var command = new UpdateInstructionsAndTreatmentPlanCommand("Lời dặn", "Kế hoạch", null);

        assertThrows(MedicalRecordInvalidVisitException.class,
                () -> service.updateInstructionsAndTreatmentPlan(record.getId(), command));

        verifyNoInteractions(accessAuditService, clockPort);
    }

    @Test
    @DisplayName("Ném MedicalRecordNotFoundException khi hồ sơ bệnh án không tồn tại")
    void throwsNotFoundWhenRecordDoesNotExist() {
        UUID recordId = UUID.randomUUID();
        when(authorizationService.requireContentWriteAccess(recordId)).thenReturn(UUID.randomUUID());
        when(medicalRecordRepository.findByIdForUpdate(recordId)).thenReturn(Optional.empty());

        var command = new UpdateInstructionsAndTreatmentPlanCommand("Lời dặn", "Kế hoạch", null);

        assertThrows(MedicalRecordNotFoundException.class,
                () -> service.updateInstructionsAndTreatmentPlan(recordId, command));

        verifyNoInteractions(visitRepository, accessAuditService, clockPort);
    }
}
