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

    List<Appointment> findByPatientIdOrderByStartTimeAsc(UUID patientId);

    List<Appointment> findByPatientIdAndStatusInOrderByStartTimeAsc(
            UUID patientId,
            Collection<AppointmentStatus> statuses
    );

    Appointment save(Appointment appointment);

    Optional<Appointment> findByAppointmentCode(String appointmentCode);

    boolean existsByAppointmentCode(String appointmentCode);

    Optional<Appointment> findTopByOrderByAppointmentCodeDesc();

    Optional<String> findAppointmentCodeWithHighestSequence();

    Page<Appointment> search(SearchAppointmentCommand command);

    boolean existsActiveAppointmentConflict( UUID doctorId, Instant startTime, Instant endTime);

    List<Appointment> findActiveAppointmentsForDoctorBetween(
            UUID doctorId,
            Instant from,
            Instant to
    );

    Page<Appointment> findOverdue( Instant threshold, Pageable pageable );

    List<UUID> findDueReminderIds(
            Instant now,
            Instant reminderDeadline,
            Collection<AppointmentStatus> statuses
    );

    Optional<Appointment> findByIdForUpdate(UUID id);

}
