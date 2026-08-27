package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.jpaRepository.appointment.AppointmentBusinessSpecification;
import com.benhsoan.persistence.jpaRepository.appointment.AppointmentSearchSpecification;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentPersistenceMapper;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryAdapter
        implements AppointmentRepository {

    private final JpaAppointmentRepository jpaRepository;

    private final AppointmentPersistenceMapper mapper;

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Appointment save(Appointment appointment) {

        AppointmentEntity entity
                = mapper.toEntity(appointment);

        AppointmentEntity savedEntity
                = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Appointment> findByAppointmentCode(
            String appointmentCode
    ) {
        return jpaRepository.findByAppointmentCode(
                appointmentCode
        ).map(mapper::toDomain);
    }

    @Override
    public boolean existsByAppointmentCode(
            String appointmentCode
    ) {
        return jpaRepository.existsByAppointmentCode(
                appointmentCode
        );
    }

    @Override
    public Optional<Appointment> findTopByOrderByAppointmentCodeDesc() {
        return jpaRepository
                .findTopByOrderByAppointmentCodeDesc()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<String> findAppointmentCodeWithHighestSequence() {
        return jpaRepository.findAppointmentCodeWithHighestSequence();
    }

    @Override
    public Page<Appointment> search(
            SearchAppointmentCommand command
    ) {

        return jpaRepository.findAll(
                AppointmentSearchSpecification.build(command),
                command.pageable()
        ).map(mapper::toDomain);

    }

    @Override
    public boolean existsActiveAppointmentConflict(
            UUID doctorId,
            Instant startTime,
            Instant endTime
    ) {

        return jpaRepository.exists(
                AppointmentBusinessSpecification
                        .hasDoctor(doctorId)
                        .and(
                                AppointmentBusinessSpecification
                                        .active()
                        )
                        .and(
                                AppointmentBusinessSpecification
                                        .overlap(
                                                startTime,
                                                endTime
                                        )
                        )
        );

    }

    @Override
    public Page<Appointment> findOverdue(
            Instant threshold,
            Pageable pageable
    ) {

        return jpaRepository.findAll(
                AppointmentBusinessSpecification.overdue(threshold),
                pageable
        ).map(mapper::toDomain);

    }

    @Override
    public List<UUID> findDueReminderIds(
            Instant now,
            Instant reminderDeadline,
            Collection<AppointmentStatus> statuses
    ) {
        return jpaRepository.findDueReminderIds(now, reminderDeadline, statuses);
    }

    @Override
    public Optional<Appointment> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findActiveAppointmentsForDoctorBetween(
            UUID doctorId,
            Instant from,
            Instant to
    ) {
        return jpaRepository.findActiveForDoctorBetween(
                        doctorId,
                        from,
                        to,
                        List.of(
                                AppointmentStatus.SCHEDULED,
                                AppointmentStatus.CONFIRMED,
                                AppointmentStatus.IN_PROGRESS
                        )
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

}
