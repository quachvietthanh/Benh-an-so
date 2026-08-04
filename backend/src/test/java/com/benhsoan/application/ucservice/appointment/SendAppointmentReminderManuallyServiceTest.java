package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.port.dto.result.AppointmentReminderResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class SendAppointmentReminderManuallyServiceTest {

    @Mock private ProcessAppointmentReminderService processAppointmentReminderService;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @InjectMocks private SendAppointmentReminderManuallyService service;

    @Test
    void sendsImmediatelyAndWritesAuditLog() {
        UUID appointmentId = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-19T02:00:00Z");
        when(clockPort.now()).thenReturn(now);
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);
        when(processAppointmentReminderService.process(appointmentId, now))
                .thenReturn(AppointmentReminderResult.sent());

        AppointmentReminderResult result = service.sendManually(appointmentId);

        assertEquals("SENT", result.status());
        verify(processAppointmentReminderService).process(appointmentId, now);
        verify(auditLogRepository).save(org.mockito.ArgumentMatchers.any(AuditLog.class));
    }
}
