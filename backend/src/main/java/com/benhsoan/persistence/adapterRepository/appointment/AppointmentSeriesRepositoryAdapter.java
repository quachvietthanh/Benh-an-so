package com.benhsoan.persistence.adapterRepository.appointment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentSeriesRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentSeriesPersistenceMapper;
import com.benhsoan.port.outbound.repository.appointment.AppointmentSeriesRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AppointmentSeriesRepositoryAdapter implements AppointmentSeriesRepository {

    private final JpaAppointmentSeriesRepository jpaRepository;
    private final AppointmentSeriesPersistenceMapper mapper;

    @Override
    public Optional<AppointmentSeries> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppointmentSeries> findBySeriesCode(String seriesCode) {
        return jpaRepository.findBySeriesCode(seriesCode).map(mapper::toDomain);
    }

    @Override
    public List<AppointmentSeries> findByPatientIdOrderByCreatedAtDesc(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public AppointmentSeries save(AppointmentSeries series) {
        var entity = mapper.toEntity(series);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<String> findHighestSeriesCode() {
        return jpaRepository.findHighestSeriesCode();
    }
}
