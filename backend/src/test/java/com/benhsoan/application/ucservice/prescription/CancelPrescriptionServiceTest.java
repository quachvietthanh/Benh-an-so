package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyCancelledException;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyDispensedException;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.prescription.exception.UnauthorizedPrescriptionCancellationException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CancelPrescriptionCommand;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class CancelPrescriptionServiceTest {

        private static final Instant NOW = Instant.parse("2026-08-20T02:30:00Z");
        private static final UUID DOCTOR_ID = UUID.randomUUID();

        private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        private final PrescriptionWarningLogRepository warningLogRepository = mock(
                        PrescriptionWarningLogRepository.class);
        private final PrescriptionClinicalContextValidator clinicalContextValidator = mock(
                        PrescriptionClinicalContextValidator.class);
        private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        private final ClockPort clockPort = mock(ClockPort.class);
        private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        private final PrescriptionAccessDeniedAuditWriter accessDeniedAuditWriter = mock(
                        PrescriptionAccessDeniedAuditWriter.class);
        private final PrescriptionResultMapper resultMapper = mock(PrescriptionResultMapper.class);
        private final ObjectMapper objectMapper = new ObjectMapper();

        private CancelPrescriptionService service;

        @BeforeEach
        void setUp() {
                service = new CancelPrescriptionService(
                                prescriptionRepository,
                                warningLogRepository,
                                clinicalContextValidator,
                                currentUserPort,
                                clockPort,
                                auditLogRepository,
                                accessDeniedAuditWriter,
                                resultMapper,
                                objectMapper);
                when(clockPort.now()).thenReturn(NOW);
        }

        @Test
        @DisplayName("Successfully cancels a prescription when doctor and clinical context are valid (TC-01, QTN-27, P1)")
        void cancel_success() {
                UUID prescriptionId = UUID.randomUUID();
                UUID medicalRecordId = UUID.randomUUID();
                Prescription prescription = createPendingPrescription(prescriptionId, medicalRecordId, DOCTOR_ID);

                when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
                when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
                when(prescriptionRepository.save(any(Prescription.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PrescriptionResult expectedResult = mock(PrescriptionResult.class);
                when(resultMapper.toResult(any(Prescription.class), any())).thenReturn(expectedResult);

                CancelPrescriptionCommand command = new CancelPrescriptionCommand(prescriptionId,
                                "Bệnh nhân đổi phác đồ điều trị");
                PrescriptionResult result = service.cancel(command);

                assertNotNull(result);
                assertEquals(expectedResult, result);
                assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());
                assertEquals("Bệnh nhân đổi phác đồ điều trị", prescription.getCancelReason());
                assertEquals(DOCTOR_ID, prescription.getUpdatedBy());
                assertEquals(NOW, prescription.getUpdatedAt());

                verify(clinicalContextValidator).requireDoctorPermissionForPrescriptionCancellation(medicalRecordId,
                                DOCTOR_ID);
                verify(prescriptionRepository).save(prescription);
                verify(auditLogRepository).save(argThat(log -> {
                        assertEquals(DOCTOR_ID, log.getUserId());
                        assertEquals(ActionType.CANCEL, log.getActionType());
                        assertEquals(ResourceType.PRESCRIPTION, log.getResourceType());
                        assertEquals(prescriptionId, log.getResourceId());
                        assertTrue(log.getDetail().contains("Bệnh nhân đổi phác đồ điều trị"));
                        assertTrue(log.getDetail().contains(prescription.getPrescriptionCode()));
                        return true;
                }));
        }

        @Test
        @DisplayName("Rejects cancellation when reason is null or blank (TC-02, QTN-27)")
        void cancel_missingReason() {
                UUID prescriptionId = UUID.randomUUID();

                assertThrows(ValidationException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, null)));
                assertThrows(ValidationException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "")));
                assertThrows(ValidationException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "   ")));

                verify(prescriptionRepository, never()).save(any());
                verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects cancellation when prescription id is null")
        void cancel_missingPrescriptionId() {
                assertThrows(ValidationException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(null, "Lý do hợp lệ")));
                assertThrows(ValidationException.class,
                                () -> service.cancel((CancelPrescriptionCommand) null));
        }

        @Test
        @DisplayName("Throws PrescriptionNotFoundException when prescription does not exist")
        void cancel_prescriptionNotFound() {
                UUID prescriptionId = UUID.randomUUID();
                when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
                when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.empty());

                assertThrows(PrescriptionNotFoundException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "Lý do hợp lệ")));
        }

        @Test
        @DisplayName("Rejects cancellation when user does not have DOCTOR role (TC-04)")
        void cancel_notDoctorRole() {
                UUID prescriptionId = UUID.randomUUID();
                when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

                assertThrows(AccessDeniedException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "Lý do hợp lệ")));

                verify(prescriptionRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("Rejects cancellation when doctor is not the prescribing doctor and logs access denied (TC-04, P2, P3)")
        void cancel_notPrescribingDoctor() {
                UUID prescriptionId = UUID.randomUUID();
                UUID otherDoctorId = UUID.randomUUID();
                Prescription prescription = createPendingPrescription(prescriptionId, UUID.randomUUID(), otherDoctorId);

                when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
                when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

                assertThrows(UnauthorizedPrescriptionCancellationException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "Lý do hợp lệ")));

                verify(accessDeniedAuditWriter).writeCancelDenied(
                                eq(DOCTOR_ID),
                                eq(prescriptionId),
                                eq(otherDoctorId),
                                eq(NOW),
                                eq("Attempted to cancel prescription prescribed by another doctor"));
                verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects cancellation when doctor is not responsible for the visit (P1 clinical context)")
        void cancel_doctorNotResponsibleForVisit() {
                UUID prescriptionId = UUID.randomUUID();
                UUID medicalRecordId = UUID.randomUUID();
                Prescription prescription = createPendingPrescription(prescriptionId, medicalRecordId, DOCTOR_ID);

                when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
                when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
                when(clinicalContextValidator.requireDoctorPermissionForPrescriptionCancellation(medicalRecordId,
                                DOCTOR_ID))
                                .thenThrow(new AccessDeniedException(
                                                "Only the doctor responsible for the visit can cancel prescriptions."));

                assertThrows(AccessDeniedException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "Lý do hợp lệ")));

                verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects cancellation when prescription is already dispensed (TC-03, QTN-27)")
        void cancel_alreadyDispensed() {
                UUID prescriptionId = UUID.randomUUID();
                UUID medicalRecordId = UUID.randomUUID();
                Prescription prescription = createPendingPrescription(prescriptionId, medicalRecordId, DOCTOR_ID);
                prescription.markDispensed(DOCTOR_ID, NOW);

                when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
                when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

                assertThrows(PrescriptionAlreadyDispensedException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "Lý do hợp lệ")));

                verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects cancellation when prescription is already cancelled")
        void cancel_alreadyCancelled() {
                UUID prescriptionId = UUID.randomUUID();
                UUID medicalRecordId = UUID.randomUUID();
                Prescription prescription = createPendingPrescription(prescriptionId, medicalRecordId, DOCTOR_ID);
                prescription.cancel("Đã hủy trước đó", DOCTOR_ID, NOW);

                when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
                when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
                when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

                assertThrows(PrescriptionAlreadyCancelledException.class,
                                () -> service.cancel(new CancelPrescriptionCommand(prescriptionId, "Lý do hợp lệ")));

                verify(prescriptionRepository, never()).save(any());
        }

        private Prescription createPendingPrescription(UUID prescriptionId, UUID medicalRecordId, UUID prescribedBy) {
                return Prescription.create(
                                prescriptionId, "RX000123", medicalRecordId, "Ghi chú điều trị", prescribedBy, NOW,
                                List.of(PrescriptionItem.create(
                                                UUID.randomUUID(), prescriptionId, UUID.randomUUID(),
                                                "Paracetamol 500 mg",
                                                "Paracetamol", "500 mg", "vien", "1 vien", 3,
                                                AdministrationRoute.ORAL, 3, 9, null, NOW)));
        }
}
