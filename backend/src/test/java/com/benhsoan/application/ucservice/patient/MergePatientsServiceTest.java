package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.enums.PatientStatus;
import com.benhsoan.domain.patient.exception.CannotMergeSamePatientException;
import com.benhsoan.domain.patient.exception.PatientAlreadyMergedException;
import com.benhsoan.domain.patient.exception.PatientIdentityConflictException;
import com.benhsoan.port.dto.command.patient.MergePatientsCommand;
import com.benhsoan.port.dto.result.patient.MergePatientsResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientMergeDataPort;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("MergePatientsService - Unit Tests (NCL-02-CN-006 / TC-01, TC-03, TC-05, QTN-33)")
class MergePatientsServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientMergeDataPort patientMergeDataPort;
    @Mock private PatientChangeLogRepository patientChangeLogRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;

    private MergePatientsService service;
    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MergePatientsService(
                patientRepository,
                patientMergeDataPort,
                auditLogRepository,
                patientChangeLogRepository,
                currentUserPort
        );
    }

    @Test
    @DisplayName("TC-01 & TC-05: Gộp thành công 2 hồ sơ trùng -> Chuyển toàn bộ dữ liệu, đổi trạng thái hồ sơ nguồn sang MERGED và ghi audit log")
    void successfulMerge_TransfersData_MarksSourceMerged_LogsAuditAndChangeLog() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Patient sourcePatient = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        sourcePatient.setIdForTest(sourceId);

        Patient targetPatient = Patient.create(
                "BN000002", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        targetPatient.setIdForTest(targetId);

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(patientRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(sourcePatient));
        when(patientRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(targetPatient));
        when(patientMergeDataPort.transferAllPatientData(sourceId, targetId)).thenReturn(3);
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

        MergePatientsCommand command = new MergePatientsCommand(sourceId, targetId, "Gộp hồ sơ do nhập trùng lúc tiếp đón");
        MergePatientsResult result = service.merge(command);

        // Verify result
        assertNotNull(result);
        assertEquals(sourceId, result.sourcePatientId());
        assertEquals("BN000001", result.sourcePatientCode());
        assertEquals(targetId, result.targetPatientId());
        assertEquals("BN000002", result.targetPatientCode());
        assertEquals(3, result.transferredVisitsCount());
        assertEquals(currentUserId, result.mergedBy());
        assertEquals("Gộp hồ sơ do nhập trùng lúc tiếp đón", result.reason());
        assertNotNull(result.mergedAt());

        // Verify source patient is marked as MERGED
        assertEquals(PatientStatus.MERGED, sourcePatient.getStatus());
        assertTrue(sourcePatient.isMerged());
        assertFalse(sourcePatient.isActive());
        assertEquals(targetId, sourcePatient.getMergedIntoPatientId());
        assertEquals(currentUserId, sourcePatient.getMergedBy());

        // Verify transfer was called
        verify(patientMergeDataPort).transferAllPatientData(sourceId, targetId);

        // Verify saves
        verify(patientRepository).save(sourcePatient);

        // Verify change logs (both source and target)
        ArgumentCaptor<PatientChangeLog> changeLogCaptor = ArgumentCaptor.forClass(PatientChangeLog.class);
        verify(patientChangeLogRepository, times(2)).save(changeLogCaptor.capture());
        PatientChangeLog sourceChangeLog = changeLogCaptor.getAllValues().get(0);
        assertEquals(PatientChangeAction.MERGE, sourceChangeLog.getAction());

        // Verify AuditLog (fulfills TC-05)
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog auditLog = auditLogCaptor.getValue();
        assertEquals(ActionType.MERGE, auditLog.getActionType());
        assertEquals(ResourceType.PATIENT, auditLog.getResourceType());
        assertEquals(targetId, auditLog.getResourceId());
        assertEquals(currentUserId, auditLog.getUserId());
        assertTrue(auditLog.getDetail().contains("BN000001"));
        assertTrue(auditLog.getDetail().contains("BN000002"));
    }

    @Test
    @DisplayName("NCL-02-CN-006: Chặn gộp chính mình (sourceId == targetId)")
    void cannotMergeSamePatient_ThrowsException() {
        UUID patientId = UUID.randomUUID();
        MergePatientsCommand command = new MergePatientsCommand(patientId, patientId, "Trùng ID");

        assertThrows(CannotMergeSamePatientException.class, () -> service.merge(command));
        verify(patientRepository, never()).findByIdForUpdate(any());
        verify(patientMergeDataPort, never()).transferAllPatientData(any(), any());
    }

    @Test
    @DisplayName("NCL-02-CN-006: Chặn gộp khi hồ sơ nguồn đã ở trạng thái MERGED")
    void sourcePatientAlreadyMerged_ThrowsException() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Patient sourcePatient = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        sourcePatient.setIdForTest(sourceId);
        sourcePatient.markAsMerged(UUID.randomUUID(), currentUserId, "Đã gộp trước đó");

        Patient targetPatient = Patient.create(
                "BN000002", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        targetPatient.setIdForTest(targetId);

        when(patientRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(sourcePatient));
        when(patientRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(targetPatient));

        MergePatientsCommand command = new MergePatientsCommand(sourceId, targetId, "Thử gộp lại");

        assertThrows(PatientAlreadyMergedException.class, () -> service.merge(command));
        verify(patientMergeDataPort, never()).transferAllPatientData(any(), any());
    }

    @Test
    @DisplayName("NCL-02-CN-006: Chặn gộp khi hồ sơ đích đã ở trạng thái MERGED")
    void targetPatientAlreadyMerged_ThrowsException() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Patient sourcePatient = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        sourcePatient.setIdForTest(sourceId);

        Patient targetPatient = Patient.create(
                "BN000002", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        targetPatient.setIdForTest(targetId);
        targetPatient.markAsMerged(UUID.randomUUID(), currentUserId, "Target đã gộp vào hồ sơ khác");

        when(patientRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(sourcePatient));
        when(patientRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(targetPatient));

        MergePatientsCommand command = new MergePatientsCommand(sourceId, targetId, "Thử gộp vào target đã gộp");

        assertThrows(PatientAlreadyMergedException.class, () -> service.merge(command));
        verify(patientMergeDataPort, never()).transferAllPatientData(any(), any());
    }

    @Test
    @DisplayName("QTN-33: Chặn gộp khi có xung đột định danh (khác ngày sinh) và hồ sơ nguồn đã có bệnh án hoàn tất/ký khóa")
    void identityConflictWithFinalizedMedicalRecords_ThrowsException() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Patient sourcePatient = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        sourcePatient.setIdForTest(sourceId);

        Patient targetPatient = Patient.create(
                "BN000002", "Nguyen Van A", LocalDate.of(1992, 5, 10), Gender.MALE, // khác ngày sinh
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        targetPatient.setIdForTest(targetId);

        when(patientRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(sourcePatient));
        when(patientRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(targetPatient));
        when(patientMergeDataPort.hasFinalizedMedicalRecords(sourceId)).thenReturn(true);
        when(patientMergeDataPort.hasFinalizedMedicalRecords(targetId)).thenReturn(true);

        MergePatientsCommand command = new MergePatientsCommand(sourceId, targetId, "Gộp hồ sơ khác ngày sinh có bệnh án khóa");

        assertThrows(PatientIdentityConflictException.class, () -> service.merge(command));
        verify(patientMergeDataPort, never()).transferAllPatientData(any(), any());
    }
}
