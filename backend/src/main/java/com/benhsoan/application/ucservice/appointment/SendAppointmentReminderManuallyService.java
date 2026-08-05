package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.AppointmentReminderResult;
import com.benhsoan.port.inbound.appointment.SendAppointmentReminderManuallyUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SendAppointmentReminderManuallyService
        implements SendAppointmentReminderManuallyUseCase {

    private final ProcessAppointmentReminderService processAppointmentReminderService;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public AppointmentReminderResult sendManually(UUID appointmentId) {
        Instant now = clockPort.now();
        AppointmentReminderResult result = processAppointmentReminderService.process(appointmentId, now);
        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(), ActionType.UPDATE,
                ResourceType.APPOINTMENT, appointmentId, null,
                "{\"action\":\"MANUAL_APPOINTMENT_REMINDER\",\"result\":\""
                        + result.status() + "\"}"));
        return result;
    }
}
