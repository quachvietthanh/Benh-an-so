package com.benhsoan.application.ucservice.appointment;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.appointment.exception.WaitlistEntryNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.command.appointment.CancelWaitlistEntryCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.inbound.appointment.CancelWaitlistEntryUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelWaitlistEntryService implements CancelWaitlistEntryUseCase {

    private final AppointmentWaitlistRepository appointmentWaitlistRepository;
    private final AppointmentWaitlistResultMapper resultMapper;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public AppointmentWaitlistResult cancel(CancelWaitlistEntryCommand command) {
        validatePermission();
        Objects.requireNonNull(command.id(), "ID danh sách chờ không được để trống");

        AppointmentWaitlist waitlist = appointmentWaitlistRepository.findByIdForUpdate(command.id())
                .orElseThrow(() -> new WaitlistEntryNotFoundException("Không tìm thấy mục danh sách chờ với ID: " + command.id()));

        waitlist.cancel(command.reason(), clockPort.now());
        AppointmentWaitlist saved = appointmentWaitlistRepository.save(waitlist);

        UUID currentUserId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.CANCEL,
                        ResourceType.APPOINTMENT_WAITLIST,
                        saved.getId(),
                        """
                        {
                          "waitlistId":"%s",
                          "cancelReason":"%s"
                        }
                        """.formatted(saved.getId(), command.reason()),
                        null
                )
        );

        return resultMapper.toResult(saved);
    }

    private void validatePermission() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedAppointmentOperationException();
        }
    }
}
