package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.exception.UnauthorizedPrescriptionCancellationException;
import com.benhsoan.port.dto.command.prescription.CancelPrescriptionCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class CancelPrescriptionAuditIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:30:00Z");

    @Test
    @DisplayName("Audit log is written when Doctor B attempts to cancel Doctor A's prescription (P2-02, TC-04)")
    void accessDeniedAuditWrittenWhenDifferentDoctorAttemptsCancel() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PrescriptionWarningLogRepository warningLogRepository = mock(PrescriptionWarningLogRepository.class);
        PrescriptionClinicalContextValidator clinicalContextValidator = mock(PrescriptionClinicalContextValidator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        PrescriptionResultMapper resultMapper = mock(PrescriptionResultMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PrescriptionAccessDeniedAuditWriter auditWriter =
                new PrescriptionAccessDeniedAuditWriter(auditLogRepository, objectMapper);

        CancelPrescriptionService service = new CancelPrescriptionService(
                prescriptionRepository,
                warningLogRepository,
                clinicalContextValidator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                auditWriter,
                resultMapper,
                objectMapper
        );

        UUID doctorA = UUID.randomUUID();
        UUID doctorB = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();

        Prescription prescription = Prescription.create(
                prescriptionId, "RX000999", UUID.randomUUID(), "Ghi chú", doctorA, NOW,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Thuốc A",
                        "Hoạt chất", "500 mg", "viên", "1 viên", 2,
                        AdministrationRoute.ORAL, 2, 4, null, NOW
                ))
        );

        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorB);
        when(clockPort.now()).thenReturn(NOW);
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

        CancelPrescriptionCommand command = new CancelPrescriptionCommand(prescriptionId, "Bác sĩ B muốn hủy");

        assertThrows(UnauthorizedPrescriptionCancellationException.class, () -> service.cancel(command));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog savedAudit = captor.getValue();

        assertNotNull(savedAudit);
        assertEquals(doctorB, savedAudit.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, savedAudit.getActionType());
        assertEquals(ResourceType.PRESCRIPTION, savedAudit.getResourceType());
        assertEquals(prescriptionId, savedAudit.getResourceId());
        assertTrue(savedAudit.getDetail().contains(doctorA.toString()));
        assertTrue(savedAudit.getDetail().contains("Attempted to cancel prescription prescribed by another doctor"));

        verify(prescriptionRepository, never()).save(any());
    }
}
