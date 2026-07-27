package com.benhsoan.persistence.adapterRepository.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.jpaRepository.appointment.AppointmentBusinessSpecification;
import com.benhsoan.persistence.jpaRepository.appointment.AppointmentSearchSpecification;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentPersistenceMapper;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;

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
    public void deleteById(UUID id) {

        if (id == null) {
            return;
        }

        jpaRepository.deleteById(id);
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

}
