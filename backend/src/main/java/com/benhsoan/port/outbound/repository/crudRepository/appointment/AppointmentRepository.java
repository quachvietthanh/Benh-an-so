package com.benhsoan.port.outbound.repository.crudRepository.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface AppointmentRepository
        extends BaseRepository<Appointment, UUID> {

    Optional<Appointment> findByAppointmentCode(String appointmentCode);

    boolean existsByAppointmentCode(String appointmentCode);

    Optional<Appointment> findTopByOrderByAppointmentCodeDesc();

    Page<Appointment> search(SearchAppointmentCommand command);

    boolean existsActiveAppointmentConflict( UUID doctorId, Instant startTime, Instant endTime);

    Page<Appointment> findOverdue( Instant threshold, Pageable pageable );

}