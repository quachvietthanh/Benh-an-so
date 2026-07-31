package com.benhsoan.application.ucservice.queue;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.dto.command.queue.CheckInAppointmentCommand;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.inbound.queue.CheckInAppointmentUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckInAppointmentService implements CheckInAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final QueueCheckInCoordinator queueCheckInCoordinator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public QueueCheckInResult checkIn(CheckInAppointmentCommand command) {
        requireReceptionPermission();
        Appointment appointment = appointmentRepository.findByIdForUpdate(command.appointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(command.appointmentId()));
        Instant checkedInAt = clockPort.now();
        appointment.checkIn(checkedInAt);

        QueueCheckInResult result = queueCheckInCoordinator.checkIn(appointment.getPatientId(), appointment.getDoctorId(),
                appointment.getId(), QueueItemSourceType.APPOINTMENT, appointment.getReason(), null,
                currentUserPort.getCurrentUserId(), checkedInAt);
        appointmentRepository.save(appointment);
        return result;
    }

    private void requireReceptionPermission() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedQueueOperationException();
        }
    }
}
