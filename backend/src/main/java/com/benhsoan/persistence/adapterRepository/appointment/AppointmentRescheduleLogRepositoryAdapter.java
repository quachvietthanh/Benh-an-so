package com.benhsoan.persistence.adapterRepository.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentRescheduleLogRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentRescheduleLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRescheduleLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AppointmentRescheduleLogRepositoryAdapter
        implements AppointmentRescheduleLogRepository {

    private final JpaAppointmentRescheduleLogRepository jpaRepository;
    private final AppointmentRescheduleLogPersistenceMapper mapper;

    @Override
    public AppointmentRescheduleLog save(AppointmentRescheduleLog log) {
        var entity = mapper.toEntity(log);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AppointmentRescheduleLog> findByAppointmentId(UUID appointmentId) {
        return jpaRepository.findByAppointmentIdOrderByRescheduledAtDesc(appointmentId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
