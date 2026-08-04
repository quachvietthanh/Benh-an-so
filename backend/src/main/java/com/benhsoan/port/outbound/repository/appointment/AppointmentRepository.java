package com.benhsoan.port.outbound.repository.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
public interface AppointmentRepository {

    Optional<Appointment> findById(UUID id);

    Appointment save(Appointment appointment);

    Optional<Appointment> findByAppointmentCode(String appointmentCode);

    boolean existsByAppointmentCode(String appointmentCode);

    Optional<Appointment> findTopByOrderByAppointmentCodeDesc();

    Page<Appointment> search(SearchAppointmentCommand command);

    boolean existsActiveAppointmentConflict( UUID doctorId, Instant startTime, Instant endTime);

    Page<Appointment> findOverdue( Instant threshold, Pageable pageable );

    List<UUID> findDueReminderIds(
            Instant now,
            Instant reminderDeadline,
            Collection<AppointmentStatus> statuses
    );

    Optional<Appointment> findByIdForUpdate(UUID id);

}
